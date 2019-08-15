package com.darren.architect_day31;

import android.util.Log;

/**
 * Created by hcDarren on 2017/12/2.
 */

public class ObservableJust<T> extends Observable<T> {
    private T item;
    public ObservableJust(T item) {
        this.item = item;
    }

    @Override
    protected void subscribeActual(Observer<T> observer) {
        // 代理对象，why? 方便代码扩展，
        // 2.第二步
        Log.e("TAG", "ObservableJust subscribeActual:"+Thread.currentThread().getName());
        Log.d("TAG", "ObservableJust subscribeActual observer: "+observer);
        //一般来说，订阅到了最上层就不用往上订阅了，此时会触发发送事件
        //这时发送事件所处的线程就是最近一个Observable订阅时所处的线程。
        ScalarDisposable sd = new ScalarDisposable(observer,item);
        observer.onSubscribe();
        sd.run();
    }

    private class ScalarDisposable<T>{
        private Observer observer;
        private T item;

        public ScalarDisposable(Observer<T> observer, T item) {
            this.observer = observer;
            this.item = item;
        }

        public void run(){
            try {
                // 3.第三步 observer -> MapObserver.onNext(String)
                Log.d("TAG"," ObservableJust PollingActivity onNext=="+item+"  " +Thread.currentThread().getName());
                observer.onNext(item);
                observer.onComplete();
            }catch (Exception e){
                observer.onError(e);
            }
        }
    }
}
