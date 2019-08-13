package com.darren.architect_day31;

import android.util.Log;

/**
 * Created by hcDarren on 2017/12/9.
 */

final class ObservableSchedulers<T> extends Observable<T> {
    final Observable<T> source;
    final Schedulers schedulers;

    public ObservableSchedulers(Observable<T> source, Schedulers schedulers) {
        this.source = source;
        this.schedulers = schedulers;
    }

    @Override
    protected void subscribeActual(Observer<T> observer) {
        //subscribeOn把订阅放在runable（SchedulerTask）中，即 source.subscribe(observer)放在了一个其他线程来订阅
        // 可以设置为主线程或者io线程等
        //rx的订阅从最后一层执行，往上回溯，最下面的在最外面，上面的在最里面
        schedulers.scheduleDirect(new SchedulerTask(observer));
    }

    private class SchedulerTask implements Runnable{
        final Observer<T> observer;
        public SchedulerTask(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public void run() {
            // 线程池最终回来执行 Runnable -> 这行代码，会执行上游的 subscribe()
            // 而这个run方法在子线程中
            //订阅发生在schedulers.scheduleDirect的方法中，
            // MainSchedulers的scheduleDirect运行在主线程中
            // IOSchedulers的scheduleDirect运行在iO线程中
            Log.e("TAG", "ObservableSchedulers subscribeActual:"+Thread.currentThread().getName());
            Log.d("TAG", "ObservableSchedulers subscribeActual observer: "+observer);
            source.subscribe(observer);
        }
    }
}
