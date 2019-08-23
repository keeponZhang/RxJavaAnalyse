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

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Action0;
import rx.internal.tag.TagAction0;

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
                System.out.println("OperatorTimeout call FirstTimeoutStub Scheduler.Worker="+inner);
                //这里有个定时任务，超过时间没有取消掉的话，会调用call方法，从而调用timeoutSubscriber.onTimeout(seqId)方法
                return inner.schedule(new Action0() {
                    @Override
                    public void call() {
                        Log.e("TAG", "-------OperatorTimeout OperatorTimeoutBase call firstTimeoutStub 没发送一个数据调用timeout啦------------");
                        timeoutSubscriber.onTimeout(seqId);
                    }
                }, timeout, timeUnit);
            }
        }, new TimeoutStub<T>() {

            @Override
            public Subscription call(final TimeoutSubscriber<T> timeoutSubscriber, final Long seqId, T value, Scheduler.Worker inner) {
//                Action0 action0 = new Action0() {
//                    @Override
//                    public void call() {
//                        System.out.println("<<<<<<<<<<< OperatorTimeout NewThreadWorker OperatorTimeoutBase call otherTimeoutStub 调用timeout啦>>>>>>>>>>");
//                        timeoutSubscriber.onTimeout(seqId);
//                    }
//                };
                Action0 action0 = new TagAction0(){
                    @Override
                    public void call() {
                        super.call();
                        Log.e("TAG", "<<<<<<<<<<< OperatorTimeout NewThreadWorker OperatorTimeoutBase call  TagAction otherTimeoutStub 调用timeout啦>>>>>>>>>>)");
                        timeoutSubscriber.onTimeout(seqId);
                    }
                };
              System.out.println("------- NewThreadWorker OperatorTimeoutBase  TagAction call  TimeoutStub action0 ------------" + action0 +" "+System.currentTimeMillis());
                return inner.schedule(action0, timeout, timeUnit);
            }
        }, other, scheduler);
    }
}
