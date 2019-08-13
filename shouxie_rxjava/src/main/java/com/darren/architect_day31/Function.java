package com.darren.architect_day31;

/**
 * Created by hcDarren on 2017/12/3.
 */

public interface Function<T,R> {
    R apply(T t) throws Exception;
}
