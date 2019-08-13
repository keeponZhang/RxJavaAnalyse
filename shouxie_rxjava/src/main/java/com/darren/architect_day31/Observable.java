package com.darren.architect_day31;

/**
 * Created by hcDarren on 2017/12/2.
 * 被观察者
 */

public abstract class Observable<T> implements ObservableSource<T>{

    public static <T> Observable<T> just(T item) {
        return onAssembly(new ObservableJust<T>(item));
    }

    private static <T> Observable<T> onAssembly(Observable<T> source) {
        // 留出来了
        return source;
    }

    //subscribe会调用离它最近的Obsrvable的  subscribeActual(observer)方法
    @Override
    public void subscribe(Observer<T> observer) {
        subscribeActual(observer);
    }

    public void subscribe(Consumer<T> onNext){
        subscribe(onNext,null,null);
    }

    private void subscribe(Consumer<T> onNext, Consumer<T> error, Consumer<T> complete) {
        subscribe(new LambdaObserver<T>(onNext));
    }

    protected abstract void subscribeActual(Observer<T> observer);

    public <R> Observable<R> map(Function<T, R> function) {
        return onAssembly(new ObservableMap<>(this,function));
    }

    public Observable<T> subscribeOn(Schedulers schedulers) {
        return onAssembly(new ObservableSchedulers(this,schedulers));
    }

    public Observable<T> observerOn(Schedulers schedulers) {
        return onAssembly(new ObserverOnObservable(this,schedulers));
    }
}
