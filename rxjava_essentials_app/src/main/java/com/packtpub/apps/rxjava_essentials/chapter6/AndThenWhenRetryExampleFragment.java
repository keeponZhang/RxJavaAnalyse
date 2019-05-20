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
import rx.joins.Pattern2;
import rx.joins.Plan0;
import rx.observables.JoinObservable;
import rx.subjects.PublishSubject;


public class AndThenWhenRetryExampleFragment extends Fragment {

	@BindView(R.id.fragment_first_example_list)
	RecyclerView mRecyclerView;

	@BindView(R.id.fragment_first_example_swipe_container)
	SwipeRefreshLayout mSwipeRefreshLayout;
	@BindView(R.id.andthenwhen)
	Button             mAndthenwhen;
	@BindView(R.id.retry)
	Button             mRetry;
	@BindView(R.id.retryWhen)
	Button             mRetryWhen;
	Unbinder unbinder;
	@BindView(R.id.asyncSubject)
	Button mAsyncSubject;
	@BindView(R.id.behaviorSubject)
	Button mBehaviorSubject;
	@BindView(R.id.publishSubject)
	Button mPublishSubject;
	@BindView(R.id.replaySubject)
	Button mReplaySubject;

	private ApplicationAdapter mAdapter;

	private ArrayList<AppInfo> mAddedApps = new ArrayList<>();
	private List<AppInfo>      mApps;

	public AndThenWhenRetryExampleFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_example_andthenwhen, container, false);
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

		loadList(mApps.subList(0, 2));
	}

	private void loadList(List<AppInfo> apps) {
		mRecyclerView.setVisibility(View.VISIBLE);

		Observable<AppInfo> observableApp = Observable.from(apps);

		Observable<Long> tictoc = Observable.interval(1, TimeUnit.SECONDS);

		Pattern2<AppInfo, Long> pattern = JoinObservable.from(observableApp).and(tictoc);
		Plan0<AppInfo> plan = pattern.then(new Func2<AppInfo, Long, AppInfo>() {
			@Override
			public AppInfo call(AppInfo appInfo, Long time) {
				appInfo.setName(time + " " + appInfo.getName());
				return appInfo;
			}
		});
		//		Plan0<AppInfo> plan = pattern.then(this::updateTitle);
		//目前来看，跟zip效果一样
		JoinObservable
				.when(plan)
				.toObservable()
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


	private void retryWhen() {
	}

	private void retry() {
		Observable<Integer> observable = Observable.create(new Observable.OnSubscribe<Integer>() {
			@Override
			public void call(Subscriber<? super Integer> subscriber) {
				if (subscriber.isUnsubscribed())
					return;
				//循环输出数字
				try {
					for (int i = 0; i < 10; i++) {
						if (i == 4) {
							throw new Exception("this is number 4 error！");
						}
						subscriber.onNext(i);
					}
					subscriber.onCompleted();
				} catch (Throwable e) {
					subscriber.onError(e);
				}
			}
		});

		observable.retry(2).subscribe(new Subscriber<Integer>() {
			@Override
			public void onCompleted() {
				Log.e("TAG", "AndThenWhenRetryExampleFragment onCompleted:");
			}

			@Override
			public void onError(Throwable e) {
				Log.e("TAG", "AndThenWhenRetryExampleFragment onError:");
			}

			@Override
			public void onNext(Integer value) {
				Log.e("TAG", "AndThenWhenRetryExampleFragment onNext:" + value);
			}
		});

	}

	private void andthenwhen() {

	}

	private void publishSubject() {
		PublishSubject publishSubject = PublishSubject.create();
		publishSubject.onNext("publishSubject1");
		publishSubject.onNext("publishSubject2");
		publishSubject.subscribe(new Observer() {
			@Override
			public void onCompleted() {

			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(Object o) {

			}
		});
		publishSubject.onNext("publishSubject3");
		publishSubject.onNext("publishSubject4");
	}

	@OnClick({R.id.andthenwhen, R.id.retry, R.id.retryWhen, R.id.asyncSubject, R.id.behaviorSubject, R.id.publishSubject, R.id.replaySubject})
	public void onViewClicked(View view) {
		switch (view.getId()) {
			case R.id.andthenwhen:
				andthenwhen();
				break;
			case R.id.retry:
				retry();
				break;
			case R.id.retryWhen:
				retryWhen();
				break;
			case R.id.asyncSubject:

				break;
			case R.id.behaviorSubject:
				break;
			case R.id.publishSubject:
				publishSubject();
				break;
			case R.id.replaySubject:
				break;
		}
	}
}
