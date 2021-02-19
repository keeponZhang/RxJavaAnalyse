/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;

import android.util.Log;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;

import static rx.internal.operators.OnSubscribeRedo.getOnSubscribeRedoTag;

/**
 * Applies a function of your choosing to every item emitted by an {@code Observable}, and emits the results of
 * this transformation as a new {@code Observable}.
 * <p>
 * <img width="640" height="305" src="https://raw.githubusercontent.com/wiki/ReactiveX/RxJava/images/rx-operators/map.png" alt="">
 */
// 这的OperatorMap没有把订阅过程也做了，放在了lift，这里这是有创建了一个新的Subscriber
public final class OperatorMap<T, R> implements Operator<R, T> {

    private final Func1<? super T, ? extends R> transformer;

    public OperatorMap(Func1<? super T, ? extends R> transformer) {
        this.transformer = transformer;
    }
    //map的变换可见只是一个简单的静态代理模式
    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> o) {
        //这里也是代理模式,这个subscriber会发送给上层，订阅后会调用该subscriber的onNext方法，把事件往下传递
        Log.w("TAG", "OperatorMap call 1.12 调用call方法 生成新的 Subscriber:");
        return new Subscriber<T>(o) {

            @Override
            public void onCompleted() {
                o.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onNext(T t) {
                try {
                    //接收到事件时，会用func1函数进行下转换
                    //把上层的T转换成R在发送给下层订阅者
                    Log.i("TAG", getOnSubscribeRedoTag()+"OperatorMap onNext 调用 transformer" +
                            ".call方法:");
                    R call = transformer.call(t);
                    Log.e("TAG", "OperatorMap onNext 转换后call:"+call );
                    o.onNext(call);
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    onError(OnErrorThrowable.addValueAsLastCause(e, t));
                }
            }

        };
    }

}

