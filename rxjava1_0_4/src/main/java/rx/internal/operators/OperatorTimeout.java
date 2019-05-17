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

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;

/**
 * Applies a timeout policy for each element in the observable sequence, using
 * the specified scheduler to run timeout timers. If the next element isn't
 * received within the specified timeout duration starting from its predecessor,
 * the other observable sequence is used to produce future messages from that
 * point on.
 */
public final class OperatorTimeout<T> extends OperatorTimeoutBase<T> {

    public OperatorTimeout(final long timeout, final TimeUnit timeUnit, Observable<? extends T> other, Scheduler scheduler) {
        super(new FirstTimeoutStub<T>() {
//  在 OperatorTimeoutBase的call方法中会调用，inner为EventLoopsScheduler的EventLoopWorker
            @Override
            public Subscription call(final TimeoutSubscriber<T> timeoutSubscriber, final Long seqId, Scheduler.Worker inner) {
                System.out.println("OperatorTimeout call Scheduler.Worker="+inner);
                return inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        System.out.println("-------OperatorTimeout OperatorTimeoutBase call firstTimeoutStub 调用timeout啦------------");
                        timeoutSubscriber.onTimeout(seqId);
                    }
                }, timeout, timeUnit);
            }
        }, new TimeoutStub<T>() {

            @Override
            public Subscription call(final TimeoutSubscriber<T> timeoutSubscriber, final Long seqId, T value, Scheduler.Worker inner) {
                Action0 action0 = new Action0() {
                    @Override
                    public void call() {
                        System.out.println("<<<<<<<<<<< OperatorTimeout NewThreadWorker OperatorTimeoutBase call otherTimeoutStub 调用timeout啦>>>>>>>>>>");
                        timeoutSubscriber.onTimeout(seqId);
                    }
                };
              System.out.println("------- NewThreadWorker OperatorTimeoutBase call action0 ------------" + action0);
                return inner.schedule(action0, timeout, timeUnit);
            }
        }, other, scheduler);
    }
}
