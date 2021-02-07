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

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import rx.Notification;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Producer;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.SerialSubscription;

import static rx.Observable.create;

public final class OnSubscribeRedo<T> implements OnSubscribe<T> {

    static final Func1<Observable<? extends Notification<?>>, Observable<?>> REDO_INIFINITE = new Func1<Observable<? extends Notification<?>>, Observable<?>>() {
        @Override
        public Observable<?> call(Observable<? extends Notification<?>> ts) {
            System.out.println(getOnSubscribeRedoTag()+"  1.1--------- REDO_INIFINITE  call------------》》》》》");
            return ts.map(new Func1<Notification<?>, Notification<?>>() {
                @Override
                public Notification<?> call(Notification<?> terminal) {
                    return Notification.createOnNext(null);
                }
            });
        }
    };

    public static final class RedoFinite implements Func1<Observable<? extends Notification<?>>, Observable<?>> {
        //表示要重复多少次
        private final long count;

        public RedoFinite(long count) {
            this.count = count;
        }

        //ts:前面 terminals.lift创建的一个Observable
        @Override
        public Observable<?> call(Observable<? extends Notification<?>> ts) {
            Log.e("TAG", getOnSubscribeRedoTag()+" RedoFinite  0.6 (准备lift 2次，map，OperatorDematerialize)---------  map" +
                    "  " +
                    "Observable<? extends Notification<?>> call------------》》》》》:");
            //这里通过先map后lift再次创建Observable返回，相当于lift两次
            Observable<?> dematerialize = ts.map(new Func1<Notification<?>, Notification<?>>() {

                int num = 0;

                @Override
                public Notification<?> call(Notification<?> terminalNotification) {
                    Log.e("TAG", "OnSubscribeRedo  2--------- RedoFinite Func1 call------------》》》》》terminalNotification=" + terminalNotification+ " terminalNotification.getKind()="+terminalNotification.getKind());
                    if (count == 0) {
                        return terminalNotification;
                    }

                    num++;
                    //terminalNotification.getKind OnError,下面是否重试会通过判断terminalNotification.getKind 来判断
                    if (num <= count) {
                        return Notification.createOnNext(num);
                    } else {
                        return terminalNotification;
                    }
                }

            }).lift(new OperatorDematerialize());;
            Log.e("TAG", getOnSubscribeRedoTag()+"  0.7--------- RedoFinite  " +
                    "return------------》》》》》 " +
                    "Observable dematerialize="+dematerialize);
            return dematerialize;
        }
    }

    public static final class RetryWithPredicate implements Func1<Observable<? extends Notification<?>>, Observable<? extends Notification<?>>> {
        private Func2<Integer, Throwable, Boolean> predicate;

        public RetryWithPredicate(Func2<Integer, Throwable, Boolean> predicate) {
            this.predicate = predicate;
        }

        @Override
        public Observable<? extends Notification<?>> call(Observable<? extends Notification<?>> ts) {
            return ts.scan(Notification.createOnNext(0), new Func2<Notification<Integer>, Notification<?>, Notification<Integer>>() {
                @SuppressWarnings("unchecked")
                @Override
                public Notification<Integer> call(Notification<Integer> n, Notification<?> term) {
                    final int value = n.getValue();
                    if (predicate.call(value, term.getThrowable()).booleanValue())
                        return Notification.createOnNext(value + 1);
                    else
                        return (Notification<Integer>) term;
                }
            });
        }
    }

    public static <T> Observable<T> retry(Observable<T> source) {
        return retry(source, REDO_INIFINITE);
    }

    public static <T> Observable<T> retry(Observable<T> source, final long count) {
        if (count < 0)
            throw new IllegalArgumentException("count >= 0 expected");
        if (count == 0)
            return source;
        return retry(source, new RedoFinite(count));
    }

