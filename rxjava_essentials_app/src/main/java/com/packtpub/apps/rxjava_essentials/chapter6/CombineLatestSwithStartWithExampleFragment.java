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
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;


public class CombineLatestSwithStartWithExampleFragment extends Fragment {

	@BindView(R.id.fragment_first_example_list)
	RecyclerView mRecyclerView;

	@BindView(R.id.fragment_first_example_swipe_container)
	SwipeRefreshLayout mSwipeRefreshLayout;
	@BindView(R.id.combinelastest)
	Button             mCombinelastest;
	@BindView(R.id.switche)
	Button             mSwitche;
	@BindView(R.id.startwith)
	Button             mStartwith;
	Unbinder unbinder;

	private ApplicationAdapter mAdapter;

	private ArrayList<AppInfo> mAddedApps = new ArrayList<>();
	private List<AppInfo> mApps;

	public CombineLatestSwithStartWithExampleFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_example_combinelastest, container, false);
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

		loadList(mApps.subList(0,5));
	}

	private void loadList(List<AppInfo> apps) {
		mRecyclerView.setVisibility(View.VISIBLE);

		Observable<AppInfo> appsSequence = Observable.interval(1000, TimeUnit.MILLISECONDS)
				.map(position -> apps.get(position.intValue()));

		Observable<Long> tictoc = Observable.interval(1500, TimeUnit.MILLISECONDS);

		Observable
				.combineLatest(appsSequence, tictoc, this::updateTitle)
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

    //正如我们已经学习的， zip() 作用于最近未打包的两个Observables。相
    //反， combineLatest() 作用于最近发射的数据项
	public void combinelastest() {
		Observable<Integer> just = Observable.just(1, 2, 3, 4);
		Observable<String> just1 = Observable.just("zhangsan", "lisi", "wangwu");
		//4zhangsan,4lisi,4wangwu
		Observable.combineLatest(just, just1, new Func2<Integer, String, String>() {
			@Override
			public String call(Integer integer, String s) {
				return integer+s;
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
				Log.e("TAG", "CombineLatestSwithStartWithExampleFragment  combinelastest onNext:" + s);
			}
		});

	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@OnClick({R.id.combinelastest,R.id.combinelastest2 ,R.id.switche, R.id.startwith})
	public void onViewClicked(View view) {
		switch (view.getId()) {
			case R.id.combinelastest:
				combinelastest();
				break;
			case R.id.combinelastest2:
				combinelastest2();
				break;
			case R.id.switche:
				SwitchOnNext();
				break;
			case R.id.startwith:
				startwith();
				break;
		}
	}

	//当有多个Observable发送数据时，下一个Observable发送数据时，会取消上一个订阅
	private void SwitchOnNext() {
		//每隔500ms产生一个Observable
		Observable<Observable<Long>> observable = Observable.interval(500, TimeUnit.MILLISECONDS)
				.map(new Func1<Long, Observable<Long>>() {
					@Override
					public Observable<Long> call(Long aLong) {
						//每隔200毫秒产生一组数据（0,10,20,30,40)
						return Observable.interval(200, TimeUnit.MILLISECONDS)
								.map(new Func1<Long, Long>() {
									@Override
									public Long call(Long aLong) {
										return aLong * 10;
									}
								}).take(5);
					}
				}).take(2);

		Observable.switchOnNext(observable)
				.subscribe(new Action1<Long>() {
					@Override
					public void call(Long aLong) {
						Log.e("TAG", "CombineLatestSwithStartWithExampleFragment call:" + aLong);
					}
				});
	}

	//原理就是使用concat保证startWith后面的先发送
	private void startwith() {
		Observable.just(4, 5, 6, 7)
				//startwith的实现很简单，是调用 concat(just(t1, t2, t3), this);
				.startWith(1, 2, 3)
				.subscribe(new Action1<Integer>() {
					@Override
					public void call(Integer integer) {
						Log.e("rx_test", "startWith：" + integer);
					}
				});
	}

	private void combinelastest2() {
		Observable<Integer> observable = Observable.create(new Observable.OnSubscribe<Integer>() {
			@Override
			public void call(Subscriber<? super Integer> subscriber) {
				for (int i = 0; i < 4; i++) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					Log.d("TAG", "CombineLatestSwithStartWithExampleFragment observable call:"+i);
					subscriber.onNext(i);
				}


			}
		}).subscribeOn(Schedulers.newThread());
		Observable<String> observable2 = Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> subscriber) {
				for (int i = 0; i <3; i++) {
					try {
						Thread.sleep(400);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					if(i==0){
						Log.d("TAG", "CombineLatestSwithStartWithExampleFragment observable2 call Zhangsan:");
						subscriber.onNext("Zhangsan");
					}else if(i==1){
						Log.d("TAG", "CombineLatestSwithStartWithExampleFragment observable2 call Lisi:");
						subscriber.onNext("Lisi");
					}else{
						Log.d("TAG", "CombineLatestSwithStartWithExampleFragment observable2 call wangwu:");
						subscriber.onNext("wangwu");
					}
				}

			}
		}).subscribeOn(Schedulers.newThread());

		Observable.combineLatest(observable, observable2, new Func2<Integer, String, String>() {
			@Override
			public String call(Integer integer, String s) {
				return integer+s;
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
				Log.e("TAG", "CombineLatestSwithStartWithExampleFragment  combinelastest2 onNext:" + s);
			}
		});
	}
}
