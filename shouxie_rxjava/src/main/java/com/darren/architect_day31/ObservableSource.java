package com.darren.architect_day31;

/**
 * Created by hcDarren on 2017/12/2.
 */

public interface ObservableSource<T> {
    void subscribe(Observer<T> observer);
}
