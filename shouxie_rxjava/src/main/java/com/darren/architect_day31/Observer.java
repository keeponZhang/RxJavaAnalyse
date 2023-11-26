package com.darren.architect_day31;
import androidx.annotation.NonNull;

/**
 * Created by hcDarren on 2017/12/2.
 * 观察者
 */
public interface Observer<T> {
    void onSubscribe();
    void onNext(@NonNull T item);
    void onError(@NonNull Throwable e);
    void onComplete();
}
