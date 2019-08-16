/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Notification;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.ListCompositeDisposable;
import io.reactivex.internal.disposables.SequentialDisposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.observers.ToNotificationObserver;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

public final class ObservableRedo<T> extends AbstractObservableWithUpstream<T, T> {
    final Function<? super Observable<Notification<Object>>, ? extends ObservableSource<?>> manager;

    final boolean retryMode;

    public ObservableRedo(ObservableSource<T> source,
            Function<? super Observable<Notification<Object>>, ? extends ObservableSource<?>> manager,
                    boolean retryMode) {
        super(source);
        this.manager = manager;
        this.retryMode = retryMode;
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {
//  当观察者订阅BehaviorSubject时，它开始发射原始Observable最近发射的数据（如果此时还没有收到任何数据，它会发射一个默认值），
//  然后继续发射其它任何来自原始Observable的数据。
        Subject<Notification<Object>> subject = BehaviorSubject.<Notification<Object>>create().toSerialized();

        final RedoObserver<T> parent = new RedoObserver<T>(s, subject, source, retryMode);

        //这个观察者只是用来重新订阅
        ToNotificationObserver<Object> actionObserver = new ToNotificationObserver<Object>(new Consumer<Notification<Object>>() {
            @Override
            public void accept(Notification<Object> o) {
                //其实这里是由ToNotificationObserver的onNext方法调用的
                // 这里其实比parent.handle(Notification.<Object>createOnNext(0))晚调用，等发送了数据才会到这里
                Log.w("TAG", "<<<<<<<ObservableRedo PollingActivity ToNotificationObserver aceept 被调用:"+o);
                // 主要作用:retrywhen上游的Observable订阅RedoObserver
                parent.handle(o);

            }
        });
        ListCompositeDisposable cd = new ListCompositeDisposable(parent.arbiter, actionObserver);
        s.onSubscribe(cd);

        ObservableSource<?> action;

        try {

            Log.e("TAG", "PollingActivity ObservableRedo subscribeActual subject:"+subject);
            //这里传入的是subject，不是source 特别注意
            //handler:repeatWhen中的 function   no :Observable<Notification<Object>> handler:
//            handler.apply(no.map(ObservableInternalHelper.MapToInt.INSTANCE));
            action = ObjectHelper.requireNonNull(manager.apply(subject), "The function returned a null ObservableSource");
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            s.onError(ex);
            return;
        }

        //action:repeatWhen生成的Observable  接着会走到ToNotificationObserver的accept方法
        //分析:如果action是flatMapObservable ，如果是这样处理的repeatWhen的Function的apply方法是下面这样的， parent.handle(Notification.<Object>createOnNext(0))方法调用后
        //相当于repeatWhen前面的Observable被actionObserver（ToNotificationObserver）和RedoObserver两个订阅，所以最原始发送的消息除了RedoObserver的onNext收的到，
        // ToNotificationObserver的onNext也收的到，onNext调用accept,而ToNotificationObserver的accept调用source.subscribe(this)，重新订阅，
        // repeatWhen前面的Observable又会重新发送事件，RedoObserver的OnNext又收到
        // return objectObservable.flatMap(new Function<Object, ObservableSource<Long>>() {   //objectObservable是上级的，这里有用到
        //       @Override
        //       public ObservableSource<Long> apply(Object o) throws Exception {
        //           mRepeatCount++;
        //           if(mRepeatCount>3){
        //               return Observable.error(new Throwable("Polling work finished"));
        //           }
        //           return Observable.just(4L);
        //       }
        //   });
        action.subscribe(actionObserver);

        // trigger 触发 first subscription
        //handle一次， source.subscribe(this);  source: retrywhen上游的Observable ,this :RedoObserver
        //action有主动发射，这里触发的是第二次重复；如果没有则触发第一次，一般来说，中间层不会主动发射
        //  parent.handle(Notification.<Object>createOnNext(0))可以调用  source.subscribe(this);诱发最顶层被观察者发射 ,从而不仅RedoObserver可以收到，ToNotificationObserver也可以收到
        Log.w("TAG", "PollingActivity ObservableRedo subscribeActual handle 第一次 before#########################");
       parent.handle(Notification.<Object>createOnNext(0));
        Log.w("TAG", "PollingActivity ObservableRedo subscribeActual handle 第一次 #################end");
    }

    static final class RedoObserver<T> extends AtomicBoolean implements Observer<T> {

        private static final long serialVersionUID = -1151903143112844287L;
        final Observer<? super T> actual;
        final Subject<Notification<Object>> subject;
        final ObservableSource<? extends T> source;
        final SequentialDisposable arbiter;

        final boolean retryMode;

        final AtomicInteger wip = new AtomicInteger();

        RedoObserver(Observer<? super T> actual, Subject<Notification<Object>> subject, ObservableSource<? extends T> source, boolean retryMode) {
            this.actual = actual;
            this.subject = subject;
            this.source = source;
            this.arbiter = new SequentialDisposable();
            this.retryMode = retryMode;
            this.lazySet(true);
        }

        @Override
        public void onSubscribe(Disposable s) {
            arbiter.replace(s);
        }

        @Override
        public void onNext(T t) {
            Log.e("TAG", "PollingActivity 发送给真正的观察者 RedoObserver onNext:"+t);
            //下游真正的观察者
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            if (compareAndSet(false, true)) {
                if (retryMode) {
                    subject.onNext(Notification.createOnError(t));
                } else {
                    subject.onError(t);
                }
            }
        }

        @Override
        public void onComplete() {
            Log.e("TAG", "PollingActivity RedoObserver onComplete:");
            if (compareAndSet(false, true)) {
                if (retryMode) {
                    subject.onComplete();
                } else {
                    subject.onNext(Notification.createOnComplete());
                }
            }
        }

        void handle(Notification<Object> notification) {
            Log.e("TAG", "<<<<<<<ObservableRedo PollingActivity Notification handle 被调用:"+notification);
            //如果发送第一次事件是error的，第二次重复的时候是进不来的
            if (compareAndSet(true, false)) {
                if (notification.isOnError()) {
                    arbiter.dispose();
                    actual.onError(notification.getError());
                } else {
                    if (notification.isOnNext()) {
                        if (wip.getAndIncrement() == 0) {
                            int missed = 1;
                            for (;;) {
                                Log.e("TAG", " PollingActivity RedoObserver handle arbiter.isDisposed():"+arbiter.isDisposed());
                                if (arbiter.isDisposed()) {
                                    return;
                                }
//                                source: retrywhen上游的Observable ,this :RedoObserver
                                //如果发送的事件时onNext的，会一直重复订阅
                                Log.e("TAG", "PollingActivity RedoObserver handle repeat 上游订阅 source.subscribe(this)++++source =="+source);
                                source.subscribe(this);

                                missed = wip.addAndGet(-missed);
                                if (missed == 0) {
                                    break;
                                }
                            }
                        }
                    } else {
                        arbiter.dispose();
                        actual.onComplete();
                    }
                }
            }
        }
    }
}
