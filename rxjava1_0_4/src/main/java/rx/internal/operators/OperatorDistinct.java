/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Returns an Observable that emits all distinct items emitted by the source.
 * 
 * @param <T> the value type
 * @param <U> the key type
 */
public final class OperatorDistinct<T, U> implements Operator<T, T> {
    final Func1<? super T, ? extends U> keySelector;

    public OperatorDistinct(Func1<? super T, ? extends U> keySelector) {
        this.keySelector = keySelector;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {
            //distinct实现很简单，用HashSet去重
            Set<U> keyMemory = new HashSet<U>();

            @Override
            public void onNext(T t) {
                Log.i("TAG", "OperatorDistinct onNext 变换前:"+t);
                //有传fun，增加一个装换功能
                U key = keySelector.call(t);
                Log.i("TAG", "OperatorDistinct onNext key变换后:"+key);
                boolean add = keyMemory.add(key);
               System.out.println("OperatorDistinct onNext key:"+key+"  add="+add);
                if (add) {
                    child.onNext(t);
                } else {
                    request(1);
                }
            }

            @Override
            public void onError(Throwable e) {
                keyMemory = null;
                child.onError(e);
            }

            @Override
            public void onCompleted() {
                keyMemory = null;
                child.onCompleted();
            }
            
        };
    }
}
