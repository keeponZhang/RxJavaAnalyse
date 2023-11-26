package com.packtpub.apps.rxjava_essentials.chapter6;


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

import androidx.fragment.app.Fragment;

import com.google.common.collect.Lists;
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
import rx.schedulers.Schedulers;


public class MergeExampleFragment extends Fragment {

	@BindView(R.id.fragment_first_example_list)
	RecyclerView mRecyclerView;

	@BindView(R.id.fragment_first_example_swipe_container)
	SwipeRefreshLayout mSwipeRefreshLayout;
	@BindView(R.id.merge)
	Button             mMerge;
	Unbinder unbinder;
	@BindView(R.id.mergeAsyn)
	Button mMergeAsyn;

	private ApplicationAdapter mAdapter;

	private ArrayList<AppInfo> mAddedApps = new ArrayList<>();

	public MergeExampleFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_example_merge, container, false);
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

		loadList(apps);
	}

	private void loadList(List<AppInfo> apps) {
		mRecyclerView.setVisibility(View.VISIBLE);

		List reversedApps = Lists.reverse(apps);

		Observable<AppInfo> observableApps = Observable.from(apps);
		Observable<AppInfo> observableReversedApps = Observable.from(reversedApps);
		//merge跟concat有点像，不过不保证有序
		Observable<AppInfo> mergedObserbable = Observable.merge(observableApps, observableReversedApps);

		mergedObserbable.subscribe(new Observer<AppInfo>() {
			@Override
			public void onCompleted() {
				mSwipeRefreshLayout.setRefreshing(false);
				Toast.makeText(getActivity(), "Here is the list!", Toast.LENGTH_LONG).show();
			}

			@Override
			public void onError(Throwable e) {
				Toast.makeText(getActivity(), "One of the two Observable threw an error!", Toast.LENGTH_SHORT).show();
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


	private void mergeSyn() {
		//这里之所以是顺序的，因为OperatorMerge 的MergeSubscriber的onNext收到的Observable是在同一线程，是顺序的
		Observable<Integer> just = Observable.just(1, 2, 3);
		Observable<Integer> just1 = Observable.just(8, 9, 20);
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


	@OnClick({R.id.merge, R.id.mergeAsyn})
	public void onViewClicked(View view) {
		switch (view.getId()) {
			case R.id.merge:
				mergeSyn();
				break;
			case R.id.mergeAsyn:
				mergeAsyn();
				break;
		}
	}

	private void mergeAsyn() {
		Observable<Integer> just = Observable.create(new Observable.OnSubscribe<Integer>() {
			@Override
			public void call(Subscriber<? super Integer> subscriber) {
				subscriber.onNext(1);
				Log.e("TAG", "OperatorMerge MergeExampleFragment just call:" + Thread.currentThread().getName());
//				try {
//					Thread.sleep(200);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
				subscriber.onNext(2);
				subscriber.onNext(3);
				subscriber.onCompleted();
			}
		}).subscribeOn(Schedulers.io());//不同线程
		Observable<Integer> just1 = Observable.create(new Observable.OnSubscribe<Integer>() {
			@Override
			public void call(Subscriber<? super Integer> subscriber) {
				subscriber.onNext(7);
				Log.e("TAG", "OperatorMerge MergeExampleFragment just1 call:" + Thread.currentThread().getName());
				subscriber.onNext(8);
				subscriber.onNext(9);
				subscriber.onCompleted();
			}
		});
		Observable.merge(just, just1).subscribe(new Subscriber<Integer>() {
			@Override
			public void onCompleted() {

			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(Integer integer) {
				Log.e("TAG", " OperatorMerge MergeExampleFragment mergeAsyn onNext:" + integer);
			}
		});

	}
}
