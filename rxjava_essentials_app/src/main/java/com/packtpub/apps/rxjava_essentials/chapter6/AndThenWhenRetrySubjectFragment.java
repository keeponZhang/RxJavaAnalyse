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
import rx.joins.Pattern2;
import rx.joins.Plan0;
import rx.observables.JoinObservable;
import rx.subjects.AsyncSubject;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;


public class AndThenWhenRetrySubjectFragment extends Fragment {

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

	public AndThenWhenRetrySubjectFragment() {
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

		observable.retryWhen(new Func1<Observable<? extends Throwable>, Observable<?>>() {
			@Override
			public Observable<?> call(Observable<? extends Throwable> throwableObservable) {
				//这里可以发送新的被观察者 Observable
				return throwableObservable.flatMap(new Func1<Throwable, Observable<?>>() {
					@Override
					public Observable<?> call(Throwable throwable) {
						//如果发射的onError就终止
						return Observable.error(new Throwable("retryWhen终止啦"));
					}
				});

			}
		}).subscribe(new Subscriber<Integer>() {
			@Override
			public void onCompleted() {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment retryWhen onCompleted:");
			}

			@Override
			public void onError(Throwable e) {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment retryWhen  OnSubscribeRedo onError:");
			}

			@Override
			public void onNext(Integer value) {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment retryWhen OnSubscribeRedo onNext:" + value);
			}
		});
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
				Log.e("TAG", "AndThenWhenRetrySubjectFragment retry onCompleted:");
			}

			@Override
			public void onError(Throwable e) {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment retry OnSubscribeRedo onError:");
			}

			@Override
			public void onNext(Integer value) {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment  retry OnSubscribeRedo onNext:" + value);
			}
		});

	}

	private void andthenwhen() {
	}
//	相对比其他Subject常用，它的Observer只会接收到PublishSubject被订阅之后发送的数据
	private void publishSubject() {
		PublishSubject<String> publishSubject = PublishSubject.create();
		publishSubject.onNext("publishSubject1");
		publishSubject.onNext("publishSubject2");
		publishSubject.subscribe(new Observer<String>() {
			@Override
			public void onCompleted() {

			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(String o) {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment onNext:" +o);
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
				asyncSubject();
				break;
			case R.id.behaviorSubject:
				behaviorSubject();
				break;
			case R.id.publishSubject:
				publishSubject();
				break;
			case R.id.replaySubject:
				replaySubject();
				break;
		}
	}
//	ReplaySubject会发射所有数据给观察者，无论它们是何时订阅的。也有其它版本的ReplaySubject，在重放缓存增长到一定大小的时候或过了一段时间后会丢弃旧的数据
	private void replaySubject() {
		ReplaySubject replaySubject = ReplaySubject.create(); //创建默认初始缓存容量大小为16的ReplaySubject，当数据条目超过16会重新分配内存空间，使用这种方式，不论ReplaySubject何时被订阅，Observer都能接收到数据
		//replaySubject = ReplaySubject.create(100);//创建指定初始缓存容量大小为100的ReplaySubject
		//replaySubject = ReplaySubject.createWithSize(2);//只缓存订阅前最后发送的2条数据
		//replaySubject=ReplaySubject.createWithTime(1,TimeUnit.SECONDS,Schedulers.computation());  //replaySubject被订阅前的前1秒内发送的数据才能被接收
		replaySubject.onNext("replaySubject:pre1");
		replaySubject.onNext("replaySubject:pre2");
		replaySubject.onNext("replaySubject:pre3");
		replaySubject.subscribe(new Action1<String>() {
			@Override
			public void call(String s) {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment call:" +s);
			}
		});
		replaySubject.onNext("replaySubject:after1");
		replaySubject.onNext("replaySubject:after2");

	}
//	以上代码，Observer会接收到behaviorSubject2、behaviorSubject3、behaviorSubject4，如果在behaviorSubject.subscribe()之前不发送behaviorSubject1、behaviorSubject2，则Observer会先接收到default,再接收behaviorSubject3、behaviorSubject4。
	private void behaviorSubject() {
		BehaviorSubject behaviorSubject = BehaviorSubject.create("default");
		behaviorSubject.onNext("behaviorSubject1");
		behaviorSubject.onNext("behaviorSubject2");
		behaviorSubject.subscribe(new Observer<String>() {
			@Override
			public void onCompleted() {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment behaviorSubject onCompleted:" );
			}

			@Override
			public void onError(Throwable e) {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment behaviorSubject onError:" );
			}

			@Override
			public void onNext(String s) {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment behaviorSubject onNext:" );
			}
		});

		behaviorSubject.onNext("behaviorSubject3");
		behaviorSubject.onNext("behaviorSubject4");
	}
//	Observer只会接收asyncSubject的onCompleted()被调用前的最后一个数据，即“asyncSubject3”，如果不调用onCompleted()，Subscriber将不接收任何数据。
	private void asyncSubject() {
		AsyncSubject asyncSubject = AsyncSubject.create();
		asyncSubject.onNext("asyncSubject1");
		asyncSubject.onNext("asyncSubject2");
		asyncSubject.onNext("asyncSubject3");
//		asyncSubject.onNext(null);
//			asyncSubject.onCompleted();
		asyncSubject.onError(new Exception("keepon"));
		asyncSubject.subscribe(new Observer<String>() {
			@Override
			public void onCompleted() {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment asyncSubject onCompleted:" );
			}

			@Override
			public void onError(Throwable e) {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment asyncSubject onError:" );
			}

			@Override
			public void onNext(String s) {
				Log.e("TAG", "AndThenWhenRetrySubjectFragment asyncSubject onNext:" +s);
			}
		});
		asyncSubject.onNext("asyncSubject4");

	}
}