    public static <T> Observable<T> retry(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, true, false, Schedulers.trampoline()));
    }

    public static <T> Observable<T> retry(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler, Scheduler scheduler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, true, false, scheduler));
    }

    public static <T> Observable<T> repeat(Observable<T> source) {
        return repeat(source, Schedulers.trampoline());
    }

    public static <T> Observable<T> repeat(Observable<T> source, Scheduler scheduler) {
        return repeat(source, REDO_INIFINITE, scheduler);
    }

    public static <T> Observable<T> repeat(Observable<T> source, final long count) {
        return repeat(source, count, Schedulers.trampoline());
    }

    public static <T> Observable<T> repeat(Observable<T> source, final long count, Scheduler scheduler) {
        if(count == 0) {
            return Observable.empty();
        }
        if (count < 0)
            throw new IllegalArgumentException("count >= 0 expected");
        return repeat(source, new RedoFinite(count - 1), scheduler);
    }

    public static <T> Observable<T> repeat(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, false, true, Schedulers.trampoline()));
    }

    public static <T> Observable<T> repeat(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler, Scheduler scheduler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, false, true, scheduler));
    }

    public static <T> Observable<T> redo(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> notificationHandler, Scheduler scheduler) {
        return create(new OnSubscribeRedo<T>(source, notificationHandler, false, false, scheduler));
    }

    private Observable<T> source;
    private final Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> controlHandlerFunction;
    private boolean stopOnComplete;
    private boolean stopOnError;
    private final Scheduler scheduler;

    private OnSubscribeRedo(Observable<T> source, Func1<? super Observable<? extends Notification<?>>, ? extends Observable<?>> f, boolean stopOnComplete, boolean stopOnError,
            Scheduler scheduler) {
        //source是上层的Observable
        this.source = source;
        //OnSubscribeRedo.RedoFinite 次数有限的话
        this.controlHandlerFunction = f;
        this.stopOnComplete = stopOnComplete;
        this.stopOnError = stopOnError;
        this.scheduler = scheduler;
    }
    //child是下层的Subscriber，source是上层的Observable
    @Override
    public void call(final Subscriber<? super T> child) {
        final AtomicBoolean isLocked = new AtomicBoolean(true);
        final AtomicBoolean resumeBoundary = new AtomicBoolean(true);
        // incremented when requests are made, decremented when requests are fulfilled
        final AtomicLong consumerCapacity = new AtomicLong(0l);
        final AtomicReference<Producer> currentProducer = new AtomicReference<Producer>();
        //TrampolineScheduler InnerCurrentThreadScheduler
        final Scheduler.Worker worker = scheduler.createWorker();
        child.add(worker);

        final SerialSubscription sourceSubscriptions = new SerialSubscription();
        child.add(sourceSubscriptions);
        //是Notification类型的，这个很重要
        final PublishSubject<Notification<?>> terminals = PublishSubject.create();

        final Action0 subscribeToSource = new Action0() {
            @Override
            public void call() {
                if (child.isUnsubscribed()) {
                    return;
                }

                Subscriber<T> terminalDelegatingSubscriber = new Subscriber<T>() {
                    @Override
                    public void onCompleted() {
                        unsubscribe();
                        Log.e("TAG", getOnSubscribeRedoTag()+" onCompleted (PublishSubject)terminals.onNext:"+Notification.createOnCompleted());
                        terminals.onNext(Notification.createOnCompleted());

                    }

                    @Override
                    public void onError(Throwable e) {
                        unsubscribe();
                        //onError跟onCompleted terminals ，经过此时terminals充当observer，terminals.lift创建的subsrciber
                        //触发三次lift生成的Observable的最顶层Observable发送事件，调用的是PublishSubject的onNext,接着调用SubjectSubscriptionManager的SubjectObserver的onNext
                        //terminals相当于充当最顶层Observer和Observable
                        //2.1增加注释：触发订阅一般会触发到最顶层onSubscribe.call(subscriber),
                        // call里面有触发发送的话才会发送，这里的话是触发到SubjectSubscriptionManager的call，会触发增加观察者的逻辑,
                        Log.e("TAG", getOnSubscribeRedoTag()+"  收到错事件了 onError:");
                        terminals.onNext(Notification.createOnError(e));
                    }
                    //onNext方法直接接受source发送的事件，进而转发给child,不经过terminals，所以next事件跟后面lift生成的Observable无关
                    @Override
                    public void onNext(T v) {
                        if (consumerCapacity.get() != Long.MAX_VALUE) {
                            consumerCapacity.decrementAndGet();
                        }
                        Log.e("TAG",getOnSubscribeRedoTag()+" terminalDelegatingSubscriber 1.5 接送到source 发送来的事件 onNext="+v);
                        //调用该方法把事件发给下层的订阅者
                        child.onNext(v);
                    }

                    @Override
                    public void setProducer(Producer producer) {
                        currentProducer.set(producer);
                        long c = consumerCapacity.get();
                        if (c > 0) {
                            producer.request(c);
                        }
                    }
                };
                // new subscription each time so if it unsubscribes itself it does not prevent retries
                // by unsubscribing the child subscription
                sourceSubscriptions.set(terminalDelegatingSubscriber);
                Log.e("TAG", getOnSubscribeRedoTag()+"  subscribeToSource ---soure订阅开始喽 source.unsafeSubscribe(terminalDelegatingSubscriber)"+terminalDelegatingSubscriber);
                source.unsafeSubscribe(terminalDelegatingSubscriber);
            }
        };

        // the observable received by the control handler function will receive notifications of onCompleted in the case of 'repeat' 
        // type operators or notifications of onError for 'retry' this is done by lifting in a custom operator to selectively divert 
        // the retry/repeat relevant values to the control handler
        //terminals:PublishSubject 这里充当Observable
        Observable<Notification<?>> terminalslift = terminals.lift(new Operator<Notification<?>, Notification<?>>() {
            //下面call方法是Operator的call方法，返回另一个Subscriber
            @Override
            public Subscriber<? super Notification<?>> call(final Subscriber<? super Notification<?>> filteredTerminals) {
                //启动订阅后，会OnSubscribe的call方法，一层一层上去，这里就是其中几个lift的一层
                Log.w("TAG", getOnSubscribeRedoTag()+"  1.2--------- 调用call方法 restarts Operator " +
                        "call------------filteredTerminals 》》》》》" + filteredTerminals + " subsriber= " + this);
                return new Subscriber<Notification<?>>(filteredTerminals) {
                    @Override
                    public void onCompleted() {
                        filteredTerminals.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.w("TAG", getOnSubscribeRedoTag()+"  Notification filteredTerminals.onError(e)" +
                                ":"+e);
                        filteredTerminals.onError(e);
                    }

                    @Override
                    public void onNext(Notification<?> t) {
                       //这里也是重点（retry :stopOnError true ; repeat :stopOnComplete true）
                        Log.e("TAG", getOnSubscribeRedoTag()+" 1.95 重点 terminalslift call   onNext t:"+t);
                        if (t.isOnCompleted() && stopOnComplete) {
                            Log.e("TAG", getOnSubscribeRedoTag()+"  1.98 onNext   Subscriber terminals.lift onNext <<  if stopOnComplete >>" + this+"  Notification.getKind="+t.getKind());
                            child.onCompleted();
                        } else if (t.isOnError() && stopOnError) {
                            Log.e("TAG", getOnSubscribeRedoTag()+" onNext   1.98 Subscriber terminals.lift onNext << else if stopOnError >>" + this+"  Notification.getKind="+t.getKind());
                            child.onError(t.getThrowable());
                        } else {
                            Log.e("TAG", getOnSubscribeRedoTag()+" onNext  1.98   Subscriber terminals.lift onNext << else  >>" + this+"  Notification.getKind="+t.getKind());
                            isLocked.set(false);
                            filteredTerminals.onNext(t);
                        }
                    }

                    @Override
                    public void setProducer(Producer producer) {
                        producer.request(Long.MAX_VALUE);
                    }
                };
            }
        });
        Log.e("TAG", getOnSubscribeRedoTag()+" call   0.1 terminals.lift 准备调用fun1.call方法:");
        //  controlHandlerFunction: RedoFinite  terminalslift作为controlHandlerFunction.call的方法参数
        //相当于terminalslift.map ,相当于terminals.lift.map.  lift所以terminalslift是比较上层，terminals为最上层Observable,restarts为底层Observable,restarts调用subscribe开始一层层订阅
        //其实这里面实质就是调用call生成Observable，就是lift
        final Observable<?> restarts = controlHandlerFunction.call(terminalslift);
        Log.e("TAG", getOnSubscribeRedoTag()+" call   0.8 terminals.lift fun1.call(controlHandlerFunction.call)方法调用完毕:");
        // subscribe to the restarts observable to know when to schedule the next redo.
        //worker经过一系列调用会调到call方法
        worker.schedule(new Action0() {
            @Override
            public void call() {
                //1.call方法通过worker调用，restarts作为Obesrvable订阅,workerSubscriber作为参数传到lift
                // Observable的OnSubscribe的call方法中，restarts的call方法中,再调到上面2中Operator的call方法中
                Log.i("TAG", getOnSubscribeRedoTag()+"  0.9--------- worker.schedule call------------》》》》》");
                Subscriber<Object> workerSubscriber = new Subscriber<Object>(child) {
                    @Override
                    public void onCompleted() {
                        System.out.println(getOnSubscribeRedoTag()+" workerSubscriber onCompleted" );
                        child.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("TAG", getOnSubscribeRedoTag()+" workerSubscriber onError" );
                        child.onError(e);
                    }

                    @Override
                    public void onNext(Object t) {
                        boolean isLockedBoolean = isLocked.get();
                        boolean unsubscribed = child.isUnsubscribed();
                        Log.e("TAG", "OnSubscribeRedo  ---------workerSubscriber 重新发射 worker.schedule   restarts onNext==" + t +" subscriber="+this+ " ---------"+"  isLockedBoolean="+ isLockedBoolean+" unsubscribed="+unsubscribed);
                        if (!isLocked.get() && !child.isUnsubscribed()) {
                            //这里重新发射
                            if (consumerCapacity.get() > 0) {
                                //重复发射的启动：重点
                                worker.schedule(subscribeToSource);
                            } else {
                                resumeBoundary.compareAndSet(false, true);
                            }
                        }
                    }

                    @Override
                    public void setProducer(Producer producer) {
                        System.out.println(getOnSubscribeRedoTag()+"   worker.schedule setProducer ");
                        producer.request(Long.MAX_VALUE);
                    }
                };
                Log.e("TAG", getOnSubscribeRedoTag()+" 1 (负责重复发送的Observable开始订阅，实际发送需要触发terminals" +
                        ".onNext方法(因为最顶层Observable是PublishObservable))" +
                        "---------" +
                        "  restarts" +
                        ".unsafeSubscribe" +
                        "(workerSubscriber) restarts==" + restarts +" workerSubscriber="+workerSubscriber+ " ---------");
                //restarts是lift 3次最后一次生成的Observable, restarts.unsafeSubscribe会实现层层订阅（层层调用Observable的OnSubscribe的call方法，再调用Operator的call方法生成subscriber，让生成的subsriber订阅上层，最上层的会调用onNext开始分发）
                //terminals.lift.map.lift.unsafeSubscribe(workerSubscriber);
                //相当于PublishSubject.lift.map.lift.unsafeSubscribe(workerSubscriber);
                //lift会先生成一个Observable和OnSubscribe,下层触发订阅的时候会调用生成subscriber，然后传给对应的Operator
                //相当于PublishSubject.lift（上面有个匿名内部类Opertator）.lift(new OperatorMap<T, R>(func)) .lift(OperatorDematerialize)
                // .unsafeSubscribe(workerSubscriber);
                //这里启动层层订阅，但是最顶层的observabel还没发射，需要调用terminals.onNext
                restarts.unsafeSubscribe(workerSubscriber);
            }
        });
        Log.i("TAG", getOnSubscribeRedoTag()+" OnSubscribeRedo OnSubscribe方法call setProducer");
        child.setProducer(new Producer() {

            @Override
            public void request(final long n) {
                long c = consumerCapacity.getAndAdd(n);
                Producer producer = currentProducer.get();
                if (producer != null) {
                    producer.request(n);
                } else
                if (c == 0 && resumeBoundary.compareAndSet(true, false)) {
                    Log.e("TAG", getOnSubscribeRedoTag() +"  request ----------开始调用Action0的call" +
                            "方法，call方法中 " +
                            "source.unsafeSubscribe,启动发射");
                    //第一次启动：重点
                    worker.schedule(subscribeToSource);
                }
            }
        });
    }
    public static String getOnSubscribeRedoTag(){
        return "OnSubscribeRedoTAG ";
    }
    public static String getOnSubscribeRedoDebugTag(){
        return "OnSubscribeRedoTAG ";
    }
}
