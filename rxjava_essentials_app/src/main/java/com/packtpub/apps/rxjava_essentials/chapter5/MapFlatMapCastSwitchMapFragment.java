package com.packtpub.apps.rxjava_essentials.chapter5;

import android.app.Fragment;
import android.os.Bundle;
import android.os.SystemClock;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MapFlatMapCastSwitchMapFragment extends Fragment {

    @BindView(R.id.fragment_first_example_list)
    RecyclerView mRecyclerView;

    @BindView(R.id.fragment_first_example_swipe_container)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.map)
    Button mMap;
    @BindView(R.id.flatmap)
    Button mFlatmap;
    @BindView(R.id.cast)
    Button mCast;
    Unbinder unbinder;

    private ApplicationAdapter mAdapter;

    private ArrayList<AppInfo> mAddedApps = new ArrayList<>();
    private List<AppInfo> mApps;

    public MapFlatMapCastSwitchMapFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_example_map_flatmap, container, false);
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
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24,
                        getResources().getDisplayMetrics()));

        // Progress
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(true);
        mRecyclerView.setVisibility(View.GONE);

        mApps = ApplicationsList.getInstance().getList();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick({R.id.map, R.id.merge, R.id.flatmap, R.id.concatmap, R.id.cast, R.id.switchmap})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.map:
                map();
                break;
            case R.id.merge:
                mergeSyn();
                break;
            case R.id.flatmap:
                flatmap();
                break;
            case R.id.concatmap:
                concatmap();
                break;
            case R.id.switchmap:
                swithcmap();
                break;
            case R.id.cast:
                cast();
                break;
        }
    }

    private void map() {
        mRecyclerView.setVisibility(View.VISIBLE);
        //把上层的T转换成R在发送给下层订阅者
        Observable.from(mApps)
                .map(new Func1<AppInfo, AppInfo>() {
                    @Override
                    public AppInfo call(AppInfo appInfo) {
                        String currentName = appInfo.getName();
                        String lowerCaseName = currentName.toLowerCase();
                        appInfo.setName(lowerCaseName);
                        return appInfo;
                    }
                })
                .subscribe(new Observer<AppInfo>() {
                    @Override
                    public void onCompleted() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getActivity(), "Something went south!", Toast.LENGTH_SHORT)
                                .show();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onNext(AppInfo appInfo) {
                        mAddedApps.add(appInfo);
                        mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
                    }
                });
    }

    private void mergeSyn() {
        //这里之所以是顺序的，因为OperatorMerge 的MergeSubscriber的onNext收到的Observable是在同一线程，是顺序的
        Observable<Integer> just = Observable.just(1, 2, 3);
        Observable<Integer> just1 = Observable.just(8, 9, 20);
        //merge里面会创建OnSubscribeFromIterable
        Observable.merge(just, just1).subscribe(new Subscriber<Integer>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                Log.e("TAG", " OperatorMerge MergeExampleFragment mergeSyn onNext:" + integer);
            }
        });
    }

    //flatmap 生成的observable会被InnerObserver订阅一次()，接着在发送下级真正的观察者
    private void flatmap() {
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                for (int i = 0; i < 3; i++) {
                    subscriber.onNext(i);
                }
            }
        }).flatMap(new Func1<Integer, Observable<String>>() {
            //这里为什么要是Observable呢，因为一会这里要调用merge，merge接收的是observable
            @Override
            public Observable<String> call(Integer integer) {

                return getObservable1(integer);
            }
        }).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "MapFlatMapCastSwitchMapFragment onNext:" + s);
            }
        });

    }

    private void concatmap() {
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                for (int i = 0; i < 2; i++) {
                    subscriber.onNext(i);
                    Log.e("TAG", "MapFlatMapCastSwitchMapFragment concatmap call:" + i);
                }
            }
        }).concatMap(new Func1<Integer, Observable<String>>() {
            //这里为什么要是Observable呢，因为一会这里要调用merge，merge接收的是observable
            @Override
            public Observable<String> call(Integer integer) {
                return getObservable1(integer);
            }
        }).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                Log.e("TAG", "MapFlatMapCastSwitchMapFragment  concatmap onCompleted:");
            }

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "MapFlatMapCastSwitchMapFragment concatmap onError:");
            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "MapFlatMapCastSwitchMapFragment 收到 onNext:" + s);
            }
        });

    }

    //其实就是转换类型，不能转回报错
    private void cast() {
        Observable.create(new Observable.OnSubscribe<Float>() {
            @Override
            public void call(Subscriber<? super Float> subscriber) {
                subscriber.onNext(1.1f);
            }
        }).cast(Number.class).subscribe(new Subscriber<Number>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "MapFlatMapCastSwitchMapFragment onError:");
            }

            @Override
            public void onNext(Number integer) {
                Log.e("TAG", "MapFlatMapCastSwitchMapFragment 最后收到 onNext:" + integer);
            }
        });
    }

    //	switchMap() 和 flatMap() 很像，除了一点：每当源Observable,发射一个新的数据项（Observable）时，它将取消订阅并停止监视之前那个数据项
//	产生的Observable，并开始监视当前发射的这一个
    //多线程才能看出效果
    private void swithcmap() {
        Observable.create(new Observable.OnSubscribe<Integer>() {
            @Override
            public void call(Subscriber<? super Integer> subscriber) {
                for (int i = 1; i < 4; i++) {
                    subscriber.onNext(i);
                }
            }
        }).switchMap(new Func1<Integer, Observable<String>>() {
            @Override
            public Observable<String> call(Integer integer) {
                return getObservable(integer);
            }
        }).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                Log.e("TAG", "MapFlatMapCastSwitchMapFragment onCompleted:");
            }

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "MapFlatMapCastSwitchMapFragment onError:");
            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "MapFlatMapCastSwitchMapFragment  swithcmap onNext:" + s);
            }
        });

    }

    private Observable<String> getObservable(Integer integer) {
        ArrayList<String> strings = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            strings.add("我是" + integer + "班;学号" + i);
        }
        Observable<String> stringObservable =
                Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        if (integer == 1) {
                            for (int i = 0; i < strings.size(); i++) {
                                String s = strings.get(i);
                                if (i == 1) {
                                    SystemClock.sleep(500);
                                }
                                subscriber.onNext(s);
                            }
                        } else if (integer == 2) {
                            for (int i = 0; i < strings.size(); i++) {
                                String s = strings.get(i);
                                if (i == 2) {
                                    SystemClock.sleep(500);
                                }
                                subscriber.onNext(s);
                            }
                        } else {
                            for (int i = 0; i < strings.size(); i++) {
                                String s = strings.get(i);
                                subscriber.onNext(s);

                            }
                        }
                    }
                });
        return stringObservable.subscribeOn(Schedulers.io());
    }

    private Observable<String> getObservable1(Integer integer) {
        Observable<String> stringObservable =
                Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        if (integer == 0) {
                            SystemClock.sleep(2000);
                        }
                        subscriber.onNext("keepon index " + integer + "  Thread.name=" +
                                Thread.currentThread().getName());
                        Log.w("TAG",
                                "MapFlatMapCastSwitchMapFragment MapFlatMapCastSwitchMapFragment call:" +
                                        integer);
                        subscriber.onCompleted();
                    }
                }).subscribeOn(Schedulers.newThread());
        return stringObservable;
    }
}
