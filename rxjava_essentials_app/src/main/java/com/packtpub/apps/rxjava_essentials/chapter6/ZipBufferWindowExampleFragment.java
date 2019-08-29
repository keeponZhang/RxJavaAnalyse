package com.packtpub.apps.rxjava_essentials.chapter6;


import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.packtpub.apps.rxjava_essentials.R;
import com.packtpub.apps.rxjava_essentials.apps.AppInfo;
import com.packtpub.apps.rxjava_essentials.apps.ApplicationAdapter;
import com.packtpub.apps.rxjava_essentials.apps.ApplicationsList;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;


public class ZipBufferWindowExampleFragment extends Fragment {

    @BindView(R.id.fragment_first_example_list)
    RecyclerView mRecyclerView;

    @BindView(R.id.fragment_first_example_swipe_container)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.zip)
    Button mZip;
    @BindView(R.id.buffer)
    Button mBuffer;
    @BindView(R.id.window)
    Button mWindow;
    Unbinder unbinder;

    private ApplicationAdapter mAdapter;

    private ArrayList<AppInfo> mAddedApps = new ArrayList<>();
    private List<AppInfo> mApps;

    public ZipBufferWindowExampleFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_example_zip, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        mAdapter = new ApplicationAdapter(new ArrayList<>(), R.layout.applications_list_item);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.myPrimaryColor));
        mSwipeRefreshLayout.setProgressViewOffset(false, 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));

        // Progress
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(true);
        mRecyclerView.setVisibility(View.GONE);

        mApps = ApplicationsList.getInstance().getList();

        loadList(mApps.subList(0, 5));
    }

    private void loadList(List<AppInfo> apps) {
        mRecyclerView.setVisibility(View.VISIBLE);

        Observable<AppInfo> observableApp = Observable.from(apps);

        Observable<Integer> tictoc = Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                for (int i = 0; i < 2; i++) {
                    subscriber.onNext(i);
                }
            }
        });

        Observable
                .zip(observableApp, tictoc, new Func2<AppInfo, Integer, AppInfo>() {
                    @Override
                    public AppInfo call(AppInfo appInfo, Integer time) {
                        appInfo.setName(time + " " + appInfo.getName());
                        return appInfo;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AppInfo>() {
                    @Override
                    public void onCompleted() {
                        Toast.makeText(getActivity(), "Here is the list!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        mSwipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getActivity(), "Something went wrong!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNext(AppInfo appInfo) {
                        if (mSwipeRefreshLayout.isRefreshing()) {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                        mAddedApps.add(appInfo);
                        int position = mAddedApps.size() - 1;
                        mAdapter.addApplication(position, appInfo);
                        mRecyclerView.smoothScrollToPosition(position);
                    }
                });
    }

    private AppInfo updateTitle(AppInfo appInfo, Long time) {
        appInfo.setName(time + " " + appInfo.getName());
        return appInfo;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick({R.id.zip, R.id.buffer, R.id.window, R.id.bufferskip})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.zip:
                zip();
                break;
            case R.id.buffer:
                buffer();
                break;
            case R.id.window:
                window();
                break;
            case R.id.bufferskip:
                bufferskip();
                break;
        }
    }
    //window() 函数和 buffer() 很像，但是它发射的是Observable而不是列表
    private void window() {
        Observable<AppInfo> observableApp = Observable.from(mApps.subList(0, 6));

        Subscriber<Observable<AppInfo>> subscriber = new Subscriber<Observable<AppInfo>>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Observable<AppInfo> appInfoObservable) {
                appInfoObservable.subscribe(new Subscriber<AppInfo>() {
                    @Override
                    public void onCompleted() {
                        Log.w("TAG", "ZipBufferWindowExampleFragment window appInfoObservable onCompleted:" + appInfoObservable);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(AppInfo appInfo) {
                        Log.w("TAG", "ZipBufferWindowExampleFragment OperatorWindowWithSize window appInfoObservable onNext:" + appInfo+"  appInfoObservable="+appInfoObservable);
                    }
                });
            }
        };
        observableApp.window(3).subscribe(subscriber);
        Log.e("TAG", "ZipBufferWindowExampleFragment window child subscriber:" + subscriber);
    }

    //将源Observable变换一个新的Observable，这个新的Observable每次发射一组列表值而不是一个一个发射
    //buffer实现很简单，用list集合装起来，到了指定的数量是，list集合发送出去，list清空
    private void buffer() {
        Observable<AppInfo> observableApp = Observable.from(mApps.subList(0, 6));
        observableApp.buffer(3).subscribe(new Subscriber<List<AppInfo>>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(List<AppInfo> appInfos) {
                Log.e("TAG", "ZipBufferWindowExampleFragment buffer onNext:" + appInfos);
            }
        });
    }

    //其中有一个是允许你指定一个 skip  值：此后每 skip  项数据，然后又用count项数据填充缓冲区
    private void bufferskip() {
        Observable<AppInfo> observableApp = Observable.from(mApps.subList(0, 6));
        observableApp.buffer(2, 3).subscribe(new Subscriber<List<AppInfo>>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(List<AppInfo> appInfos) {
                Log.e("TAG", "ZipBufferWindowExampleFragment bufferskip onNext:" + appInfos);
            }
        });
    }

    //zip 一一对应，有一个observable发送onComplete，最下方的观察者就会收到onComplete事件
    //如果两方都没发送onComplete，一方多一方少，队列里有16个值后，会发送onError事件
    private void zip() {
//		Observable<String> just = Observable.just("zhangshan", "lisi", "wnagwu");
        Observable<String> just = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                subscriber.onNext("zhangshan");
                subscriber.onNext("lisi");
                subscriber.onNext("wnagwu");
            }
        });
        Observable<Integer> tictoc = Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                for (int i = 0; i < 20; i++) {
                    Log.w("TAG", "ZipBufferWindowExampleFragment call tictoc 发送====:" + i);
                    subscriber.onNext(i);
                }
            }
        });
        Observable.zip(just, tictoc, new Func2<String, Integer, String>() {
            @Override
            public String call(String s, Integer aLong) {
                Log.e("TAG", "ZipBufferWindowExampleFragment call s:" + s + " aLong=" + aLong);
                return s + ":" + aLong;
            }
        }).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                Log.e("TAG", "ZipBufferWindowExampleFragment onCompleted:");
            }

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "OperatorZip ZipBufferWindowExampleFragment onError:" + e);
            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "ZipBufferWindowExampleFragment onNext:" + s);
            }
        });
    }
}
