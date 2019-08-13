package com.darren.architect_day31;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by hcDarren on 2017/12/9.
 */

public abstract class Schedulers {
    static Schedulers MAIN_THREAD;
    static Schedulers IO;
    static Schedulers CPU;
    static {
        IO = new IOSchedulers();
        CPU = new CPUSchedulers();
        MAIN_THREAD = new MainSchedulers(new Handler(Looper.getMainLooper()));
    }

    public static Schedulers io() {
        return IO;
    }
    public static Schedulers cpu() {
        return CPU;
    }

    public abstract void scheduleDirect(Runnable runnable);

    public static Schedulers mainThread() {
        return MAIN_THREAD;
    }

    private static class IOSchedulers extends Schedulers {
        ExecutorService service;
        public IOSchedulers(){
            service = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                @Override
                public Thread newThread(@NonNull Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("IO线程");
                    return thread;
                }
            });
        }

        @Override
        public void scheduleDirect(Runnable runnable) {
            service.execute(runnable);
        }
    }
    private static class CPUSchedulers extends Schedulers {
        ExecutorService service;
        public CPUSchedulers(){
            service = Executors.newScheduledThreadPool(1, new ThreadFactory() {
                @Override
                public Thread newThread(@NonNull Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("CPU线程");
                    return thread;
                }
            });
        }

        @Override
        public void scheduleDirect(Runnable runnable) {
            service.execute(runnable);
        }
    }

    private static class MainSchedulers extends Schedulers {
        private Handler handler;
        public MainSchedulers(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void scheduleDirect(Runnable runnable) {
            Message message = Message.obtain(handler,runnable);
            handler.sendMessage(message);
        }
    }
}
