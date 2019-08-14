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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.*;
import io.reactivex.observers.SerializedObserver;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableDebounceTimed<T> extends AbstractObservableWithUpstream<T, T> {
    final long timeout;
    final TimeUnit unit;
    final Scheduler scheduler;

    public ObservableDebounceTimed(ObservableSource<T> source, long timeout, TimeUnit unit, Scheduler scheduler) {
        super(source);
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new DebounceTimedObserver<T>(
                new SerializedObserver<T>(t),
                timeout, unit, scheduler.createWorker()));
    }

    static final class DebounceTimedObserver<T>
    implements Observer<T>, Disposable {
        final Observer<? super T> actual;
        final long timeout;
        final TimeUnit unit;
        final Scheduler.Worker worker;

        Disposable s;

        final AtomicReference<Disposable> timer = new AtomicReference<Disposable>();

        volatile long index;

        boolean done;

        DebounceTimedObserver(Observer<? super T> actual, long timeout, TimeUnit unit, Worker worker) {
            this.actual = actual;
            this.timeout = timeout;
            this.unit = unit;
            this.worker = worker;
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            long idx = index + 1;

            index = idx;


            Disposable d = timer.get();
            if (d != null) {
                Log.e("TAG", "DebounceTimedObserver onNext before dispose:"+d+" d.isDisposed="+d.isDisposed()+" 数据t:"+t);
                //这句也是重点，只要有数据进来并且worker.schedule还没调用run方法，就dispose掉，达到防抖效果
                d.dispose();
            }
            //d第一次是null,之后是DISPOSED
            //de new 完也是null
            DebounceEmitter<T> de = new DebounceEmitter<T>(t, idx, this);
            Log.d("TAG", "DebounceTimedObserver onNext before d:"+d+" de="+de);
            boolean compareAndSet = timer.compareAndSet(d, de);
            Log.e("TAG", "DebounceTimedObserver onNext after compareAndSet d:"+d+" de="+de+"  compareAndSet===="+compareAndSet);
            if (compareAndSet) {
                d = worker.schedule(de, timeout, unit);
                Log.w("TAG", "DebounceTimedObserver onNext after schedule d:"+d+" de="+de);
                //重点是这句（timer的值闲杂是de了，所以需要这句）
                de.setResource(d);
                Log.e("TAG", "DebounceTimedObserver onNext after setResource d:"+d+" de="+de+ "------------------end");
            }

        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            DisposableHelper.dispose(timer);
            actual.onError(t);
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;

            Disposable d = timer.get();
            if (d != DisposableHelper.DISPOSED) {
                @SuppressWarnings("unchecked")
                DebounceEmitter<T> de = (DebounceEmitter<T>)d;
                if (de != null) {
                    de.run();
                }
                DisposableHelper.dispose(timer);
                worker.dispose();
                actual.onComplete();
            }
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(timer);
            worker.dispose();
            s.dispose();
        }

        @Override
        public boolean isDisposed() {
            return timer.get() == DisposableHelper.DISPOSED;
        }

        void emit(long idx, T t, DebounceEmitter<T> emitter) {
            if (idx == index) {
                actual.onNext(t);
                emitter.dispose();
            }
        }
    }

    static final class DebounceEmitter<T> extends AtomicReference<Disposable> implements Runnable, Disposable {

        private static final long serialVersionUID = 6812032969491025141L;

        final T value;
        final long idx;
        final DebounceTimedObserver<T> parent;

        final AtomicBoolean once = new AtomicBoolean();

        DebounceEmitter(T value, long idx, DebounceTimedObserver<T> parent) {
            this.value = value;
            this.idx = idx;
            this.parent = parent;
        }

        @Override
        public void run() {
            Log.e("TAG", "DebounceTimedObserver DebounceEmitter run:");
            if (once.compareAndSet(false, true)) {
                parent.emit(idx, value, this);
            }
        }

        @Override
        public void dispose() {
            Log.e("TAG", "DebounceEmitter dispose:");
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return get() == DisposableHelper.DISPOSED;
        }

        public void setResource(Disposable d) {
            DisposableHelper.replace(this, d);
        }
    }
}
