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

import java.util.Iterator;

import io.reactivex.*;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.observers.BasicQueueDisposable;

public final class ObservableFromIterable<T> extends Observable<T> {
//    source由第1步中知道它就是我们提供的数据源(在我们的实例中就是students)
    final Iterable<? extends T> source;
    public ObservableFromIterable(Iterable<? extends T> source) {
        System.out.println("ObservableFromIterable 调用构造函数");
        this.source = source;
    }

    @Override
    public void subscribeActual(Observer<? super T> s) {
        Iterator<? extends T> it;
        try {
            it = source.iterator();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, s);
            return;
        }
        boolean hasNext;
        try {
            hasNext = it.hasNext();
        } catch (Throwable e) {
            Exceptions.throwIfFatal(e);
            EmptyDisposable.error(e, s);
            return;
        }
        if (!hasNext) {
            EmptyDisposable.complete(s);
            return;
        }
//        构建FromIterableDisposable，一个Disposable对象，是onSubscribe方法的参数。它的构造参数有两个
//       1. observer：这里就是MergeObserver，作为下游保存起来
//      2.  it：数据源的迭代器
        FromIterableDisposable<T> d = new FromIterableDisposable<T>(s, it);
        //如果这里用了flatMap,走到ObservableFlatMap.MergeObserver的onSubscribe方法
        s.onSubscribe(d);

        if (!d.fusionMode) {
            d.run();
        }
    }

    static final class FromIterableDisposable<T> extends BasicQueueDisposable<T> {

        final Observer<? super T> actual;

        final Iterator<? extends T> it;

        volatile boolean disposed;

        boolean fusionMode;

        boolean done;

        boolean checkNext;

        FromIterableDisposable(Observer<? super T> actual, Iterator<? extends T> it) {
            this.actual = actual;
            this.it = it;
        }

        void run() {
            boolean hasNext;

            do {
                if (isDisposed()) {
                    return;
                }
                T v;

                try {
                    v = ObjectHelper.requireNonNull(it.next(), "The iterator returned a null value");
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    actual.onError(e);
                    return;
                }

                actual.onNext(v);

                if (isDisposed()) {
                    return;
                }
                try {
                    hasNext = it.hasNext();
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    actual.onError(e);
                    return;
                }
            } while (hasNext);

            if (!isDisposed()) {
                actual.onComplete();
            }
        }

        @Override
        public int requestFusion(int mode) {
            if ((mode & SYNC) != 0) {
                fusionMode = true;
                return SYNC;
            }
            return NONE;
        }

        @Override
        public T poll() {
            if (done) {
                return null;
            }
            if (checkNext) {
                if (!it.hasNext()) {
                    done = true;
                    return null;
                }
            } else {
                checkNext = true;
            }

            return ObjectHelper.requireNonNull(it.next(), "The iterator returned a null value");
        }

        @Override
        public boolean isEmpty() {
            return done;
        }

        @Override
        public void clear() {
            done = true;
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }
    }
}
