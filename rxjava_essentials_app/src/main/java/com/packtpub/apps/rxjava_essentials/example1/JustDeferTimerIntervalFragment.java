package com.packtpub.apps.rxjava_essentials.example1;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.packtpub.apps.rxjava_essentials.R;
import com.packtpub.apps.rxjava_essentials.apps.AppInfo;
import com.packtpub.apps.rxjava_essentials.apps.ApplicationsList;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func0;
import rx.internal.operators.OnSubscribeRedo;

/**
 * createBy keepon
 */
public class JustDeferTimerIntervalFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_just_defer, container, false);
    }

    private Subscription mTimeSubscription;

    private List<AppInfo> mApps;
    private AppInfo mAppOne;
    private AppInfo mAppTwo;
    private AppInfo mAppThree;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        mApps = ApplicationsList.getInstance().getList();

        mAppOne = mApps.get(0);

        mAppTwo = mApps.get(1);

        mAppThree = mApps.get(2);
        view.findViewById(R.id.just).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                just(mAppOne, mAppTwo, mAppThree);
            }
        });
        view.findViewById(R.id.repeat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testRepeat();
                // testRepeat2();
            }
        });
        view.findViewById(R.id.defer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testDefer();
            }
        });
        view.findViewById(R.id.range).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                range();
            }
        });
        view.findViewById(R.id.interval).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                interval();
            }
        });
        view.findViewById(R.id.timer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timer();
            }
        });
    }

    private void just(AppInfo appOne, AppInfo appTwo, AppInfo appThree) {
        //just其实也是用from，订阅后遍历迭代器按顺序发送
        Observable<AppInfo> threeOfThem = Observable.just(appOne, appTwo, appThree);

        threeOfThem.subscribe(new Observer<AppInfo>() {
            @Override
            public void onCompleted() {
                Toast.makeText(getActivity(), "Here is the list!", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(getActivity(), "Something went wrong!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNext(AppInfo appInfo) {

            }
        });
    }

    private void testRepeat() {
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                Log.e("TAG", OnSubscribeRedo.getOnSubscribeRedoTag() +
                        "JustDeferTimerIntervalFragment call  " +
                        "真正发送数据咯---------------------" +
                        " " +
                        "Keepon:");
                subscriber.onNext("Keepon");
                //要发射onCompleted才能出发重新订阅
                subscriber.onCompleted();
                //onError并不会触发订阅
                // subscriber.onError(new RuntimeException("keepon"));
            }
        }).repeat(2).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "JustDeferTimerIntervalFragment  testRepeat onNext:" + s);
            }
        });
    }

    //repeat原理比较复杂
    private void testRepeat2() {
        AppInfo appInfo = mApps.get(0);
        //		Observable.create(new Observable.OnSubscribe<AppInfo>() {
        //			@Override
        //			public void call(Subscriber<? super AppInfo> subscriber) {
        //				subscriber.onNext(appInfo);
        //			}
        //		})
        Observable.just(appInfo)
                .repeat(2)
                .subscribe(new Observer<AppInfo>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getActivity(), "Something went south!", Toast.LENGTH_SHORT)
                                .show();
                    }

                    @Override
                    public void onNext(AppInfo appInfo) {
                        Log.e("TAG", "JustDeferTimerIntervalFragment  testRepeat2 onNext:" +
                                appInfo.getName());
                    }
                });
    }

    //当产生订阅时，会调用deferred中observableFactory.call()方法，得到真正的Observable,然后订阅真正的Subscriber
    private void testDefer() {
        deferred.subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                Log.e("TAG", "JustDeferTimerIntervalFragment onViewClicked number:" + integer);
            }
        });
    }

    //range实现原理也很简单，从start开始，发送count个，每次加1
    public void range() {
        Observable.range(10, 3)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onCompleted() {
                        Log.e("TAG", "JustDeferTimerIntervalFragment onCompleted:");

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("TAG", "JustDeferTimerIntervalFragment onError:");
                    }

                    @Override
                    public void onNext(Integer number) {
                        Log.e("TAG", "JustDeferTimerIntervalFragment onNext:" + number);

                    }
                });
    }

    //interval就是timer 延迟时间和周期时间一样，最终调用的是同一个方法
    private void interval() {
        Subscription stopMePlease = Observable.interval(3, TimeUnit.SECONDS)
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onCompleted() {
                        Toast.makeText(getActivity(), "Yeaaah!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getActivity(), "Something went wrong!"
                                , Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(Long number) {
                        //这里不是主线程，RxComputationThreadPool-3
                        Log.e("TAG", "JustDeferTimerIntervalFragment onNext:" +
                                Thread.currentThread().getName() + " number==" + number);
//						Toast.makeText(getActivity(), "I say " + number, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    Observable<Integer> deferred = Observable.defer(getInt());

    //使用线程池延迟，initialDelay是第一次延迟时间，period是之后的延迟时间
    private void timer() {
        //OnSubscribeTimerPeriodically 两个参数分别是第一次延迟的时间，发送的间隔时间
        mTimeSubscription = Observable.timer(3, 3, TimeUnit.SECONDS)
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Long number) {
                        Log.e("TAG", "I say " + number + " " + Thread.currentThread().getName());
                    }
                });
    }

    //这里延迟3秒发送，只执行一次
    private void timer2() {
//		OnSubscribeTimerOnce
        mTimeSubscription = Observable.timer(3, TimeUnit.SECONDS)
                //默认发送一个long型
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Long number) {
                        Log.e("RXJAVA", "I say " + number);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mTimeSubscription != null && !mTimeSubscription.isUnsubscribed()) {
            mTimeSubscription.unsubscribe();
        }
    }

    private Func0 getInt() {
        Observable<Integer> observable = Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                if (subscriber.isUnsubscribed()) {
                    return;
                }
                Log.e("TAG", "JustDeferTimerIntervalFragment getInt:");
                subscriber.onNext(42);
                subscriber.onCompleted();
            }
        });
        return new Func0<Observable<Integer>>() {
            @Override
            //注意此处的call方法没有Subscriber参数
            public Observable<Integer> call() {
                return observable;
            }
        };
    }
}
