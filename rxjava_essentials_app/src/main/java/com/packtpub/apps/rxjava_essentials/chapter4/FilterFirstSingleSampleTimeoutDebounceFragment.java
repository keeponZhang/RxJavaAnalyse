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
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
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
	@BindView(R.id.unsubscribe)
	Button mUnsubscribe;

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

		mApps = ApplicationsList.getInstance().getList().subList(0,3);

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
	//记住发送的数据，如果被观察者发送多个是时会发送onError
	private void single() {
		//single 如果发送多个的话，一个也收不到，会收到onError
		Observable.just(mApps.get(0))
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

	//获取第一个符合条件的
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
	//获取第一个发送的数据
	private void first() {
		Observable.from(mApps)
				//take(1),只会取第一个
				//first()==take(1).single()
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


	@OnClick({R.id.first, R.id.firstFun, R.id.single, R.id.elementAt, R.id.sample, R.id.debounce, R.id.timeout,R.id.throttleFirst,R.id.unsubscribe})
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
			case R.id.unsubscribe:
				unsubscribe();
				break;
		}
	}
	//开启定时任务，超时了发送onError
	private void timeout() {
		Observable.create(new Observable.OnSubscribe<AppInfo>() {

			@Override
			public void call(Subscriber<? super AppInfo> subscriber) {
				SystemClock.sleep(2000);
				for (int i = 0; i < mApps.size(); i++) {
					if(i==2){
						SystemClock.sleep(2000);
						break;
					}
					Log.w("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment call 发射:"+mApps.get(i));
					subscriber.onNext(mApps.get(i));
				}
				subscriber.onCompleted();
			}
		}).subscribeOn(Schedulers.io())
				.timeout(1500,TimeUnit.MILLISECONDS)
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Observer<AppInfo>() {
					@Override
					public void onCompleted() {
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onError(Throwable e) {
						Toast.makeText(getActivity(), "Something onError", Toast.LENGTH_SHORT).show();
						Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment 收到 OperatorTimeoutBase onError:"+e);
						mSwipeRefreshLayout.setRefreshing(false);
					}

					@Override
					public void onNext(AppInfo appInfo) {
						Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment 收到 OperatorTimeoutBase onNext:" + appInfo.getName()+ " "+System.currentTimeMillis());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}
	//取消定时任务，所以如果没有切换线程，一般是没有用的
	private void unsubscribe() {
		Subscription subscription = Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> subscriber) {
				Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment call subscriber.onNext keepon:");
				subscriber.onNext("keepon");
			}
		})
				//一层一层层往上订阅,这里订阅是在新线程，下面unsubscribe会把往上的线程取消掉
				.subscribeOn(Schedulers.newThread())
				.subscribe(new Action1<String>() {
					@Override
					public void call(String s) {

						Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment call:"+s);
					}
				});

		//默认subscription 调用完
		Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment unsubscribe:" + subscription.isUnsubscribed());
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		subscription.unsubscribe();
		Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment unsubscribe:" + subscription.isUnsubscribed());
	}


	//去抖动，一段时间后只发送一个数据(其实是这段时间发送的最后一个数据)
	private void debounce() {
		Observable.create(new Observable.OnSubscribe<AppInfo>() {

			@Override
			public void call(Subscriber<? super AppInfo> subscriber) {
				for (int i = 0; i < mApps.size(); i++) {
					Log.w("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment call onNext 发送:"+mApps.get(i));
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
	//sample是采样，时间到了，没有取消订阅，会发送最后一个（如果这段时间内被观察者没发送数据，观察者就不会收到数据）
	private void sample() {
		count = 0;
		Observable.create(new Observable.OnSubscribe<AppInfo>() {

			@Override
			public void call(Subscriber<? super AppInfo> subscriber) {
				for (int i = 0; i < mApps.size(); i++) {
					Log.w("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment  sample call onNext:"+mApps.get(i));
					subscriber.onNext(mApps.get(i));
					if(i == 3){
						SystemClock.sleep(1000);
					}
				}
//				subscriber.onCompleted();
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
						Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment sample onNext:" + appInfo.getName());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}

	//第一个数据会发送，时间到了，会发送接下来接收到的最近的数据，如果这时没数据，就不发送
	private void throttleFirst() {
		count = 0;
		Observable.create(new Observable.OnSubscribe<AppInfo>() {

			@Override
			public void call(Subscriber<? super AppInfo> subscriber) {
				for (int i = 0; i < mApps.size(); i++) {
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
						Log.e("TAG", "FilterFirstSingleSampleTimeoutDebounceFragment  throttleFirst onNext 收到:" + appInfo.getName()+ " "+System.currentTimeMillis());
						mAddedApps.add(appInfo);
						mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
					}
				});
	}

	//到了指定索引才发送数据，接着发送onCompleted
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

	//skip(3)指定跳过数量后，才真正发送数据
	//skipLast(3) 先入队列，队列的数量等于要跳过的数量时，还有数据发送过来，把队列的第一个发送出去
	private void skip() {
		Observable.from(mApps)
				//跟take,takeLast相对应
//				.skip(3)
				.skipLast(2)
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
