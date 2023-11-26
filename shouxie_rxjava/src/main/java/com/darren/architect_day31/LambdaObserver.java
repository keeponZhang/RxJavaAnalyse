package com.darren.architect_day31;

import androidx.annotation.NonNull;

import androidx.annotation.NonNull;

/**
 * Created by hcDarren on 2017/12/3.
 */

public class LambdaObserver<T> implements Observer<T>{
    private Consumer<T> onNext;
    public LambdaObserver(Consumer<T> onNext) {
        this.onNext = onNext;
    }

    @Override
    public void onSubscribe() {

    }

    @Override
    public void onNext(@NonNull T item) {
        try {
            onNext.onNext(item);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(@NonNull Throwable e) {

    }

    @Override
    public void onComplete() {

    }
}
