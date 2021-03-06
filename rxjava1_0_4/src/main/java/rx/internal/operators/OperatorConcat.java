/**
 * Copyright 2014 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import android.util.Log;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Observable;
import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Returns an Observable that emits the items emitted by two or more Observables, one after the other.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/concat.png" alt="">
 *
 * @param <T> the source and result value type
 */
public final class OperatorConcat<T> implements Operator<T, Observable<? extends T>> {
    @Override
    public Subscriber<? super Observable<? extends T>> call(final Subscriber<? super T> child) {
        final SerializedSubscriber<T> s = new SerializedSubscriber<T>(child);
        final SerialSubscription current = new SerialSubscription();
        child.add(current);
        ConcatSubscriber<T> cs = new ConcatSubscriber<T>(s, current);
        //这里child设置了producer
        ConcatProducer<T> cp = new ConcatProducer<T>(cs);
        Log.e("TAG", "OperatorConcat call 调用 child设置ConcatProducer:"+child.getName());
        child.setProducer(cp);
        return cs;
    }

    static final class ConcatProducer<T> implements Producer {
        final ConcatSubscriber<T> cs;

        ConcatProducer(ConcatSubscriber<T> cs) {
            this.cs = cs;
        }

        @Override
        public void request(long n) {
            Log.e("TAG", "ConcatProducer 准备开始request---------:");
            cs.requestFromChild(n);
            Log.w("TAG",
                    "<<看这里 ConcatProducer request后--------  cs.requestFromChild(n):" + n + "  " +
                            "(n==Math" +
                            ".max)=" +
                            (n == Long.MAX_VALUE));
        }

    }

    static final class ConcatSubscriber<T> extends Subscriber<Observable<? extends T>> {
        final NotificationLite<Observable<? extends T>> nl = NotificationLite.instance();
        private final Subscriber<T> child;
        private final SerialSubscription current;
        final ConcurrentLinkedQueue<Object> queue;

        volatile ConcatInnerSubscriber<T> currentSubscriber;

        volatile int wip;
        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<ConcatSubscriber> WIP_UPDATER =
                AtomicIntegerFieldUpdater.newUpdater(ConcatSubscriber.class, "wip");

        // accessed by REQUESTED_UPDATER
        private volatile long requested;
        @SuppressWarnings("rawtypes")
        private static final AtomicLongFieldUpdater<ConcatSubscriber> REQUESTED_UPDATER =
                AtomicLongFieldUpdater.newUpdater(ConcatSubscriber.class, "requested");

