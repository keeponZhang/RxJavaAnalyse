package com.darren.architect_day31;

import android.support.annotation.NonNull;
import android.util.Log;

/**
 * Created by hcDarren on 2017/12/2.
 * 静态代理的包装类
 */

public class ObservableMap<T,R> extends Observable<R> {
    final Observable<T> source;// 前面的 Observable
    final Function<T, R> function;// 当前转换
    public ObservableMap(Observable<T> source, Function<T, R> function) {
        this.source = source;
        this.function = function;
    }

    @Override
    protected void subscribeActual(Observer<R> observer) {
        // 对 observer 包裹了一层，静态代理包裹 source 永远是上游的 Observable 对象
        // observer 代表的是下游给我们的封装好的 observer 对象
        Log.e("TAG", "ObservableMap subscribeActual:"+Thread.currentThread().getName());
        Log.d("TAG", "ObservableMap subscribeActual observer: "+observer);
        source.subscribe(new MapObserver(observer,function));
    }

    //因为上层被观察者发送的数据类型和下层的观察者订阅的数据类型不一致，所以ObservableMap会构建一个观察者来接收上层的类型，
    //转换后在发送给下层的观察者（ObservableMap也是一个被观察者，下层的观察者订阅的是ObservableMap要发送的类型数据）
    //与此相比，ObserverOnObservable也会构建一个观察者，但是ObserverOnObservable构建的观察者类型跟上层发送的数据类型是一样的，ObserverOnObservable的作用是起一个
    //承接作用，接收到数据后一般会做个线程变换再发送个下一个观察者。(observerOn)
    //ObservableSchedulers则不会构建一个观察者，当这个观察者订阅被观察者是，会做一些事件变换（subscribeOn）
    private class MapObserver<T> implements Observer<T>{
        final Observer<R> observer;
        final Function<T, R> function;
        public MapObserver(Observer<R> source, Function<T, R> function) {
            this.observer = source;
            this.function = function;
        }

        @Override
        public void onSubscribe() {
            observer.onSubscribe();
        }

        @Override
        public void onNext(@NonNull T item) {
            // item 是 String  xxxUrl
            // 要去转换 String -> Bitmap
            // 4.第四步 function.apply
            try {
                //map的apply方法就是收到上一层发送的数据后，经过转换，再发送给下一个观察者订阅的数据类型
                R applyR = function.apply(item);
                // 6. 第六步，调用 onNext
                // 把 Bitmap 传出去
                observer.onNext(applyR);
            } catch (Exception e) {
                e.printStackTrace();
                observer.onError(e);
            }
        }

        @Override
        public void onError(@NonNull Throwable e) {
            observer.onError(e);
        }

        @Override
        public void onComplete() {
            observer.onComplete();
        }
    }
}
