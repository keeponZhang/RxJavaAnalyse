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
package rx.subjects;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.CompositeException;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.internal.operators.NotificationLite;
import rx.internal.operators.OnSubscribeRedo;
import rx.subjects.SubjectSubscriptionManager.SubjectObserver;

import static rx.internal.operators.OnSubscribeRedo.getOnSubscribeRedoDebugTag;

/**
 * Subject that, once an {@link Observer} has subscribed, emits all subsequently observed items to the
 * subscriber.
 * <p>
 * <img width="640" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/S.PublishSubject.png" alt="">
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

PublishSubject<Object> subject = PublishSubject.create();
// observer1 will receive all onNext and onCompleted events
subject.subscribe(observer1);
subject.onNext("one");
subject.onNext("two");
// observer2 will only receive "three" and onCompleted
subject.subscribe(observer2);
subject.onNext("three");
subject.onCompleted();

} </pre>
 *
 * @param <T>
 *          the type of items observed and emitted by the Subject
 */
public final class PublishSubject<T> extends Subject<T, T> {

    /**
     * Creates and returns a new {@code PublishSubject}.
     *
     * @param <T> the value type
     * @return the new {@code PublishSubject}
     */
    public static <T> PublishSubject<T> create() {
        //这个是重点，SubjectSubscriptionManager是OnSubscribe，下层往上触发的时候，OnSubscribe的call
        // 并没有触发发送数据，而是天剑观察者，需要调用PublishSubject的onNext方法
        final SubjectSubscriptionManager<T> state = new SubjectSubscriptionManager<T>();
        state.onTerminated = new Action1<SubjectObserver<T>>() {

            @Override
            public void call(SubjectObserver<T> o) {
                o.emitFirst(state.get(), state.nl);
            }

        };
        //state也是OnSubscribe
        //真正订阅的时候才添加观察者
        Log.d("TAG", getOnSubscribeRedoDebugTag()+"PublishSubject create:");
        return new PublishSubject<T>(state, state);
    }

    final SubjectSubscriptionManager<T> state;
    private final NotificationLite<T> nl = NotificationLite.instance();

    protected PublishSubject(OnSubscribe<T> onSubscribe, SubjectSubscriptionManager<T> state) {
        super(onSubscribe);
        this.state = state;
    }

    @Override
    public void onCompleted() {
        if (state.active) {
            Object n = nl.completed();
            for (SubjectObserver<T> bo : state.terminate(n)) {
                bo.emitNext(n, state.nl);
            }
        }

    }

    @Override
    public void onError(final Throwable e) {
        if (state.active) {
            Object n = nl.error(e);
            List<Throwable> errors = null;
            for (SubjectObserver<T> bo : state.terminate(n)) {
                try {
                    bo.emitNext(n, state.nl);
                } catch (Throwable e2) {
                    if (errors == null) {
                        errors = new ArrayList<Throwable>();
                    }
                    errors.add(e2);
                }
            }
            if (errors != null) {
                if (errors.size() == 1) {
                    Exceptions.propagate(errors.get(0));
                } else {
                    throw new CompositeException("Errors while emitting PublishSubject.onError",
                            errors);
                }
            }
        }
    }

    @Override
    public void onNext(T v) {
        for (SubjectObserver<T> bo : state.observers()) {
            Log.i("TAG",
                    getOnSubscribeRedoDebugTag()+"PublishSubject 1.8 OnSubscribeRedo 启动重新订阅 " +
                            "onNext " + bo+" 发送的数据="+v);
            //bo：SubjectSubscriptionManager。SubjectObserver
            bo.onNext(v);
        }
    }

    @Override
    public boolean hasObservers() {
        return state.observers().length > 0;
    }

    public Subscription subscribe(Subscriber<? super T> subscriber) {
        Log.e("TAG", "PublishSubject subscribe 开始调用subscribe方法 subscriber:"+subscriber);
        return super.subscribe(subscriber);
    }
}
