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

package io.reactivex.internal.disposables;

import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.disposables.Disposable;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Utility methods for working with Disposables atomically.
 */
public enum DisposableHelper implements Disposable {
    /**
     * The singleton instance representing a terminal, disposed state, don't leak it.
     */
    DISPOSED
    ;

    public static boolean isDisposed(Disposable d) {
        return d == DISPOSED;
    }

    public static boolean set(AtomicReference<Disposable> field, Disposable d) {
        for (;;) {
            Disposable current = field.get();
            if (current == DISPOSED) {
                if (d != null) {
                    d.dispose();
                }
                return false;
            }
            if (field.compareAndSet(current, d)) {
                if (current != null) {
                    current.dispose();
                }
                return true;
            }
        }
    }

    /**
     * Atomically sets the field to the given non-null Disposable and returns true
     * or returns false if the field is non-null.
     * If the target field contains the common DISPOSED instance, the supplied disposable
     * is disposed. If the field contains other non-null Disposable, an IllegalStateException
     * is signalled to the RxJavaPlugins.onError hook.
     * 
     * @param field the target field
     * @param d the disposable to set, not null
     * @return true if the operation succeeded, false
     */
    public static boolean setOnce(AtomicReference<Disposable> field, Disposable d) {
        ObjectHelper.requireNonNull(d, "d is null");
        if (!field.compareAndSet(null, d)) {
            d.dispose();
            if (field.get() != DISPOSED) {
                reportDisposableSet();
            }
            return false;
        }
        return true;
    }

    /**
     * Atomically replaces the Disposable in the field with the given new Disposable
     * but does not dispose the old one.
     * @param field the target field to change
     * @param d the new disposable, null allowed
     * @return true if the operation succeeded, false if the target field contained
     * the common DISPOSED instance and the given disposable (if not null) is disposed.
     */
    public static boolean replace(AtomicReference<Disposable> field, Disposable d) {
        for (;;) {
            Disposable current = field.get();
            if (current == DISPOSED) {
                if (d != null) {
                    d.dispose();
                }
                return false;
            }
            if (field.compareAndSet(current, d)) {
                return true;
            }
        }
    }

    /**
     * Atomically disposes the Disposable in the field if not already disposed.
     * @param field the target field
     * @return true if the curren thread managed to dispose the Disposable
     */
    public static boolean dispose(AtomicReference<Disposable> field) {
        //1、获取当前传递进来的对象所持有的disposable对象
        Disposable current = field.get();
        //2、终止标识
        Disposable d = DISPOSED;
        //3、当前current不等于终止标识（dispose()方法还未被调用）
        if (current != d) {
            //4、使用AtomicReference的原子方法将终止标识设置到field对象中，并返回field对象中的旧值
                    //并将其保存在current中
            current = field.getAndSet(d);
            //5、current仍旧不等于终止标识
            //备注：当第一次调用dispose()方法时，此时current为空，满足这个条件，下面的current != null为false，直接返回true；
            //另外一种情况就是程序多次调用了dispose()方法，但是disposable值不等于终止标识，说明之前的设置失败了，
            //此时current不为空，再次调用dispose()方法
            if (current != d) {
                if (current != null) {
                    current.dispose();
                }
                //7、返回true，表示当前线程成功的设置了终止标识
                return true;
            }
        }
        ///8、之前已经调用过dispose()方法，并且已经正确设置了disposable终止标识，
        //订阅事件已经被终止了，再次调用该方法时直接返回false，表示设置终止标识失败
        return false;
    }

    /**
     * Verifies that current is null, next is not null, otherwise signals errors
     * to the RxJavaPlugins and returns false.
     * @param current the current Disposable, expected to be null
     * @param next the next Disposable, expected to be non-null
     * @return true if the validation succeeded
     */
    public static boolean validate(Disposable current, Disposable next) {
        if (next == null) {
            RxJavaPlugins.onError(new NullPointerException("next is null"));
            return false;
        }
        if (current != null) {
            next.dispose();
            reportDisposableSet();
            return false;
        }
        return true;
    }

    /**
     * Reports that the disposable is already set to the RxJavaPlugins error handler.
     */
    public static void reportDisposableSet() {
        RxJavaPlugins.onError(new IllegalStateException("Disposable already set!"));
    }

    @Override
    public void dispose() {
        // deliberately no-op
    }

    @Override
    public boolean isDisposed() {
        return true;
    }
}
