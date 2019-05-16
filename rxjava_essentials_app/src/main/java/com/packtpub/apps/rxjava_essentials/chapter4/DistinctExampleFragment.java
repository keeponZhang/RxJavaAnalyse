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
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

public class DistinctExampleFragment extends Fragment {

	@BindView(R.id.fragment_first_example_list)
	RecyclerView mRecyclerView;

	@BindView(R.id.fragment_first_example_swipe_container)
	SwipeRefreshLayout mSwipeRefreshLayout;
	@BindView(R.id.distinct)
	Button             mDistinct;
	@BindView(R.id.distinctFun)
	Button             mDistinctFun;
	@BindView(R.id.distinctUntilChanged)
	Button             mDistinctUntilChanged;
	@BindView(R.id.distinctUntilChangedfun)
	Button             mDistinctUntilChangedfun;
	Unbinder unbinder;

	private ApplicationAdapter mAdapter;

	private ArrayList<AppInfo> mAddedApps = new ArrayList<>();
	private List<AppInfo> mApps;

	public DistinctExampleFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_example_distinct, container, false);
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
		distinct();
	}

	private void distinct() {
		mSwipeRefreshLayout.setEnabled(false);
		mSwipeRefreshLayout.setRefreshing(true);
		mRecyclerView.setVisibility(View.GONE);
		mApps = ApplicationsList.getInstance().getList();
		loadList(mApps);
	}

	private void loadList(List<AppInfo> apps) {
		mRecyclerView.setVisibility(View.VISIBLE);
		Observable<AppInfo> fullOfDuplicates = Observable.from(apps)
				.take(3)
				.repeat(3);
		//distinct过滤重复的
		fullOfDuplicates.distinct()
				.subscribe(new Observer<AppInfo>() {
					@Override
					public void onCompleted() {
						Log.e("TAG", "DistinctExampleFragment onCompleted:");
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onError(Throwable e) {
						Log.e("TAG", "DistinctExampleFragment onError:");
						Toast.makeText(getActivity(), "Something went south!", Toast.LENGTH_SHORT).show();
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onNext(AppInfo appInfo) {
						Log.e("TAG", "DistinctExampleFragment onNext:" + appInfo.getName());
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

	@OnClick({R.id.distinct, R.id.distinctFun, R.id.distinctUntilChanged, R.id.distinctUntilChangedfun})
	public void onViewClicked(View view) {
		switch (view.getId()) {
			case R.id.distinct:
				distinct();
				break;
			case R.id.distinctFun:
				distinctFun();
				break;
			case R.id.distinctUntilChanged:
				distinctUntilChanged();
				break;
			case R.id.distinctUntilChangedfun:
				distinctUntilChangedfun();
				break;
		}
	}

	private void distinctUntilChangedfun() {

	}

	private void distinctFun() {
		Observable<AppInfo> fullOfDuplicates = Observable.from(mApps)
				.take(3)
				.repeat(3);
		//distinct过滤重复的
		fullOfDuplicates.distinct(new Func1<AppInfo, AppInfo>() {
			@Override
			public AppInfo call(AppInfo appInfo) {
				appInfo.setName(appInfo.getName()+":change");
				return null;
			}
		})
				.subscribe(new Observer<AppInfo>() {
					@Override
					public void onCompleted() {
						Log.e("TAG", "DistinctExampleFragment onCompleted:");
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onError(Throwable e) {
						Log.e("TAG", "DistinctExampleFragment onError:");
						Toast.makeText(getActivity(), "Something went south!", Toast.LENGTH_SHORT).show();
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onNext(AppInfo appInfo) {
						Log.e("TAG", "DistinctExampleFragment onNext:" + appInfo.getName());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}

	private void distinctUntilChanged() {
				//去重，连续的，后面跟前面一样的
			 Observable.interval(1, TimeUnit.SECONDS)
					.map(new Func1<Long, AppInfo>() {
						@Override
						public AppInfo call(Long aLong) {

//							if (aLong.intValue() == mAddedApps.size() - 1) {
//								if (!mDistinctInterval.isUnsubscribed()) {
//									mDistinctInterval.unsubscribe();
//								}
//							}

							if (aLong.intValue() % 3 == 0) {
								Log.e("TAG", "DistinctExampleFragment call:"+mApps.get(aLong.intValue()).getName());
								return mApps.get(aLong.intValue());
							}
							Log.e("TAG", "DistinctExampleFragment call:"+mApps.get(3).getName());

							return mApps.get(3);
						}
					})
					.distinctUntilChanged()
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(new Action1<AppInfo>() {
						@Override
						public void call(AppInfo appInfo) {
							mAddedApps.add(appInfo);
							mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
						}
					});
		}
}
