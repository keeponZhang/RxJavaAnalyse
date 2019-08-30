/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.SerialSubscription;

/**
 * Correlates the elements of two sequences based on overlapping durations.
 * 
 * @param <TLeft> the left value type
 * @param <TRight> the right value type
 * @param <TLeftDuration> the left duration value type
 * @param <TRightDuration> the right duration type
 * @param <R> the result type
 */
public final class OnSubscribeJoin<TLeft, TRight, TLeftDuration, TRightDuration, R> implements OnSubscribe<R> {
    final Observable<TLeft> left;
    final Observable<TRight> right;
    final Func1<TLeft, Observable<TLeftDuration>> leftDurationSelector;
    final Func1<TRight, Observable<TRightDuration>> rightDurationSelector;
    final Func2<TLeft, TRight, R> resultSelector;

    public OnSubscribeJoin(
            Observable<TLeft> left,
            Observable<TRight> right,
            Func1<TLeft, Observable<TLeftDuration>> leftDurationSelector,
            Func1<TRight, Observable<TRightDuration>> rightDurationSelector,
            Func2<TLeft, TRight, R> resultSelector) {
        this.left = left;
        this.right = right;
        this.leftDurationSelector = leftDurationSelector;
        this.rightDurationSelector = rightDurationSelector;
        this.resultSelector = resultSelector;
    }

    @Override
    public void call(Subscriber<? super R> t1) {
        ResultSink result = new ResultSink(new SerializedSubscriber<R>(t1));
        result.run();
    }

    /** Manage the left and right sources. */
    final class ResultSink {
        final CompositeSubscription group;
        final Subscriber<? super R> subscriber;
        final Object guard = new Object();
        /** Guarded by guard. */
        boolean leftDone;
        /** Guarded by guard. */
        int leftId;
        /** Guarded by guard. */
        final Map<Integer, TLeft> leftMap;
        /** Guarded by guard. */
        boolean rightDone;
        /** Guarded by guard. */
        int rightId;
        /** Guarded by guard. */
        final Map<Integer, TRight> rightMap;

        public ResultSink(Subscriber<? super R> subscriber) {
            this.subscriber = subscriber;
            this.group = new CompositeSubscription();
            this.leftMap = new HashMap<Integer, TLeft>();
            this.rightMap = new HashMap<Integer, TRight>();
        }

        public void run() {
            subscriber.add(group);
            
            Subscriber<TLeft> s1 = new LeftSubscriber();
            Subscriber<TRight> s2 = new RightSubscriber();
            
            group.add(s1);
            group.add(s2);

            left.unsafeSubscribe(s1);
            right.unsafeSubscribe(s2);
        }

        /** Observes the left values. */
        final class LeftSubscriber extends Subscriber<TLeft> {

            protected void expire(int id, Subscription resource) {
                boolean complete = false;
                synchronized (guard) {
                    if (leftMap.remove(id) != null && leftMap.isEmpty() && leftDone) {
                        complete = true;
                    }
                }
                //leftMap.isEmpty() 空了才发送onCompleted事件
                if (complete) {
                    subscriber.onCompleted();
                    subscriber.unsubscribe();
                } else {
                    group.remove(resource);
                }
            }

            @Override
            public void onNext(TLeft args) {
                int id;
                int highRightId;

                synchronized (guard) {
                    id = leftId++;
                    //leftMap会保持
                    leftMap.put(id, args);
                    highRightId = rightId;
                }

                Observable<TLeftDuration> duration;
                try {
                    Log.w("TAG", " JoinExampleFragment LeftSubscriber onNext leftDurationSelector call:"+args);
                    duration = leftDurationSelector.call(args);

                    //duration 一般是个延迟的Observable ,因为LeftDurationSubscriber的onNext方法会调用下游观察者的onComplete方法
                    //有几个数据就有几个LeftDurationSubscriber
                    Subscriber<TLeftDuration> d1 = new LeftDurationSubscriber(id);
                    group.add(d1);

                    duration.unsafeSubscribe(d1);

                    List<TRight> rightValues = new ArrayList<TRight>();
                    synchronized (guard) {
                        for (Map.Entry<Integer, TRight> entry : rightMap.entrySet()) {
                            if (entry.getKey() < highRightId) {
                                rightValues.add(entry.getValue());
                            }
                        }
                    }
                    for (TRight r : rightValues) {
                        Log.e("TAG", "JoinExampleFragment <LeftSubscriber> onNext resultSelector.call(args, r):");
                        R result = resultSelector.call(args, r);
                        //最下游的观察者收到转换后的事件
                        subscriber.onNext(result);
                    }
                } catch (Throwable t) {
                    onError(t);
                }
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
                subscriber.unsubscribe();
            }

