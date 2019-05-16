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
import rx.functions.Func1;


public class FilterFirstSingleExampleFragment extends Fragment {

	@BindView(R.id.fragment_first_example_list)
	RecyclerView mRecyclerView;

	@BindView(R.id.fragment_first_example_swipe_container)
	SwipeRefreshLayout mSwipeRefreshLayout;
	@BindView(R.id.first)
	Button             mFirst;
	@BindView(R.id.firstFun)
	Button             mFirstFun;
	Unbinder unbinder;
	@BindView(R.id.single)
	Button mSingle;

	private ApplicationAdapter mAdapter;

	private ArrayList<AppInfo> mAddedApps = new ArrayList<>();
	private List<AppInfo>      mApps;

	public FilterFirstSingleExampleFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_example_fliter_first_single, container, false);
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

		loadList(mApps);
	}

	private void loadList(List<AppInfo> apps) {
		mRecyclerView.setVisibility(View.VISIBLE);

		Observable.from(apps)
				.filter((appInfo) -> appInfo.getName().startsWith("C"))
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

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@OnClick({R.id.first, R.id.firstFun,R.id.single})
	public void onViewClicked(View view) {
		switch (view.getId()) {
			case R.id.first:
				first();
				break;
			case R.id.firstFun:
				firstFun();
				break;
			case R.id.single:
				single();
				break;
		}
	}

	private void single() {
		Observable.from(mApps)
				.single()
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
						Log.e("TAG", "TakeRepeatRangeDeferExampleFragment onNext:" + appInfo.getName());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}

	private void firstFun() {
		Observable.from(mApps)
				//first(Func1),会取符合条件的第一个
				.first(new Func1<AppInfo, Boolean>() {
					@Override
					public Boolean call(AppInfo appInfo) {
						return null;
					}
				})
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
						Log.e("TAG", "TakeRepeatRangeDeferExampleFragment onNext:" + appInfo.getName());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}

	private void first() {
		Observable.from(mApps)
				//take(1),只会取第一个
				.first()
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
						Log.e("TAG", "TakeRepeatRangeDeferExampleFragment onNext:" + appInfo.getName());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}


}
