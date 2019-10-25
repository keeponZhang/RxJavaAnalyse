package com.demo.lizejun.rxsample.chapter5;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.demo.lizejun.DoubleClickCheckUtils;
import com.demo.lizejun.rxsample.R;

import java.util.concurrent.TimeUnit;

import io.reactivex.Notification;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.observers.ToNotificationObserver;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class PollingActivity extends AppCompatActivity {

    private static final String TAG = PollingActivity.class.getSimpleName();

    private TextView mTvSimple;
    private TextView mTvAdvance;
    private CompositeDisposable mCompositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polling);
        mTvSimple = (TextView) findViewById(R.id.tv_simple);
        mTvSimple.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startSimplePolling();
            }

        });
        mTvAdvance = (TextView) findViewById(R.id.tv_advance);
        mTvAdvance.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startAdvancePolling();
            }

        });
        findViewById(R.id.tv_advance2).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startAdvancePolling2();
            }

        });
        findViewById(R.id.tv_advance3).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!DoubleClickCheckUtils.vertifyDuration()){
                    return;
                }
                startAdvancePolling3();
            }

        });
        findViewById(R.id.tv_advance4).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!DoubleClickCheckUtils.vertifyDuration()){
                    return;
                }
                startAdvancePolling4();
            }

        });
        findViewById(R.id.tv_advance5).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!DoubleClickCheckUtils.vertifyDuration()){
                    return;
                }
                test();
            }

        });
        findViewById(R.id.tv_advance6).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!DoubleClickCheckUtils.vertifyDuration()){
                    return;
                }
                subsribeAgain();
            }

        });
        mCompositeDisposable = new CompositeDisposable();
    }

    private void subsribeAgain() {

    }

    Observer   mObserver2 = new Observer<Long>() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onNext(Long value) {
            Log.e("TAG", "PollingActivity  mObserver2 onNext:" + value);
        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };
    Observer  mObserver1 = new Observer<Long>() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onNext(Long value) {
            Log.e("TAG", "PollingActivity  mObserver1 onNext:" + value);
        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };
    Observable  mLongObservable = Observable.create(new ObservableOnSubscribe<Long>() {
        @Override
        public void subscribe(ObservableEmitter<Long> e) throws Exception {
            long l = System.currentTimeMillis();
            e.onNext(l);
            e.onComplete();
        }
    });
    private void test() {
        ToNotificationObserver<Object> actionObserver = new ToNotificationObserver<Object>(new Consumer<Notification<Object>>() {
            @Override
            public void accept(Notification<Object> o) {
                Log.e("TAG", "PollingActivity test accept:");

            }
        });
        mLongObservable.flatMap(new Function<Long, ObservableSource<?>>() {
            @Override
            public ObservableSource<?> apply(Long aLong) throws Exception {

                return Observable.just(aLong);
            }
        }).subscribe(actionObserver);
//        mLongObservable.subscribe(mObserver2);

    }

    private void startSimplePolling() {
        Log.d(TAG, "startSimplePolling");
        Observable<Long> observable = Observable.intervalRange(0, 5, 0, 3000, TimeUnit.MILLISECONDS)
                .take(5)
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        doWork(); //这里使用了doOnNext，因此DisposableObserver的onNext要等到该方法执行完才会回调。
                    }

                });
        DisposableObserver<Long> disposableObserver = getDisposableObserver();
        observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }

    private void startAdvancePolling() {
        Log.d(TAG, "startAdvancePolling click");
        Observable<Long> observable = Observable.just(100L).doOnComplete(new Action() {

            @Override
            public void run() throws Exception {
                //doOnComplete：事件发送完成了才会回调
                doWork();
            }

        }).repeatWhen(new Function<Observable<Object>, ObservableSource<Long>>() {

            private long mRepeatCount;

            @Override
            public ObservableSource<Long> apply(Observable<Object> objectObservable) throws Exception {
                //最先调用，repeatWhen生成的Observable会最放到最前面去
                Log.e("TAG", "PollingActivity apply 1.3 我是repeatWhen handler 的 apply方法  参数objectObservable:" + objectObservable + "--------------");

                return objectObservable.flatMap(new Function<Object, ObservableSource<Long>>() {
                    @Override
                    public ObservableSource<Long> apply(Object o) throws Exception {
                        return Observable.just(1L);
                    }
                });


//                if (++mRepeatCount >=2) {
//                    //return Observable.empty(); //发送onComplete消息，无法触发下游的onComplete回调。
//                    Log.e("TAG", "PollingActivity apply  Observable.error:");
//                    return Observable.error(new Throwable("Polling work finished")); //发送onError消息，可以触发下游的onError回调。
//                }else{
//                    //这里创建一个重新发射发射的话，
//                    return Observable.create(new ObservableOnSubscribe<Long>() {
//                        @Override
//                        public void subscribe(ObservableEmitter<Long> e) throws Exception {
//                            //发送一次就 会触发一次订阅；Observable.just(0L)也会重新发送一遍
//                            e.onNext(120L);
////                            e.onNext(130L);
//                        }
//                    });
//                }


                //必须作出反应，这里是通过flatMap操作符。
//                return objectObservable.flatMap(new Function<Object, ObservableSource<Long>>() {
//
//                    @Override
//                    public ObservableSource<Long> apply(Object o) throws Exception {
//
//                        if (++mRepeatCount > 3) {
//                            //return Observable.empty(); //发送onComplete消息，无法触发下游的onComplete回调。
//                            return Observable.error(new Throwable("Polling work finished")); //发送onError消息，可以触发下游的onError回调。
//                        }
//                        Log.d(TAG, "PollingActivity startAdvancePolling apply >>>>>>>>>>"+mRepeatCount);
//                        //这里发送的1000L不会真正发送给下游的观察者，是组成Notification用的
//                        return Observable.just(1000L);
////                        return Observable.timer(1000 , TimeUnit.MILLISECONDS);
//                    }
//
//                });
            }

        });
        DisposableObserver<Long> disposableObserver = getDisposableObserver();
        observable.subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }

    private void startAdvancePolling2() {
        Log.d(TAG, "startAdvancePolling click2");
        Observable<Long> observable = Observable.just(200L).doOnComplete(new Action() {

            @Override
            public void run() throws Exception {
                //doOnComplete：事件发送完成了才会回调
                doWork();
            }

        }).repeatWhen(new Function<Observable<Object>, ObservableSource<Long>>() {
            private long mRepeatCount;

            @Override
            public ObservableSource<Long> apply(Observable<Object> objectObservable) throws Exception {
                //最先调用，repeatWhen生成的Observable会最放到最前面去
                Log.e("TAG", "PollingActivity apply Object:" + objectObservable + "--------------");

//                return objectObservable.flatMap(new Function<Object, ObservableSource<Long>>() {
//                    @Override
//                    public ObservableSource<Long> apply(Object o) throws Exception {
//                        return Observable.just(1000L);
//                    }
//                });


                if (++mRepeatCount >= 2) {
                    //return Observable.empty(); //发送onComplete消息，无法触发下游的onComplete回调。
                    Log.e("TAG", "PollingActivity apply  Observable.error:");
                    return Observable.error(new Throwable("Polling work finished")); //发送onError消息，可以触发下游的onError回调。
                } else {
                    //这里创建一个重新发射发射的话，
                    return Observable.create(new ObservableOnSubscribe<Long>() {
                        @Override
                        public void subscribe(ObservableEmitter<Long> e) throws Exception {
                            //发送一次就 会触发一次订阅；Observable.just(0L)也会重新发送一遍
                            Log.e("TAG", "PollingActivity subscribe  e.onNext(120L):");
                            e.onNext(2L);
//                            e.onNext(130L);
                        }
                    });
                }


                //必须作出反应，这里是通过flatMap操作符。
//                return objectObservable.flatMap(new Function<Object, ObservableSource<Long>>() {
//
//                    @Override
//                    public ObservableSource<Long> apply(Object o) throws Exception {
//
//                        if (++mRepeatCount > 3) {
//                            //return Observable.empty(); //发送onComplete消息，无法触发下游的onComplete回调。
//                            return Observable.error(new Throwable("Polling work finished")); //发送onError消息，可以触发下游的onError回调。
//                        }
//                        Log.d(TAG, "PollingActivity startAdvancePolling apply >>>>>>>>>>"+mRepeatCount);
//                        //这里发送的1000L不会真正发送给下游的观察者，是组成Notification用的
//                        return Observable.just(1000L);
////                        return Observable.timer(1000 , TimeUnit.MILLISECONDS);
//                    }
//
//                });
            }

        });
        DisposableObserver<Long> disposableObserver = getDisposableObserver();
        observable.subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }

    //这里repeatWhen生成的Observable会主动发射
    private void startAdvancePolling3() {
        Log.d(TAG, "startAdvancePolling click3");
        Observable<Long> observable = Observable.create(new ObservableOnSubscribe<Long>() {
            @Override
            public void subscribe(ObservableEmitter<Long> e) throws Exception {
                long l = System.currentTimeMillis();
                Log.e("TAG", "-----------PollingActivity subscribe 发送数据啦  e.onNext="+l+" ---------------");
                e.onNext(l);
                //没有onComplete，只会发送一次
                e.onComplete();
            }
        }).repeatWhen(new Function<Observable<Object>, ObservableSource<Long>>() {


            @Override
            public ObservableSource<Long> apply(Observable<Object> objectObservable) throws Exception {
                //最先调用，repeatWhen生成的Observable会最放到最前面去 ,objectObservable其实是一个Subject
                Log.e("TAG", "PollingActivity apply 1.3 我是repeatWhen handler 的 apply方法  参数objectObservable:" + objectObservable + "--------------");
                return Observable.create(new ObservableOnSubscribe<Long>() {
                    @Override
                    public void subscribe(ObservableEmitter<Long> e) throws Exception {
                        //发送一次就 会触发一次订阅；Observable.just(0L)也会重新发送一遍
                        Log.e("TAG", "PollingActivity subscribe 1.4  中间层主动发射 e.onNext(120L):");
//                        e.onNext(3L);
//                        e.onComplete();
                    }
                });

            }

        });
        DisposableObserver<Long> disposableObserver = getDisposableObserver();
        observable.subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }

    ObservableOnSubscribe mObservableOnSubscribe4 = new ObservableOnSubscribe<Long>() {
        @Override
        public void subscribe(ObservableEmitter<Long> e) throws Exception {
            long l = System.currentTimeMillis();
            Log.e("TAG", "PollingActivity subscribe ObservableOnSubscribe 发送数据:"+l);
            e.onNext(l);
            //没有onComplete，只会发送一次
            e.onComplete();
        }
    };
    private void startAdvancePolling4() {

        Log.d(TAG, "startAdvancePolling click4");
        Observable observable1 = Observable.create(mObservableOnSubscribe4);
        Observable<Long> observable2 =observable1 .repeatWhen(new Function<Observable<Object>, ObservableSource<Long>>() {
            private long mRepeatCount;

            @Override
            public ObservableSource<Long> apply(Observable<Object> objectObservable) throws Exception {
                //最先调用，repeatWhen生成的Observable会最放到最前面去
                Log.e("TAG", "PollingActivity apply Object:" + objectObservable + "--------------");
                return objectObservable.flatMap(new Function<Object, ObservableSource<Long>>() {
                    @Override
                    public ObservableSource<Long> apply(Object o) throws Exception {
                        Log.d("TAG", "PollingActivity apply  flatMap Object  ------>> :"+o);
                        mRepeatCount++;
                        if(mRepeatCount>3){
                            return Observable.error(new Throwable("Polling work finished"));
                        }
                        return Observable.just(4L);
                    }
                });

            }

        });
        //其实repeat后面生成的Oberverable 与observable1无关联，通过处理disposableObserver会订阅observable1，此时disposableObserver被两个被观察者订阅
        //因为repeat后面生成的Oberverable类似于subject,所以两次订阅只observable1会发送数据，然后调用repeat后面生成的Oberverable发送数据（subject.onNext),此时有会诱发observable1再次订阅disposableObserver，达成重复的结果
        DisposableObserver<Long> disposableObserver = getDisposableObserver();
        Log.e("TAG", "PollingActivity startAdvancePolling4 disposableObserver:"+disposableObserver);
        observable2.subscribe(disposableObserver);
        mCompositeDisposable.add(disposableObserver);
    }

    private DisposableObserver<Long> getDisposableObserver() {

        return new DisposableObserver<Long>() {

            @Override
            public void onNext(Long aLong) {
                Log.e("TAG", "PollingActivity onNext 收到数据啦:" + aLong);
            }

            @Override
            public void onError(Throwable throwable) {
                Log.d(TAG, "DisposableObserver onError, threadId=" + Thread.currentThread().getId() + ",reason=" + throwable.getMessage());
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "DisposableObserver onComplete, threadId=" + Thread.currentThread().getId());
            }
        };
    }

    private void doWork() {
        long workTime = (long) (Math.random() * 500) + 500;
        try {
            Log.d(TAG, "doWork start,  threadId=" + Thread.currentThread().getId());
//            Thread.sleep(workTime);
            Log.d(TAG, "doWork finished");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.clear();
    }
}
