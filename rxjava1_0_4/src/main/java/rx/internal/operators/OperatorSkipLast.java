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

import java.util.ArrayDeque;
import java.util.Deque;

import rx.Observable.Operator;
import rx.Subscriber;

/**
 * Bypasses a specified number of elements at the end of an observable sequence.
 */
public class OperatorSkipLast<T> implements Operator<T, T> {

    private final int count;

    public OperatorSkipLast(int count) {
        if (count < 0) {
            throw new IndexOutOfBoundsException("count could not be negative");
        }
        this.count = count;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
        return new Subscriber<T>(subscriber) {

            private final NotificationLite<T> on = NotificationLite.instance();

            /**
             * Store the last count elements until now.
             */
            private final Deque<Object> deque = new ArrayDeque<Object>();

            @Override
            public void onCompleted() {
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onNext(T value) {
                if (count == 0) {
                    // If count == 0, we do not need to put value into deque
                    // and remove it at once. We can emit the value
                    // directly.
                    //第一个是直接发送的
                    subscriber.onNext(value);
                    return;
                }
                //takeLast的实现，takeLast再onCompleted才启动发送时
               /* if (deque.size() == count) {
                    //移除前面的，因为这里是目标是要保存后面的，takeLast(因为要发送的数量是一定的，如果还有数据过来，说明队列里头的数据时可以不要的)
                    Object o = deque.removeFirst();
                    System.out.println("-------------OperatorTakeLast  deque.removeFirst()"+o+"  --------------count="+count);
                }
                //往队列里插入数据,value不为空的话，还是value，空的话返回ON_NEXT_NULL_SENTINEL，因为deque不支持插入空值
                deque.offerLast(notification.next(value));*/

               //这里的逻辑是：缓存已经到了要发送的数据了，还有数据发过来，所以队列头的是要发送的数据
                if (deque.size() == count) {
                    subscriber.onNext(on.getValue(deque.removeFirst()));
                } else {
                    request(1);
                }
                //除了第一个不入队列
                deque.offerLast(on.next(value));
            }

        };
    }

}
