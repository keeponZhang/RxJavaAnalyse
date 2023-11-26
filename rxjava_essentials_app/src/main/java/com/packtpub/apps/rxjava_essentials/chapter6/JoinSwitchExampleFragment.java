package com.packtpub.apps.rxjava_essentials.chapter6;


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

import androidx.fragment.app.Fragment;

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
import rx.functions.Func1;
import rx.functions.Func2;
import timber.log.Timber;


public class JoinSwitchExampleFragment extends Fragment {

	@BindView(R.id.fragment_first_example_list)
	RecyclerView mRecyclerView;

	@BindView(R.id.fragment_first_example_swipe_container)
	SwipeRefreshLayout mSwipeRefreshLayout;
	@BindView(R.id.join)
	Button             mJoin;
	@BindView(R.id.groupjoin)
	Button             mGropujoin;
	Unbinder unbinder;

	private ApplicationAdapter mAdapter;

	private ArrayList<AppInfo> mAddedApps = new ArrayList<>();

	public JoinSwitchExampleFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_example_join, container, false);
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

		List<AppInfo> apps = ApplicationsList.getInstance().getList();

//		loadList(apps);
	}

	private void loadList(List<AppInfo> apps) {
		mRecyclerView.setVisibility(View.VISIBLE);

		Observable<AppInfo> appsSequence = Observable.interval(1000, TimeUnit.MILLISECONDS)
				.map(position -> {
					Timber.d("Position: " + position);
					return apps.get(position.intValue());
				});
		Observable<Long> tictoc = Observable.interval(1000, TimeUnit.MILLISECONDS);

		appsSequence
				.join(
						tictoc, appInfo -> Observable.timer(2, TimeUnit.SECONDS),
						time -> Observable.timer(0, TimeUnit.SECONDS),
						this::updateTitle
				)
				.observeOn(AndroidSchedulers.mainThread())
				.take(10)
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

	@OnClick({R.id.join, R.id.groupjoin})
	public void onViewClicked(View view) {
		switch (view.getId()) {
			case R.id.join:
				join();
				break;
			case R.id.groupjoin:
				groupjoin();
				break;
		}
	}

	private void groupjoin() {
	}

	private void join() {

		Observable<Integer> observableA = Observable.range(1, 4);

		Observable<Integer> observableB = Observable.create(new Observable.OnSubscribe<Integer>() {
			@Override
			public void call(Subscriber<? super Integer> subscriber) {
				for (int i = 6; i < 11; i++) {
					if(i ==10){
						SystemClock.sleep(1000);
					}
					subscriber.onNext(i);
				}
				subscriber.onCompleted();
			}
		});
        /* Join(Observable,Func1,Func1,Func2) 需要传递四个参数
		join操作符的用法如下： observableA.join(observableB, observableA产生结果生命周期控制函数，
		 observableB产生结果生命周期控制函数， observableA产生的结果与observableB产生的结果的合并规则）*/
		/*	源Observable所要组合的目标Observable
		一个函数，就收从源Observable发射来的数据，并返回一个Observable，这个Observable的生命周期决定了源Observable发射出来数据的有效期
		一个函数，就收从目标Observable发射来的数据，并返回一个Observable，这个Observable的生命周期决定了目标Observable发射出来数据的有效期
		一个函数，接收从源Observable和目标Observable发射来的数据，并返回最终组合完的数据。*/
		observableA.join(observableB, new Func1<Integer, Observable<Integer>>() {
			@Override
			public Observable<Integer> call(Integer value) {
				//return Observable.just(value);
				Log.e("TAG", "JoinSwitchExampleFragment call A Func1 value:"+value+"  time="+ System.currentTimeMillis());
				//这里规定了observableA发出来的数据1s内都是有效的，所以可以join
				return Observable.just(value).delay(1, TimeUnit.SECONDS);
				//用下面这个，不会结合
//				return Observable.just(value);
			}
		}, new Func1<Integer, Observable<Integer>>() {
			@Override
			public Observable<Integer> call(Integer value) {
				Log.w("TAG", "JoinSwitchExampleFragment call B Func1 value:"+value+"  time="+ System.currentTimeMillis());
				return Observable.just(value);
//				return Observable.just(value).delay(1, TimeUnit.SECONDS);
			}
		}, new Func2<Integer, Integer, Integer>() {
			@Override
			public Integer call(Integer value1, Integer value2  ) {
				Log.w("TAG", "JoinSwitchExampleFragment  join Func2 call:"+"left value1: " + value1 + "  right value2:" + value2 +"  time="+ System.currentTimeMillis());
				return value1 + value2;
			}
		}).subscribe(new Observer<Integer>() {
			@Override
			public void onCompleted() {
				Log.e("TAG", "JoinSwitchExampleFragment join onCompleted:");
			}

			@Override
			public void onError(Throwable e) {
			}

			@Override
			public void onNext(Integer value) {
				Log.e("TAG", "JoinSwitchExampleFragment join Observer 收到 onNext:"+value);
			}
		});

	}
}
