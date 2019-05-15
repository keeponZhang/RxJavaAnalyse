package com.packtpub.apps.rxjava_essentials.chapter4;


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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func0;


public class TakeExampleFragment extends Fragment {

	@BindView(R.id.fragment_first_example_list)
	RecyclerView mRecyclerView;

	@BindView(R.id.fragment_first_example_swipe_container)
	SwipeRefreshLayout mSwipeRefreshLayout;
	@BindView(R.id.takeLast)
	Button             mTakeLast;
	Unbinder unbinder;
	@BindView(R.id.repeat)
	Button mRepeat;
	@BindView(R.id.range)
	Button mRange;
	@BindView(R.id.defer)
	Button mDefer;

	private ApplicationAdapter mAdapter;

	private ArrayList<AppInfo> mAddedApps = new ArrayList<>();
	private List<AppInfo>      mApps;

	public TakeExampleFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_example_takelast, container, false);
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

		mApps = ApplicationsList.getInstance().getList().subList(0, 5);
		loadList(mApps);
	}

	private void loadList(List<AppInfo> apps) {
		mRecyclerView.setVisibility(View.VISIBLE);
		Observable.from(apps)
				.take(3)
				.subscribe(new Observer<AppInfo>() {
					@Override
					public void onCompleted() {
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onError(Throwable e) {
						Toast.makeText(getActivity(), "Something went south!", Toast.LENGTH_SHORT).show();
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onNext(AppInfo appInfo) {
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}

	public void takeLast() {
		//		mAdapter.clearData();
		Observable.from(mApps)
				.takeLast(3)
				.subscribe(new Observer<AppInfo>() {
					@Override
					public void onCompleted() {
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onError(Throwable e) {
						Toast.makeText(getActivity(), "Something went south!", Toast.LENGTH_SHORT).show();
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onNext(AppInfo appInfo) {
						Log.e("TAG", "TakeExampleFragment onNext:" + appInfo.getName());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@OnClick({R.id.takeLast, R.id.repeat, R.id.range})
	public void onViewClicked(View view) {
		switch (view.getId()) {
			case R.id.takeLast:
				takeLast();
				break;
			case R.id.repeat:
				repeat();
				break;
			case R.id.range:
				range();
				break;
		}
	}

	private void repeat() {
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
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onError(Throwable e) {
						Toast.makeText(getActivity(), "Something went south!", Toast.LENGTH_SHORT).show();
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onNext(AppInfo appInfo) {
						Log.e("TAG", "TakeExampleFragment onNext:" + appInfo.getName());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}


	public void range() {
		Observable.range(10, 3)
				.subscribe(new Observer<Integer>() {
					@Override
					public void onCompleted() {
						Log.e("TAG", "TakeExampleFragment onCompleted:");

					}

					@Override
					public void onError(Throwable e) {
						Log.e("TAG", "TakeExampleFragment onError:");
					}

					@Override
					public void onNext(Integer number) {
						Log.e("TAG", "TakeExampleFragment onNext:" + number);

					}
				});
	}

	Observable<Integer> deferred = Observable.defer(getInt());

	private Func0 getInt() {
		Observable<Integer> observable = Observable.create(new Observable.OnSubscribe<Integer>() {
			@Override
			public void call(Subscriber<? super Integer> subscriber) {
				if (subscriber.isUnsubscribed()) {
					return;
				}
				Log.e("TAG", "TakeExampleFragment getInt:");
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

	@OnClick(R.id.defer)
	public void onViewClicked() {
		deferred.subscribe(new Subscriber<Integer>() {
			@Override
			public void onCompleted() {

			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(Integer integer) {
				Log.e("TAG", "TakeExampleFragment onViewClicked number:" + integer);
			}
		});

	}
}
