/**
 * Copyright 2016 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.queue.SpscLinkedArrayQueue;

public final class ObservableZip<T, R> extends Observable<R> {

    final ObservableSource<? extends T>[] sources;
    final Iterable<? extends ObservableSource<? extends T>> sourcesIterable;
    final Function<? super Object[], ? extends R> zipper;
    final int bufferSize;
    final boolean delayError;

    public ObservableZip(ObservableSource<? extends T>[] sources,
                         Iterable<? extends ObservableSource<? extends T>> sourcesIterable,
                         Function<? super Object[], ? extends R> zipper,
                         int bufferSize,
                         boolean delayError) {
        this.sources = sources;
        this.sourcesIterable = sourcesIterable;
        this.zipper = zipper;
        this.bufferSize = bufferSize;
        this.delayError = delayError;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribeActual(Observer<? super R> s) {
        ObservableSource<? extends T>[] sources = this.sources;
        int count = 0;
        if (sources == null) {
            sources = new Observable[8];
            for (ObservableSource<? extends T> p : sourcesIterable) {
                if (count == sources.length) {
                    ObservableSource<? extends T>[] b = new ObservableSource[count + (count >> 2)];
                    System.arraycopy(sources, 0, b, 0, count);
                    sources = b;
                }
                sources[count++] = p;
            }
        } else {
            count = sources.length;
        }

        if (count == 0) {
            EmptyDisposable.complete(s);
            return;
        }

        ZipCoordinator<T, R> zc = new ZipCoordinator<T, R>(s, zipper, count, delayError);
        zc.subscribe(sources, bufferSize);
    }

    static final class ZipCoordinator<T, R> extends AtomicInteger implements Disposable {

        private static final long serialVersionUID = 2983708048395377667L;
        final Observer<? super R> actual;
        final Function<? super Object[], ? extends R> zipper;
        final ZipObserver<T, R>[] observers;
        final T[] row;
        final boolean delayError;

        volatile boolean cancelled;

        //        构造函数中初始化了一个和上游 ObservableSource 一样数量大小（在本案例中是2） 的 ZipObserver 数组
//        和 T 类型(Observable的类型)的数组。
        @SuppressWarnings("unchecked")
        ZipCoordinator(Observer<? super R> actual,
                       Function<? super Object[], ? extends R> zipper,
                       int count, boolean delayError) {
            this.actual = actual;
            this.zipper = zipper;
            this.observers = new ZipObserver[count];
            this.row = (T[]) new Object[count];
            this.delayError = delayError;
        }

        //        初始化了 ZipObserver 数组并让上游 ObservableSource 分别订阅了对应的 ZipObserver。
        public void subscribe(ObservableSource<? extends T>[] sources, int bufferSize) {
            ZipObserver<T, R>[] s = observers;
            int len = s.length;
            for (int i = 0; i < len; i++) {
                s[i] = new ZipObserver<T, R>(this, bufferSize);
            }
            // this makes sure the contents of the observers array is visible
            this.lazySet(0);
            actual.onSubscribe(this);
            for (int i = 0; i < len; i++) {
                if (cancelled) {
                    return;
                }
//                让上游 ObservableSource 分别订阅了对应的 ZipObserver。
                sources[i].subscribe(s[i]);
            }
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                if (getAndIncrement() == 0) {
                    clear();
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void clear() {
            for (ZipObserver<?, ?> zs : observers) {
                zs.dispose();
                zs.queue.clear();
            }
        }

        public void drain() {
            if (getAndIncrement() != 0) {
                return;
            }

            int missing = 1;
            Log.e("TAG", "ZipCoordinator drain ------------------start:");
            final ZipObserver<T, R>[] zs = observers;
            final Observer<? super R> a = actual;
            final T[] os = row;
            final boolean delayError = this.delayError;
//            zip发送的事件数量跟上游中发送事件最少的那一根水管的事件数量是有关的
            for (; ; ) {

                for (; ; ) {
                    int i = 0;
                    Log.e("TAG", "ZipCoordinator drain for (;;) i:" + i);
                    int emptyCount = 0;
                    for (ZipObserver<T, R> z : zs) {
                        //不等于空才会进来
                        if (os[i] == null) {
                            //done 表示其中一方已经完成发送所有事件
                            boolean d = z.done;
                            T v = z.queue.poll();
                            boolean empty = v == null;
                            boolean checkTerminated = checkTerminated(d, empty, a, delayError, z);
                            Log.e("TAG",
                                    "ZipCoordinator drain os[i]为空i = " + "icheckTerminated=" +
                                            checkTerminated +
                                            " v=" + v + " delayError=" + delayError);
                            //返回true，表示发送事件少的那方已经发送完成了
                            if (checkTerminated) {
                                return;
                            }
                            if (!empty) {
                                os[i] = v;
                                Log.d("TAG", "ZipCoordinator drain os[i]:" + i + " 设置为=" + v);
                            } else {
                                emptyCount++;
                            }
                        } else {
                            Log.e("TAG",
                                    "ZipCoordinator drain os[i] 不为空:" + i + " z.done=" + z.done);
                            if (z.done && !delayError) {
                                Throwable ex = z.error;
                                if (ex != null) {
                                    clear();
                                    a.onError(ex);
                                    return;
                                }
                            }
                        }
                        i++;
                    }
                    Log.w("TAG", "ZipCoordinator drain emptyCount!=0:" + (emptyCount != 0));
                    //有一个empty就不会真正转换事件,emptyCount!=0,表示有一方是没有发送的
                    if (emptyCount != 0) {
                        break;
                    }

                    R v;
                    try {
                        //这个是zip中的apply方法
                        v = ObjectHelper.requireNonNull(zipper.apply(os.clone()),
                                "The zipper returned a null value");
                    } catch (Throwable ex) {
                        Exceptions.throwIfFatal(ex);
                        clear();
                        a.onError(ex);
                        return;
                    }

                    //转换后发送给下游真正的观察者;发送下去表示这个循环还得继续
                    a.onNext(v);
                    Log.w("TAG", "ZipCoordinator drain 转换后发送给下游真正的观察者------------------:" + v);
                    Arrays.fill(os, null);
                }

                missing = addAndGet(-missing);
                Log.e("TAG", "ZipCoordinator drain missing:" + missing);
                if (missing == 0) {
                    return;
                }
            }
        }

        boolean checkTerminated(boolean d, boolean empty, Observer<? super R> a, boolean delayError,
                                ZipObserver<?, ?> source) {
            if (cancelled) {
                clear();
                return true;
            }

            if (d) {
                if (delayError) {
                    if (empty) {
                        Throwable e = source.error;
                        clear();
                        if (e != null) {
                            a.onError(e);
                        } else {
                            //发送完成事件给下游真正的观察者
                            a.onComplete();
                        }
                        return true;
                    }
                } else {
                    Throwable e = source.error;
                    if (e != null) {
                        clear();
                        a.onError(e);
                        return true;
                    } else if (empty) {
                        clear();
                        a.onComplete();
                        return true;
                    }
                }
            }

            return false;
        }
    }

    static final class ZipObserver<T, R> implements Observer<T> {

        final ZipCoordinator<T, R> parent;
        final SpscLinkedArrayQueue<T> queue;

        volatile boolean done;
        Throwable error;

        final AtomicReference<Disposable> s = new AtomicReference<Disposable>();

        ZipObserver(ZipCoordinator<T, R> parent, int bufferSize) {
            this.parent = parent;
            this.queue = new SpscLinkedArrayQueue<T>(bufferSize);
        }

        @Override
        public void onSubscribe(Disposable s) {
            DisposableHelper.setOnce(this.s, s);
        }

        //        一是入队，二是调用 ZipCoordinator#drain() 方法
        @Override
        public void onNext(T t) {
            //offer相当于add，不抛异常
            queue.offer(t);
//  parent:  ZipCoordinator
            Log.d("TAG", "ZipObserver drain onNext:" + t);
            parent.drain();
        }

        @Override
        public void onError(Throwable t) {
            error = t;
            done = true;
            parent.drain();
        }

        @Override
        public void onComplete() {
            done = true;
            parent.drain();
        }

        public void dispose() {
            DisposableHelper.dispose(s);
        }
    }
}
