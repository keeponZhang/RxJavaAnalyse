package com.packtpub.apps.rxjava_essentials.chapter4;


import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import rx.schedulers.Schedulers;


public class FilterFirstSingleSampleTimeoutDebounceFragment extends Fragment {

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
	@BindView(R.id.elementAt)
	Button mElementAt;
	@BindView(R.id.sample)
	Button mSample;
	@BindView(R.id.debounce)
	Button mDebounce;
	@BindView(R.id.timeout)
	Button mTimeout;
	@BindView(R.id.skip)
	Button mSkip;

	private ApplicationAdapter mAdapter;

	private ArrayList<AppInfo> mAddedApps = new ArrayList<>();
	private List<AppInfo>      mApps;

	public FilterFirstSingleSampleTimeoutDebounceFragment() {
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

		mApps = ApplicationsList.getInstance().getList().subList(0,16);

		loadList(mApps);
	}

	private void loadList(List<AppInfo> apps) {
		mRecyclerView.setVisibility(View.VISIBLE);

		Observable.from(apps)
				.filter((appInfo) -> appInfo.getName().contains("C"))
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

	private void single() {
		//single 如果发送多个的话，一个也收不到，会收到onError
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


	@OnClick({R.id.first, R.id.firstFun, R.id.single, R.id.elementAt, R.id.sample, R.id.debounce, R.id.timeout,R.id.throttleFirst})
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
			case R.id.elementAt:
				elementAt();
				break;
			case R.id.throttleFirst:
				throttleFirst();
				break;
			case R.id.sample:
			sample();
				break;
			case R.id.debounce:
				debounce();
				break;
			case R.id.timeout:
				timeout();
				break;
		}
	}

	private void timeout() {
		Observable.create(new Observable.OnSubscribe<AppInfo>() {

			@Override
			public void call(Subscriber<? super AppInfo> subscriber) {
				for (int i = 0; i < mApps.size(); i++) {
					if(i==10){
						SystemClock.sleep(1200);
					}else{
						SystemClock.sleep(200);
					}
					subscriber.onNext(mApps.get(i));
				}
				subscriber.onCompleted();
			}
		}).subscribeOn(Schedulers.io())
				.timeout(600,TimeUnit.MILLISECONDS)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<AppInfo>() {
					@Override
					public void onCompleted() {
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onError(Throwable e) {
						Toast.makeText(getActivity(), "Something onError", Toast.LENGTH_SHORT).show();
						Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment OperatorTimeoutBase onError:");
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onNext(AppInfo appInfo) {
						Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment OperatorTimeoutBase onNext:" + appInfo.getName()+ " "+System.currentTimeMillis());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}

	private void debounce() {
		Observable.create(new Observable.OnSubscribe<AppInfo>() {

			@Override
			public void call(Subscriber<? super AppInfo> subscriber) {
				for (int i = 0; i < mApps.size(); i++) {
					if(i==4){
						SystemClock.sleep(1200);
					}else{
						SystemClock.sleep(200);
					}
					Log.d("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment call onNext:"+mApps.get(i));
					subscriber.onNext(mApps.get(i));
				}
				subscriber.onCompleted();
			}
		}).subscribeOn(Schedulers.io())
				.debounce(1,TimeUnit.SECONDS)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<AppInfo>() {
					@Override
					public void onCompleted() {
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onError(Throwable e) {
						Toast.makeText(getActivity(), "Something onError", Toast.LENGTH_SHORT).show();
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onNext(AppInfo appInfo) {
						Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment onNext:" + appInfo.getName()+ " "+System.currentTimeMillis());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}
	int count=0;
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (count >= mApps.size()) {
				mSampleSubscriber.onCompleted();
				return;
			}
			mSampleSubscriber.onNext(mApps.get(count));
			Log.d("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment handleMessage onNext:"+mApps.get(count)+ "  "+System.currentTimeMillis());
			count++;
			mHandler.sendMessageDelayed(Message.obtain(), 200);
		}
	};
	private Subscriber<? super AppInfo> mSampleSubscriber;
	private void sample() {
		count = 0;
		Observable.create(new Observable.OnSubscribe<AppInfo>() {

			@Override
			public void call(Subscriber<? super AppInfo> subscriber) {
				for (int i = 0; i < mApps.size(); i++) {
					if(i==4){
						SystemClock.sleep(1200);
					}else{
						SystemClock.sleep(200);
					}
					Log.d("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment  sample call onNext:"+mApps.get(i));
					subscriber.onNext(mApps.get(i));
				}
				subscriber.onCompleted();
			}
		}).subscribeOn(Schedulers.io())
				//sample是采样，如果这个时间段内没法送，不会发送最后一个
				.sample(1,TimeUnit.SECONDS)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<AppInfo>() {
					@Override
					public void onCompleted() {
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onError(Throwable e) {
						Toast.makeText(getActivity(), "Something onError!", Toast.LENGTH_SHORT).show();
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onNext(AppInfo appInfo) {
						Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment sample onNext:" + appInfo.getName()+ " "+Thread.currentThread().getName());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}

	private void throttleFirst() {
		count = 0;
		Observable.create(new Observable.OnSubscribe<AppInfo>() {

			@Override
			public void call(Subscriber<? super AppInfo> subscriber) {
				for (int i = 0; i < mApps.size(); i++) {
					if(i==4){
						SystemClock.sleep(1200);
					}else{
						SystemClock.sleep(200);
					}
					Log.d("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment  throttleFirst call onNext:"+mApps.get(i));
					subscriber.onNext(mApps.get(i));
				}
				subscriber.onCompleted();
			}
		}).subscribeOn(Schedulers.io())
				.throttleFirst(1,TimeUnit.SECONDS)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<AppInfo>() {
					@Override
					public void onCompleted() {
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onError(Throwable e) {
						Toast.makeText(getActivity(), "Something onError!", Toast.LENGTH_SHORT).show();
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onNext(AppInfo appInfo) {
						Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment  throttleFirst onNext:" + appInfo.getName()+ " "+System.currentTimeMillis());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}

	private void elementAt() {
		Observable.from(mApps)
				.elementAt(3)
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
						Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment  elementAt onNext:" + appInfo.getName());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}

	@OnClick(R.id.skip)
	public void onViewClicked() {
		skip();
	}

	private void skip() {
		Observable.from(mApps)
				//跟take,takeLast相对应
//				.skip(3)
				.skipLast(3)
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
						Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment skip onNext:" + appInfo.getName());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}
}
