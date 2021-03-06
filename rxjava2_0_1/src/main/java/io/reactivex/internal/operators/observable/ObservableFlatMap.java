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

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.*;

import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.queue.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableFlatMap<T, U> extends AbstractObservableWithUpstream<T, U> {
    final Function<? super T, ? extends ObservableSource<? extends U>> mapper;
    final boolean delayErrors;
    final int maxConcurrency;
    final int bufferSize;

    public ObservableFlatMap(ObservableSource<T> source,
            Function<? super T, ? extends ObservableSource<? extends U>> mapper,
            boolean delayErrors, int maxConcurrency, int bufferSize) {
        super(source);
        System.out.println("ObservableFlatMap 调用构造函数");
        this.mapper = mapper;
        this.delayErrors = delayErrors;
        this.maxConcurrency = maxConcurrency;
        this.bufferSize = bufferSize;
    }

    @Override
    public void subscribeActual(Observer<? super U> t) {
        Log.e("TAG", "CacheActivity ObservableFlatMap subscribeActual  --------------t:"+t+" source="+source);
        if (ObservableScalarXMap.tryScalarXMapSubscribe(source, t, mapper)) {
            return;
        }
        //其实到这里为止，实现类似于map
        source.subscribe(new MergeObserver<T, U>(t, mapper, delayErrors, maxConcurrency, bufferSize));
    }

    static final class MergeObserver<T, U> extends AtomicInteger implements Disposable, Observer<T> {

        private static final long serialVersionUID = -2117620485640801370L;
//        最原始的订阅者
        final Observer<? super U> actual;
//        就是之前我们给flatmap的映射函数
        final Function<? super T, ? extends ObservableSource<? extends U>> mapper;
        final boolean delayErrors;
//        maxConcurrency：就是我们构建ObservableFlatMap所传的值，Integer.MAX_VALUE
        final int maxConcurrency;
        final int bufferSize;

        volatile SimplePlainQueue<U> queue;

        volatile boolean done;

        final AtomicThrowable errors = new AtomicThrowable();

        volatile boolean cancelled;
//        observers：InnerObserver类型数组的一个原子引用
        final AtomicReference<InnerObserver<?, ?>[]> observers;

        static final InnerObserver<?, ?>[] EMPTY = new InnerObserver<?, ?>[0];

        static final InnerObserver<?, ?>[] CANCELLED = new InnerObserver<?, ?>[0];

        Disposable s;

        long uniqueId;
        long lastId;
        int lastIndex;

        Queue<ObservableSource<? extends U>> sources;

        int wip;

        MergeObserver(Observer<? super U> actual, Function<? super T, ? extends ObservableSource<? extends U>> mapper,
                boolean delayErrors, int maxConcurrency, int bufferSize) {
            this.actual = actual;
            this.mapper = mapper;
            this.delayErrors = delayErrors;
            this.maxConcurrency = maxConcurrency;
            this.bufferSize = bufferSize;
            if (maxConcurrency != Integer.MAX_VALUE) {
                sources = new ArrayDeque<ObservableSource<? extends U>>(maxConcurrency);
            }
            this.observers = new AtomicReference<InnerObserver<?, ?>[]>(EMPTY);
        }

        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                //  actual:最原始的订阅者
                actual.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T t) {
            // safeguard against misbehaving sources
            if (done) {
                return;
            }
            Log.d("TAG","CacheActivity-----------ObservableFlatMap收到一个上游发送来的消息------------"+t);
            ObservableSource<? extends U> p;
            try {
                p = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper returned a null ObservableSource");
                System.out.println("-----------ObservableFlatMap收到一个上游发送来的消息,并且转化成ObservableSource------------"+p);
            } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                s.dispose();
                onError(e);
                return;
            }

            if (maxConcurrency != Integer.MAX_VALUE) {
                synchronized (this) {
                    if (wip == maxConcurrency) {
                        sources.offer(p);
                        return;
                    }
                    wip++;
                }
            }
            //与map的对比：map转化后直接发送给下一层的订阅者;
            // flatmap转换后是一个ObservableSource，下层观察者订阅的不是这个类型，所以还需要InnerObserver，InnerObserver订阅ObservableSource的类型，再发送给下一层真正的Observer
//            v = ObjectHelper.requireNonNull(mapper.apply(t), "The mapper function returned a null value.");
            //            actual.onNext(v);
            subscribeInner(p);
        }

        @SuppressWarnings("unchecked")
        void subscribeInner(ObservableSource<? extends U> p) {
            for (;;) {
                if (p instanceof Callable) {
                    tryEmitScalar(((Callable<? extends U>)p));

                    if (maxConcurrency != Integer.MAX_VALUE) {
                        synchronized (this) {
                            p = sources.poll();
                            if (p == null) {
                                wip--;
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                } else {
//                    没有实现Callable，所有走到了else分支
                    InnerObserver<T, U> inner = new InnerObserver<T, U>(this, uniqueId++);
                    addInner(inner);
                    //使用flatmap,上游发送一个事件，经过flatmap，转化成ObservableSource后，还需要进行订阅（因为下游订阅的事件是具体的，而不是数组或集合）
                    //这里订阅后，假如上游只发送一个事件，转化后的事件会先发给InnerObserver。
                    //p,flatmap转换后的ObservableSource,这里是个ObservableFromIterable，subscribe调用后，会给InnerObserver发送事件
                    p.subscribe(inner);
                    break;
                }
            }
        }

        void addInner(InnerObserver<T, U> inner) {
            for (;;) {
                InnerObserver<?, ?>[] a = observers.get();
                if (a == CANCELLED) {
                    inner.dispose();
                    return;
                }
                int n = a.length;
                InnerObserver<?, ?>[] b = new InnerObserver[n + 1];
                System.arraycopy(a, 0, b, 0, n);
                b[n] = inner;
                if (observers.compareAndSet(a, b)) {
                    return;
                }
            }
        }

        void removeInner(InnerObserver<T, U> inner) {
            for (;;) {
                InnerObserver<?, ?>[] a = observers.get();
                int n = a.length;
                if (n == 0) {
                    return;
                }
                int j = -1;
                for (int i = 0; i < n; i++) {
                    if (a[i] == inner) {
                        j = i;
                        break;
                    }
                }
                if (j < 0) {
                    return;
                }
                InnerObserver<?, ?>[] b;
                if (n == 1) {
                    b = EMPTY;
                } else {
                    b = new InnerObserver<?, ?>[n - 1];
                    System.arraycopy(a, 0, b, 0, j);
                    System.arraycopy(a, j + 1, b, j, n - j - 1);
                }
                if (observers.compareAndSet(a, b)) {
                    return;
                }
            }
        }

        void tryEmitScalar(Callable<? extends U> value) {
            U u;
            try {
                u = value.call();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                errors.addThrowable(ex);
                drain();
                return;
            }

            if (u == null) {
                return;
            }


            if (get() == 0 && compareAndSet(0, 1)) {
                actual.onNext(u);
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimplePlainQueue<U> q = queue;
                if (q == null) {
                    if (maxConcurrency == Integer.MAX_VALUE) {
                        q = new SpscLinkedArrayQueue<U>(bufferSize);
                    } else {
                        q = new SpscArrayQueue<U>(maxConcurrency);
                    }
                    queue = q;
                }

                if (!q.offer(u)) {
                    onError(new IllegalStateException("Scalar queue full?!"));
                    return;
                }
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        void tryEmit(U value, InnerObserver<T, U> inner) {
            if (get() == 0 && compareAndSet(0, 1)) {
                //最原始的 最原始的订阅者就收到事件了
                actual.onNext(value);
                if (decrementAndGet() == 0) {
                    return;
                }
            } else {
                SimpleQueue<U> q = inner.queue;
                if (q == null) {
                    q = new SpscLinkedArrayQueue<U>(bufferSize);
                    inner.queue = q;
                }
                q.offer(value);
                if (getAndIncrement() != 0) {
                    return;
                }
            }
            drainLoop();
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            if (errors.addThrowable(t)) {
                done = true;
                drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            drain();
        }

        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                if (disposeAll()) {
                    Throwable ex = errors.terminate();
                    if (ex != null && ex != ExceptionHelper.TERMINATED) {
                        RxJavaPlugins.onError(ex);
                    }
                }
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }

        void drain() {
            if (getAndIncrement() == 0) {
                drainLoop();
            }
        }

        void drainLoop() {
            final Observer<? super U> child = this.actual;
            int missed = 1;
            for (;;) {
                if (checkTerminate()) {
                    return;
                }
                SimplePlainQueue<U> svq = queue;

                if (svq != null) {
                    for (;;) {
                        U o;
                        for (;;) {
                            if (checkTerminate()) {
                                return;
                            }

                            o = svq.poll();

                            if (o == null) {
                                break;
                            }

                            child.onNext(o);
                        }
                        if (o == null) {
                            break;
                        }
                    }
                }

                boolean d = done;
                svq = queue;
                InnerObserver<?, ?>[] inner = observers.get();
                int n = inner.length;

                if (d && (svq == null || svq.isEmpty()) && n == 0) {
                    Throwable ex = errors.get();
                    if (ex == null) {
                        child.onComplete();
                    } else {
                        child.onError(errors.terminate());
                    }
                    return;
                }

                boolean innerCompleted = false;
                if (n != 0) {
                    long startId = lastId;
                    int index = lastIndex;

                    if (n <= index || inner[index].id != startId) {
                        if (n <= index) {
                            index = 0;
                        }
                        int j = index;
                        for (int i = 0; i < n; i++) {
                            if (inner[j].id == startId) {
                                break;
                            }
                            j++;
                            if (j == n) {
                                j = 0;
                            }
                        }
                        index = j;
                        lastIndex = j;
                        lastId = inner[j].id;
                    }

                    int j = index;
                    sourceLoop:
                    for (int i = 0; i < n; i++) {
                        if (checkTerminate()) {
                            return;
                        }
                        @SuppressWarnings("unchecked")
                        InnerObserver<T, U> is = (InnerObserver<T, U>)inner[j];

                        U o = null;
                        for (;;) {
                            if (checkTerminate()) {
                                return;
                            }
                            SimpleQueue<U> q = is.queue;
                            if (q == null) {
                                break;
                            }
                            for (;;) {
                                try {
                                    o = q.poll();
                                } catch (Throwable ex) {
                                    Exceptions.throwIfFatal(ex);
                                    is.dispose();
                                    errors.addThrowable(ex);
                                    if (checkTerminate()) {
                                        return;
                                    }
                                    removeInner(is);
                                    innerCompleted = true;
                                    i++;
                                    continue sourceLoop;
                                }
                                if (o == null) {
                                    break;
                                }

                                child.onNext(o);

                                if (checkTerminate()) {
                                    return;
                                }
                            }
                            if (o == null) {
                                break;
                            }
                        }
                        boolean innerDone = is.done;
                        SimpleQueue<U> innerQueue = is.queue;
                        if (innerDone && (innerQueue == null || innerQueue.isEmpty())) {
                            removeInner(is);
                            if (checkTerminate()) {
                                return;
                            }
                            innerCompleted = true;
                        }

                        j++;
                        if (j == n) {
                            j = 0;
                        }
                    }
                    lastIndex = j;
                    lastId = inner[j].id;
                }

                if (innerCompleted) {
                    if (maxConcurrency != Integer.MAX_VALUE) {
                        ObservableSource<? extends U> p;
                        synchronized (this) {
                            p = sources.poll();
                            if (p == null) {
                                wip--;
                                continue;
                            }
                        }
                        subscribeInner(p);
                    }
                    continue;
                }
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }

        boolean checkTerminate() {
            if (cancelled) {
                return true;
            }
            Throwable e = errors.get();
            if (!delayErrors && (e != null)) {
                disposeAll();
                actual.onError(errors.terminate());
                return true;
            }
            return false;
        }

        boolean disposeAll() {
            s.dispose();
            InnerObserver<?, ?>[] a = observers.get();
            if (a != CANCELLED) {
                a = observers.getAndSet(CANCELLED);
                if (a != CANCELLED) {
                    for (InnerObserver<?, ?> inner : a) {
                        inner.dispose();
                    }
                    return true;
                }
            }
            return false;
        }
    }
//    它是一个Observer对象，它有个parent指向MergeObserver
    //ergeObserver 继承了 AtomicInteger，所以这里的tryEmit方法就利用了 AtomicInteger 的同步机制，所以同时只会有一个 value 被 actual Observer 发射
    static final class InnerObserver<T, U> extends AtomicReference<Disposable>
    implements Observer<U> {

        private static final long serialVersionUID = -4606175640614850599L;
        final long id;
        final MergeObserver<T, U> parent;

        volatile boolean done;
        volatile SimpleQueue<U> queue;

        int fusionMode;
//    每一个InnerObserver都有一个parent指向MergeObserver，留意这一点
        InnerObserver(MergeObserver<T, U> parent, long id) {
            System.out.println("------InnerObserver调用构造函数----------");
            this.id = id;
            this.parent = parent;
        }
        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.setOnce(this, s)) {
                if (s instanceof QueueDisposable) {
                    @SuppressWarnings("unchecked")
                    QueueDisposable<U> qd = (QueueDisposable<U>) s;

                    int m = qd.requestFusion(QueueDisposable.ANY);
                    if (m == QueueDisposable.SYNC) {
                        fusionMode = m;
                        queue = qd;
                        done = true;
                        parent.drain();
                        return;
                    }
                    if (m == QueueDisposable.ASYNC) {
                        fusionMode = m;
                        queue = qd;
                    }
                }
            }
        }
        @Override
        public void onNext(U t) {
            Log.e("TAG", " ObservableTakeUntil PollingActivity ObservableFlatMap InnerObserver onNext:"+t);
        //  funsionMode 默认是 None，走第一个if 逻辑，最终调用的是 上面的MergeObserable 的 tryEmit 方法
            if (fusionMode == QueueDisposable.NONE) {
        //    parent:  MergeObserver<T, U>
                //t是转化后的事件（转化后是集合的话，这个是集合内的单个事件）
                parent.tryEmit(t, this);
            } else {
                parent.drain();
            }
        }
        @Override
        public void onError(Throwable t) {
            if (parent.errors.addThrowable(t)) {
                if (!parent.delayErrors) {
                    parent.disposeAll();
                }
                done = true;
                parent.drain();
            } else {
                RxJavaPlugins.onError(t);
            }
        }
        @Override
        public void onComplete() {
            done = true;
            parent.drain();
        }

        public void dispose() {
            DisposableHelper.dispose(this);
        }
    }
}
