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

import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Cancellable;
import io.reactivex.internal.disposables.*;
import io.reactivex.internal.fuseable.SimpleQueue;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;
import io.reactivex.internal.util.AtomicThrowable;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableCreate<T> extends Observable<T> {
    //ObservableCreate中有ObservableOnSubscribe
    final ObservableOnSubscribe<T> source;

    public ObservableCreate(ObservableOnSubscribe<T> source) {
        System.out.println("ObservableCreate 调用构造函数");
        this.source = source;
    }

    @Override
    protected void subscribeActual(Observer<? super T> observer) {
        CreateEmitter<T> parent = new CreateEmitter<T>(observer);
        //表示订阅开始（这里会调用下一个observer.onSubsribe）
        //传给onSubscribe的跟传给ObservableOnSubscribe的subscribe方法的，都是一样的，只是不是接口或者继承
        observer.onSubscribe(parent);

        try {
            //事件发送的逻辑开始执行，这个source就是创建Observable时新建的ObservableOnSubscribe对象。
            //source:ObservableOnSubscribe
            source.subscribe(parent);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            parent.onError(ex);
        }
    }

    static final class CreateEmitter<T>
    extends AtomicReference<Disposable>
    implements ObservableEmitter<T>, Disposable {


        private static final long serialVersionUID = -3434801548987643227L;

        final Observer<? super T> observer;

        CreateEmitter(Observer<? super T> observer) {
            this.observer = observer;
        }

        @Override
        public void onNext(T t) {
            if (t == null) {
                onError(new NullPointerException("onNext called with null. Null values are generally not allowed in 2.x operators and sources."));
                return;
            }
            if (!isDisposed()) {
                System.out.println("-------ObservableCreate onNext----------"+t);
                observer.onNext(t);
            }
        }

        @Override
        public void onError(Throwable t) {
            if (t == null) {
                t = new NullPointerException("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
            }
            if (!isDisposed()) {
                try {
                    observer.onError(t);
                } finally {
                    dispose();
                }
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!isDisposed()) {
                try {
                    observer.onComplete();
                } finally {
                    dispose();
                }
            }
        }

        @Override
        public void setDisposable(Disposable d) {
            DisposableHelper.set(this, d);
        }

        @Override
        public void setCancellable(Cancellable c) {
            setDisposable(new CancellableDisposable(c));
        }

        @Override
        public ObservableEmitter<T> serialize() {
            return new SerializedEmitter<T>(this);
        }

        @Override
        public void dispose() {
            DisposableHelper.dispose(this);
        }

        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
    }

    /**
     * Serializes calls to onNext, onError and onComplete.
     *
     * @param <T> the value type
     */
    static final class SerializedEmitter<T>
    extends AtomicInteger
    implements ObservableEmitter<T> {

        private static final long serialVersionUID = 4883307006032401862L;

        final ObservableEmitter<T> emitter;

        final AtomicThrowable error;

        final SpscLinkedArrayQueue<T> queue;

        volatile boolean done;

        SerializedEmitter(ObservableEmitter<T> emitter) {
            this.emitter = emitter;
            this.error = new AtomicThrowable();
            this.queue = new SpscLinkedArrayQueue<T>(16);
        }

        @Override
        public void onNext(T t) {
            if (emitter.isDisposed() || done) {
                return;
            }
            if (t == null) {
                onError(new NullPointerException("onNext called with null. Null values are generally not allowed in 2.x operators and sources."));
                return;
            }
            if (get() == 0 && compareAndSet(0, 1)) {
                emitter.onNext(t);
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimpleQueue<T> q = queue;
                synchronized (q) {
                    q.offer(t);
                }
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        @Override
        public void onError(Throwable t) {
            if (emitter.isDisposed() || done) {
                RxJavaPlugins.onError(t);
                return;
            }
            if (t == null) {
                t = new NullPointerException("onError called with null. Null values are generally not allowed in 2.x operators and sources.");
            }
            if (error.addThrowable(t)) {
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (emitter.isDisposed() || done) {
                return;
            }
            done = true;
            drain();
        }

        void drain() {
            if (getAndIncrement() == 0) {
                drainLoop();
            }
        }

        void drainLoop() {
            ObservableEmitter<T> e = emitter;
            SpscLinkedArrayQueue<T> q = queue;
            AtomicThrowable error = this.error;
            int missed = 1;
            for (;;) {

                for (;;) {
                    if (e.isDisposed()) {
                        q.clear();
                        return;
                    }

                    if (error.get() != null) {
                        q.clear();
                        e.onError(error.terminate());
                        return;
                    }

                    boolean d = done;
                    T v = q.poll();

                    boolean empty = v == null;

                    if (d && empty) {
                        e.onComplete();
                        return;
                    }

                    if (empty) {
                        break;
                    }

                    e.onNext(v);
                }

                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        @Override
        public void setDisposable(Disposable s) {
            emitter.setDisposable(s);
        }

        @Override
        public void setCancellable(Cancellable c) {
            emitter.setCancellable(c);
        }

        @Override
        public boolean isDisposed() {
            return emitter.isDisposed();
        }

        @Override
        public ObservableEmitter<T> serialize() {
            return this;
        }
    }

}