        public ConcatSubscriber(Subscriber<T> s, SerialSubscription current) {
            super(s);
            this.child = s;
            this.current = current;
            this.queue = new ConcurrentLinkedQueue<Object>();
            add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    queue.clear();
                }
            }));
        }

        @Override
        public void onStart() {
            // no need for more than 1 at a time since we concat 1 at a time, so we'll request 2 to start ...
            // 1 to be subscribed to, 1 in the queue, then we'll keep requesting 1 at a time after that
            //把这里改成1，后面 request(1)注释掉不行,这里只是给requested赋值
            request(2);

            Log.e("TAG", "OperatorConcat ConcatSubscriber  onStart(lif call里面调用)   request(2) " +
                    "------------------:");
        }

        //订阅的时候就会设置为Long.MAX_VALUE
        private void requestFromChild(long n) {
            // we track 'requested' so we know whether we should subscribe the next or not
            long andAdd = REQUESTED_UPDATER.getAndAdd(this, n);
            Log.e("TAG",
                    "OperatorConcat ConcatSubscriber requestFromChild andAdd:" + andAdd + "  " +
                            "wip=" + wip + "  (currentSubscriber == null)=" +
                            (currentSubscriber == null));
            if (andAdd == 0) {
                if (currentSubscriber == null && wip > 0) {
                    // this means we may be moving from one subscriber to another after having stopped processing
                    // so need to kick off the subscribe via this request notification
                    subscribeNext();
                    // return here as we don't want to do the requestMore logic below (which would double request)
                    return;
                }
            }

            if (currentSubscriber != null) {
                // otherwise we are just passing it through to the currentSubscriber
                currentSubscriber.requestMore(n);
            }
        }

        private void decrementRequested() {
            REQUESTED_UPDATER.decrementAndGet(this);
        }

        @Override
        public void onNext(Observable<? extends T> t) {

            //队列很重要，如果第一个observable没处理完，来了第二个observable,入队列，前面observable处理完，会调用subscribeNext从队列取出
            queue.add(nl.next(t));
            int andIncrement = WIP_UPDATER.getAndIncrement(this);
            String subscribeNext = "";
            if (andIncrement == 0) {
                subscribeNext = "准备调用subscribeNext方法";
            }
            Log.w("TAG",
                    "！！！！！！！！！！！OperatorConcat ConcatSubscriber onNext 重点 收到了 Observable= " + t +
                            "  " +
                            "andIncrement" +
                            "=" +
                            andIncrement + "  " + subscribeNext);
            if (andIncrement == 0) {
                subscribeNext();
            }
        }

        @Override
        public void onError(Throwable e) {
            child.onError(e);
            unsubscribe();
        }

        @Override
        public void onCompleted() {
            queue.add(nl.completed());
            if (WIP_UPDATER.getAndIncrement(this) == 0) {
                subscribeNext();
            }
        }

        void completeInner() {
            //其实这里全部注释掉，第二个observable也会发送到ConcatSubscriber的onNext方法，只是无法继续向下分发，两个observable是用Observable.from封装成集合按顺序发送的
            Log.d("TAG", "ConcatSubscriber completeInner 准备发送下一个:");
            request(1);
            currentSubscriber = null;
            int i = WIP_UPDATER.decrementAndGet(this);
            Log.e("TAG", "ConcatSubscriber completeInner i:" + i);
            if (i > 0) {
                subscribeNext();
            }
        }

        void subscribeNext() {
            if (requested > 0) {
                Object o = queue.poll();
                //发送完成，直接回调给真正的观察者
                if (nl.isCompleted(o)) {
                    child.onCompleted();
                } else if (o != null) {
                    Observable<? extends T> obs = nl.getValue(o);

                    Log.e("TAG", "********OperatorConcat ConcatSubscriber 真正下层订阅(unsafeSubscribe)" +
                            "(既会触发request方法，又会触发订阅)" +
                            "subscribeNext" +
                            " " +
                            " " +
                            "obs（Observable）=" +
                            " " + obs);
                    //child是下层真正的subscriber
                    currentSubscriber = new ConcatInnerSubscriber<T>(this, child, requested);
                    current.set(currentSubscriber);
                    obs.unsafeSubscribe(currentSubscriber);
                }
            } else {
                // requested == 0, so we'll peek to see if we are completed, otherwise wait until another request
                Object o = queue.peek();
                Log.e("TAG", "ConcatSubscriber subscribeNext 调用 queue.peek():");
                if (nl.isCompleted(o)) {
                    child.onCompleted();
                }
            }
        }
    }

    static class ConcatInnerSubscriber<T> extends Subscriber<T> {

        private final Subscriber<T> child;
        private final ConcatSubscriber<T> parent;
        @SuppressWarnings("unused")
        private volatile int once = 0;
        @SuppressWarnings("rawtypes")
        private final static AtomicIntegerFieldUpdater<ConcatInnerSubscriber> ONCE_UPDATER =
                AtomicIntegerFieldUpdater.newUpdater(ConcatInnerSubscriber.class, "once");

        public ConcatInnerSubscriber(ConcatSubscriber<T> parent, Subscriber<T> child,
                                     long initialRequest) {
            this.parent = parent;
            this.child = child;
            request(initialRequest);
        }

        void requestMore(long n) {
            Log.w("TAG", "OperatorConcat ConcatInnerSubscriber requestMore:" + n);
            request(n);
        }

        @Override
        public void onNext(T t) {
            parent.decrementRequested();
            Log.e("TAG",
                    "OperatorConcat ConcatInnerSubscriber ConcatInnerSubscriber 发送给 child onNext:" +
                            t);
            child.onNext(t);
        }

        @Override
        public void onError(Throwable e) {
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                // terminal error through parent so everything gets cleaned up, including this inner
                parent.onError(e);
            }
        }

        @Override
        public void onCompleted() {
            //上一个Obeservable发送数据，下一个才开始发，跟merge相比，保证有序
            if (ONCE_UPDATER.compareAndSet(this, 0, 1)) {
                // terminal completion to parent so it continues to the next
                parent.completeInner();
            }
        }

    }

    ;
}
