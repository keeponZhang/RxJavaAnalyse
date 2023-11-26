package com.packtpub.apps.rxjava_essentials.example1;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.packtpub.apps.rxjava_essentials.R;

import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subjects.AsyncSubject;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;

/**
 * createBy keepon
 */
public class SubjectFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subject, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        view.findViewById(R.id.publishSubject).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doComplete();
                PublishSubject<String> stringPublishSubject = PublishSubject.create();
                Subscription subscriptionPrint = stringPublishSubject.subscribe(
                        new Observer<String>() {
                            @Override
                            public void onCompleted() {
                                Log.e("TAG", "SubjectFragment onCompleted:");
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e("TAG", "SubjectFragment onError:");
                            }

                            @Override
                            public void onNext(String message) {
                                Log.e("TAG", "SubjectFragment onNext:" + message);
                            }
                        });
                stringPublishSubject.onNext("Hello World");
            }
        });
        view.findViewById(R.id.behaviorSubject).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // BehaviorSubject会首先向他的订阅者发送截至订阅前最新的一个数据对
                // 象（或初始值）,然后正常发送订阅后的数据流
                BehaviorSubject<Integer> behaviorSubject = BehaviorSubject.create(1);
                behaviorSubject.subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        Log.e("TAG", "SubjectFragment behaviorSubject onNext:" + integer);
                    }
                });
                behaviorSubject.onNext(100);
            }
        });
        view.findViewById(R.id.asyncSubject).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AsyncSubject<Integer> asyncSubject = AsyncSubject.create();
                asyncSubject.subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        Log.e("TAG", "SubjectFragment AsyncSubject onNext:" + integer);
                    }
                });
                asyncSubject.onNext(100);
                asyncSubject.onNext(200);
                asyncSubject.subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        Log.e("TAG", "SubjectFragment AsyncSubject2 onNext:" + integer);
                    }
                });
                asyncSubject.onNext(300);
                //不调用onCompleted是无用的
                asyncSubject.onCompleted();
            }
        });
        view.findViewById(R.id.replaySubject).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReplaySubject<Integer> replaySubject = ReplaySubject.create();
                replaySubject.subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        Log.e("TAG", "SubjectFragment ReplaySubject onNext:" + integer);
                    }
                });
                replaySubject.onNext(100);
                replaySubject.onNext(200);

                replaySubject.subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        Log.e("TAG", "SubjectFragment ReplaySubject2 onNext:" + integer);
                    }
                });
                replaySubject.onNext(300);
            }
        });

    }

    private void testPublish() {

    }

    private void doComplete() {
        // lift的本质先生成一个onSubscribe和Observable，下面往上订阅的时候调用Operator的call方法生成subscriber
        // lift(new OperatorDoOnEach<T>(observer))
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                for (int i = 0; i < 5; i++) {
                    subscriber.onNext(i);
                }
                subscriber.onCompleted();
            }
        }).doOnCompleted(new Action0() {
            @Override
            public void call() {
                // subject.onNext(true);
            }
        }).subscribe();
    }
}
