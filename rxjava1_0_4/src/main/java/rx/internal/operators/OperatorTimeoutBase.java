/**
 * Copyright 2014 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import android.util.Log;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import rx.Observable;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.SerialSubscription;

class OperatorTimeoutBase<T> implements Operator<T, T> {

    /**
     * Set up the timeout action on the first value.
     *
     * @param <T>
     */
    //注意第一个参数是TimeoutSubscriber
    /* package-private */static interface FirstTimeoutStub<T> extends
            Func3<TimeoutSubscriber<T>, Long, Scheduler.Worker, Subscription> {
    }

    /**
     * Set up the timeout action based on every value
     *
     * @param <T>
     */
    // 注意第一个参数是TimeoutSubscriber
    /* package-private */static interface TimeoutStub<T> extends
            Func4<TimeoutSubscriber<T>, Long, T, Scheduler.Worker, Subscription> {
    }

    private final FirstTimeoutStub<T> firstTimeoutStub;
    private final TimeoutStub<T> timeoutStub;
    private final Observable<? extends T> other;
    private final Scheduler scheduler;

    /* package-private */OperatorTimeoutBase(FirstTimeoutStub<T> firstTimeoutStub,
                                             TimeoutStub<T> timeoutStub,
                                             Observable<? extends T> other, Scheduler scheduler) {
        //没发送数据也会超时，靠firstTimeoutStub这个定时器触发
        this.firstTimeoutStub = firstTimeoutStub;
        this.timeoutStub = timeoutStub;
        this.other = other;
        //scheduler : EventLoopsScheduler
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super T> subscriber) {
        Scheduler.Worker inner = scheduler.createWorker();
        subscriber.add(inner);
        final SerialSubscription serial = new SerialSubscription();
        subscriber.add(serial);
        // Use SynchronizedSubscriber for safe memory access
        // as the subscriber will be accessed in the current thread or the
        // scheduler or other Observables.
        final SerializedSubscriber<T> synchronizedSubscriber =
                new SerializedSubscriber<T>(subscriber);

        TimeoutSubscriber<T> timeoutSubscriber =
                new TimeoutSubscriber<T>(synchronizedSubscriber, timeoutStub, serial, other, inner);
        serial.set(firstTimeoutStub.call(timeoutSubscriber, 0L, inner));
        return timeoutSubscriber;
    }

    /* package-private */static final class TimeoutSubscriber<T> extends
            Subscriber<T> {

        private final SerialSubscription serial;
        private final Object gate = new Object();

        private final SerializedSubscriber<T> serializedSubscriber;

        private final TimeoutStub<T> timeoutStub;

        private final Observable<? extends T> other;
        private final Scheduler.Worker inner;

        volatile int terminated;
        volatile long actual;

        @SuppressWarnings("rawtypes")
        static final AtomicIntegerFieldUpdater<TimeoutSubscriber> TERMINATED_UPDATER
                = AtomicIntegerFieldUpdater.newUpdater(TimeoutSubscriber.class, "terminated");
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<TimeoutSubscriber> ACTUAL_UPDATER
                = AtomicLongFieldUpdater.newUpdater(TimeoutSubscriber.class, "actual");

        private TimeoutSubscriber(
                SerializedSubscriber<T> serializedSubscriber,
                TimeoutStub<T> timeoutStub, SerialSubscription serial,
                Observable<? extends T> other,
                Scheduler.Worker inner) {
            super(serializedSubscriber);
            this.serializedSubscriber = serializedSubscriber;
            this.timeoutStub = timeoutStub;
            this.serial = serial;
            this.other = other;
            this.inner = inner;
        }

        @Override
        public void onNext(T value) {
            boolean onNextWins = false;
            synchronized (gate) {
                if (terminated == 0) {
                    ACTUAL_UPDATER.incrementAndGet(this);
                    onNextWins = true;
                }
            }
            Log.w("TAG", "OperatorTimeoutBase TimeoutSubscriber收到 onNext =" + value + " time:" +
                    System.currentTimeMillis()+"  onNextWins="+onNextWins);
            if (onNextWins) {
                serializedSubscriber.onNext(value);
//                System.out.println("<<<<<<<<<<<<OperatorTimeoutBase TimeoutSubscriber onNext  timeoutStub.call >>>>>>>>>>>>>");
//               timeoutStub.call(this, actual, value, inner);
                //timeoutStub.call(this, actual, value, inner)会生成一个Subscription，serial.set会把上一个Subscription unSubscribe掉
                Log.e("TAG",
                        "TimeoutSubscriber onNext serial.set(timeoutStub.call(this, actual, value, inner) terminated:" +
                                terminated + " actual=" + actual);
                Log.e("TAG", "TimeoutSubscriber onNext-------解注册第一个超时action-----------:" );
                //原来在这里
                serial.set(timeoutStub.call(this, actual, value, inner));
            }
        }

        @Override
        public void onError(Throwable error) {
            Log.e("TAG", "TimeoutSubscriber onError:");
            boolean onErrorWins = false;
            synchronized (gate) {
                if (TERMINATED_UPDATER.getAndSet(this, 1) == 0) {
                    onErrorWins = true;
                }
            }
            if (onErrorWins) {
                serial.unsubscribe();
                serializedSubscriber.onError(error);
            }
        }

        @Override
        public void onCompleted() {
            Log.e("TAG", "TimeoutSubscriber onCompleted:");
            boolean onCompletedWins = false;
            synchronized (gate) {
                if (TERMINATED_UPDATER.getAndSet(this, 1) == 0) {
                    onCompletedWins = true;
                }
            }
            if (onCompletedWins) {
                serial.unsubscribe();
                serializedSubscriber.onCompleted();
            }
        }

        //超时会调用这个方法
        public void onTimeout(long seqId) {
            long expected = seqId;
            boolean timeoutWins = false;
            synchronized (gate) {
                //getAndSet 返回旧的
                if (expected == actual && TERMINATED_UPDATER.getAndSet(this, 1) == 0) {
                    timeoutWins = true;
                }
                Log.e("TAG",
                        "OperatorTimeoutBase TimeoutSubscriber onTimeout:" +
                                System.currentTimeMillis() + " timeoutWins= " + timeoutWins +
                                "  (other == null)=" + (other == null));
                if (timeoutWins) {
                    //只传时间单元的话，这里other == null
                    if (other == null) {
                        //这里会向真正的观察者发送超时事件
                        serializedSubscriber.onError(new TimeoutException());
                    } else {
                        //serializedSubscriber又会传到真正的subsrciber
                        other.unsafeSubscribe(serializedSubscriber);
                        serial.set(serializedSubscriber);
                    }
                }
            }
        }
    }
}
