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

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

public final class ObservableSubscribeOn<T> extends AbstractObservableWithUpstream<T, T> {
    final Scheduler scheduler;

    public ObservableSubscribeOn(ObservableSource<T> source, Scheduler scheduler) {
        super(source);
        this.scheduler = scheduler;
    }

    @Override
    public void subscribeActual(final Observer<? super T> s) {
        final SubscribeOnObserver<T> parent = new SubscribeOnObserver<T>(s);

        Log.e("TAG", "ObservableSubscribeOn subscribeActual onSubscribe before:");
        s.onSubscribe(parent);
        Log.e("TAG", "ObservableSubscribeOn subscribeActual onSubscribe after:");
        //如果Observer被代理了，一般代理的那个变量会叫parent，持有真正的observer
        parent.setDisposable(scheduler.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                source.subscribe(parent);
            }
        }));
    }

    static final class SubscribeOnObserver<T> extends AtomicReference<Disposable> implements Observer<T>, Disposable {

        private static final long serialVersionUID = 8094547886072529208L;
        final Observer<? super T> actual;

        final AtomicReference<Disposable> s;

        SubscribeOnObserver(Observer<? super T> actual) {
            this.actual = actual;
            this.s = new AtomicReference<Disposable>();
        }

        @Override
        public void onSubscribe(Disposable s) {
            //这个是上层的Obserable调用的
            Log.d("TAG", "ObservableSubscribeOn subscribeActual  SubscribeOnObserver onSubscribe:");
            DisposableHelper.setOnce(this.s, s);
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            actual.onComplete();
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(s);
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }

        void setDisposable(Disposable d) {
            DisposableHelper.setOnce(this, d);
        }
    }
}
