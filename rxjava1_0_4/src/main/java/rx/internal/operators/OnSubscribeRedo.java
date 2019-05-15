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
            System.out.println("OnSubscribeRedo  1.1--------- REDO_INIFINITE  call------------》》》》》");
            return ts.map(new Func1<Notification<?>, Notification<?>>() {
                @Override
                public Notification<?> call(Notification<?> terminal) {
                    return Notification.createOnNext(null);
                }
            });
        }
    };

    public static final class RedoFinite implements Func1<Observable<? extends Notification<?>>, Observable<?>> {
        private final long count;

        public RedoFinite(long count) {
            this.count = count;
        }

        @Override
        public Observable<?> call(Observable<? extends Notification<?>> ts) {
            System.out.println("OnSubscribeRedo  1.1--------- RedoFinite  call------------》》》》》ts="+ts);
            Observable<?> dematerialize = ts.map(new Func1<Notification<?>, Notification<?>>() {

                int num = 0;

                @Override
                public Notification<?> call(Notification<?> terminalNotification) {
                    System.out.println("OnSubscribeRedo  1.1--------- RedoFinite Func1 call------------》》》》》terminalNotification=" + terminalNotification);
                    if (count == 0) {
                        return terminalNotification;
                    }

                    num++;
                    if (num <= count) {
                        return Notification.createOnNext(num);
                    } else {
                        return terminalNotification;
                    }
                }

            }).dematerialize();
            System.out.println("OnSubscribeRedo  1.1--------- RedoFinite  return------------》》》》》dematerialize="+dematerialize);
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
        this.source = source;
        //OnSubscribeRedo.RedoFinite 次数有限的话
        this.controlHandlerFunction = f;
        this.stopOnComplete = stopOnComplete;
        this.stopOnError = stopOnError;
        this.scheduler = scheduler;
    }

    @Override
    public void call(final Subscriber<? super T> child) {
        final AtomicBoolean isLocked = new AtomicBoolean(true);
        final AtomicBoolean resumeBoundary = new AtomicBoolean(true);
        // incremented when requests are made, decremented when requests are fulfilled
        final AtomicLong consumerCapacity = new AtomicLong(0l);
        final AtomicReference<Producer> currentProducer = new AtomicReference<Producer>();
        //TrampolineScheduler
        final Scheduler.Worker worker = scheduler.createWorker();
        child.add(worker);

        final SerialSubscription sourceSubscriptions = new SerialSubscription();
        child.add(sourceSubscriptions);

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
                        terminals.onNext(Notification.createOnCompleted());
                    }

                    @Override
                    public void onError(Throwable e) {
                        unsubscribe();
                        terminals.onNext(Notification.createOnError(e));
                    }

                    @Override
                    public void onNext(T v) {
                        if (consumerCapacity.get() != Long.MAX_VALUE) {
                            consumerCapacity.decrementAndGet();
                        }
                        System.out.println("OnSubscribeRedo terminalDelegatingSubscriber onNext="+v);
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
                System.out.println("OnSubscribeRedo  subscribeToSource ---source.unsafeSubscribe(terminalDelegatingSubscriber)"+terminalDelegatingSubscriber);
                source.unsafeSubscribe(terminalDelegatingSubscriber);
            }
        };

        // the observable received by the control handler function will receive notifications of onCompleted in the case of 'repeat' 
        // type operators or notifications of onError for 'retry' this is done by lifting in a custom operator to selectively divert 
        // the retry/repeat relevant values to the control handler
        final Observable<?> restarts = controlHandlerFunction.call(
                terminals.lift(new Operator<Notification<?>, Notification<?>>() {
                    @Override
                    public Subscriber<? super Notification<?>> call(final Subscriber<? super Notification<?>> filteredTerminals) {
                        //
                        System.out.println("OnSubscribeRedo  2--------- restarts Operator call------------》》》》》"+filteredTerminals);
                        return new Subscriber<Notification<?>>(filteredTerminals) {
                            @Override
                            public void onCompleted() {
                                filteredTerminals.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                filteredTerminals.onError(e);
                            }

                            @Override
                            public void onNext(Notification<?> t) {
                                if (t.isOnCompleted() && stopOnComplete)
                                    child.onCompleted();
                                else if (t.isOnError() && stopOnError)
                                    child.onError(t.getThrowable());
                                else {
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
                }));

        // subscribe to the restarts observable to know when to schedule the next redo.
        //worker经过一系列调用会调到call方法
        worker.schedule(new Action0() {
            @Override
            public void call() {
                //1.call方法通过worker调用，restarts作为Obesrvable订阅,workerSubscriber作为参数传到lift
                // Observable的OnSubscribe的call方法中，restarts的call方法中,再调到上面2中Operator的call方法中
                System.out.println("OnSubscribeRedo  1--------- worker.schedule call------------》》》》》");
                Subscriber<Object> workerSubscriber = new Subscriber<Object>(child) {
                    @Override
                    public void onCompleted() {
                        child.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @Override
                    public void onNext(Object t) {
                        System.out.println("OnSubscribeRedo  ---------  worker.schedule  restarts onNext==" + t + " ---------");
                        if (!isLocked.get() && !child.isUnsubscribed()) {
                            if (consumerCapacity.get() > 0) {
                                worker.schedule(subscribeToSource);
                            } else {
                                resumeBoundary.compareAndSet(false, true);
                            }
                        }
                    }

                    @Override
                    public void setProducer(Producer producer) {
                        producer.request(Long.MAX_VALUE);
                    }
                };
                System.out.println("OnSubscribeRedo  ---------  restarts.unsafeSubscribe(workerSubscriber) restarts==" + restarts + " ---------");
                restarts.unsafeSubscribe(workerSubscriber);
            }
        });

        child.setProducer(new Producer() {

            @Override
            public void request(final long n) {
                long c = consumerCapacity.getAndAdd(n);
                Producer producer = currentProducer.get();
                if (producer != null) {
                    producer.request(n);
                } else
                if (c == 0 && resumeBoundary.compareAndSet(true, false)) {
                    worker.schedule(subscribeToSource);
                }
            }
        });
        
    }
}