            @Override
            public void onCompleted() {
                boolean complete = false;
                synchronized (guard) {
                    leftDone = true;
                    if (rightDone || leftMap.isEmpty()) {
                        complete = true;
                    }
                }
                if (complete) {
                    subscriber.onCompleted();
                    subscriber.unsubscribe();
                } else {
                    group.remove(this);
                }
            }

            /** Observes the left duration. */
            final class LeftDurationSubscriber extends Subscriber<TLeftDuration> {
                final int id;
                boolean once = true;

                public LeftDurationSubscriber(int id) {
                    this.id = id;
                }

                @Override
                public void onNext(TLeftDuration args) {
                    Log.e("TAG", "------JoinExampleFragment LeftDurationSubscriber onNext TLeftDuration args:"+args);
                    onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    LeftSubscriber.this.onError(e);
                }

                @Override
                public void onCompleted() {
                    Log.e("TAG", "JoinExampleFragment LeftDurationSubscriber onCompleted expire once:"+once);
                    if (once) {
                        once = false;
                        expire(id, this);
                    }
                }

            }
        }

        /** Observes the right values. */
        final class RightSubscriber extends Subscriber<TRight> {

            void expire(int id, Subscription resource) {
                boolean complete = false;
                synchronized (guard) {
                    if (rightMap.remove(id) != null && rightMap.isEmpty() && rightDone) {
                        complete = true;
                    }
                }
                if (complete) {
                    subscriber.onCompleted();
                    subscriber.unsubscribe();
                } else {
                    group.remove(resource);
                }
            }

            @Override
            public void onNext(TRight args) {
                int id; 
                int highLeftId;
                synchronized (guard) {
                    id = rightId++;
                    rightMap.put(id, args);
                    highLeftId = leftId;
                }
                SerialSubscription md = new SerialSubscription();
                group.add(md);

                Observable<TRightDuration> duration;
                try {
                    Log.e("TAG", "JoinExampleFragment RightSubscriber onNext  rightDurationSelector.call TRight:"+args);
                    duration = rightDurationSelector.call(args);

                    Subscriber<TRightDuration> d2 = new RightDurationSubscriber(id);
                    group.add(d2);
                    
                    duration.unsafeSubscribe(d2);
                    

                    List<TLeft> leftValues = new ArrayList<TLeft>();
                    synchronized (guard) {
                        for (Map.Entry<Integer, TLeft> entry : leftMap.entrySet()) {
                            if (entry.getKey() < highLeftId) {
                                leftValues.add(entry.getValue());
                            }
                        }
                    }
                    
                    for (TLeft lv : leftValues) {
                        Log.e("TAG", "JoinExampleFragment <RightSubscriber> onNext resultSelector.call(args, r):");
                        R result = resultSelector.call(lv, args);
                        subscriber.onNext(result);
                    }
                    
                } catch (Throwable t) {
                    onError(t);
                }
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
                subscriber.unsubscribe();
            }

            @Override
            public void onCompleted() {
                boolean complete = false;
                synchronized (guard) {
                    rightDone = true;
                    if (leftDone || rightMap.isEmpty()) {
                        complete = true;
                    }
                }
                if (complete) {
                    subscriber.onCompleted();
                    subscriber.unsubscribe();
                } else {
                    group.remove(this);
                }
            }

            /** Observe the right duration. */
            final class RightDurationSubscriber extends Subscriber<TRightDuration> {
                final int id;
                boolean once = true;

                public RightDurationSubscriber(int id) {
                    this.id = id;
                }

                @Override
                public void onNext(TRightDuration args) {
                    Log.e("TAG", "JoinExampleFragment RightDurationSubscriber onNext -----:"+args);
                    onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    RightSubscriber.this.onError(e);
                }

                @Override
                public void onCompleted() {
                    Log.e("TAG", "JoinExampleFragment RightDurationSubscriber onCompleted once:"+once);
                    if (once) {
                        once = false;
                        expire(id, this);
                    }
                }

            }
        }
    }
}
