package com.packtpub.apps.rxjava_essentials.example3;


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
import rx.Subscription;

public class ThirdExampleFragment extends Fragment {

	@BindView(R.id.fragment_first_example_list)
	RecyclerView mRecyclerView;

	@BindView(R.id.fragment_first_example_swipe_container)
	SwipeRefreshLayout mSwipeRefreshLayout;
	@BindView(R.id.timer)
	Button             mTimer;
	@BindView(R.id.just)
	Button             mJust;
	Unbinder unbinder;
	@BindView(R.id.timer2)
	Button mTimer2;

	private ApplicationAdapter mAdapter;

	private ArrayList<AppInfo> mAddedApps = new ArrayList<>();

	private Subscription  mTimeSubscription;
	private List<AppInfo> mApps;
	private AppInfo       mAppOne;
	private AppInfo       mAppTwo;
	private AppInfo       mAppThree;

	public ThirdExampleFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_example_3, container, false);
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

		mAppOne = mApps.get(0);

		mAppTwo = mApps.get(1);

		mAppThree = mApps.get(2);

		loadApps(mAppOne, mAppTwo, mAppThree);

	}

	private void loadApps(AppInfo appOne, AppInfo appTwo, AppInfo appThree) {
		mRecyclerView.setVisibility(View.VISIBLE);

		just(appOne, appTwo, appThree);

//		timer();
	}

	private void just(AppInfo appOne, AppInfo appTwo, AppInfo appThree) {
		Observable<AppInfo> threeOfThem = Observable.just(appOne, appTwo, appThree);

		threeOfThem.subscribe(new Observer<AppInfo>() {
			@Override
			public void onCompleted() {
				mSwipeRefreshLayout.setRefreshing(false);
				Toast.makeText(getActivity(), "Here is the list!", Toast.LENGTH_LONG).show();
			}

			@Override
			public void onError(Throwable e) {
				Toast.makeText(getActivity(), "Something went wrong!", Toast.LENGTH_SHORT).show();
				mSwipeRefreshLayout.setRefreshing(false);
			}

			@Override
			public void onNext(AppInfo appInfo) {
				if (mAddedApps != null) {
					mAddedApps.clear();
				}
				mAddedApps.add(appInfo);
				mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
			}
		});
	}

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
						Log.e("RXJAVA", "I say " + number);
					}
				});
	}

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
		if (!mTimeSubscription.isUnsubscribed()) {
			mTimeSubscription.unsubscribe();
		}
		unbinder.unbind();
	}

	@OnClick({R.id.timer, R.id.just})
	public void onViewClicked(View view) {
		switch (view.getId()) {
			case R.id.timer:
				timer();
				break;
			case R.id.just:
				just(mAppOne, mAppTwo, mAppThree);
				break;
		}
	}

	@OnClick(R.id.timer2)
	public void onViewClicked() {
		timer2();
	}
}
