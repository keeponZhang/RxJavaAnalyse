package com.packtpub.apps.rxjava_essentials.chapter5;


import android.app.Fragment;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;


public class GroupByConcatExampleFragment extends Fragment {

	@BindView(R.id.fragment_first_example_list)
	RecyclerView mRecyclerView;

	@BindView(R.id.fragment_first_example_swipe_container)
	SwipeRefreshLayout mSwipeRefreshLayout;
	@BindView(R.id.concat)
	Button             mConcat;

	Unbinder unbinder;

	private ApplicationAdapter mAdapter;

	private ArrayList<AppInfo> mAddedApps = new ArrayList<>();
	private List<AppInfo> mApps;

	public GroupByConcatExampleFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_example_groupby_concat, container, false);
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
		mRecyclerView.setVisibility(View.GONE);

		mApps = ApplicationsList.getInstance().getList();
		mRecyclerView.setVisibility(View.VISIBLE);

	}

	private void loadList(List<AppInfo> apps) {



	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@OnClick({R.id.concat,R.id.concat2, R.id.groupBy, R.id.groupBy2,R.id.groupBy3})
	public void onViewClicked(View view) {
		switch (view.getId()) {
			case R.id.concat:
				concat();
				break;
			case R.id.concat2:
			    concat2();
				break;
			case R.id.groupBy:
				groupBy();
				break;
			case R.id.groupBy2:
				groupBy2();
				break;
			case R.id.groupBy3:
				groupBy3();
				break;
		}
	}

	private void groupBy3() {
		Observable<GroupedObservable<String, String>> groupedObservableObservable = getGroupedObservableObservable();
		Observable.concat(groupedObservableObservable).subscribe(new Subscriber<String>() {
			@Override
			public void onCompleted() {

			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(String s) {
				Log.e("TAG", "GroupByConcatExampleFragment 收到 onNext:"+s);
			}
		});
	}

	private void groupBy() {
		Observable<GroupedObservable<String, AppInfo>> groupedItems = Observable.from(mApps)
				.groupBy(new Func1<AppInfo, String>() {
					@Override
					public String call(AppInfo appInfo) {
						SimpleDateFormat formatter = new SimpleDateFormat("MM/yyyy");
						return formatter.format(new Date(appInfo.getLastUpdateTime()));
					}
				});

		Observable
				.concat(groupedItems)
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
	private  int i = 0;
	private void groupBy2() {
		i = 0;
		Observable<GroupedObservable<String, String>> groupedObservableObservable = getGroupedObservableObservable();
		groupedObservableObservable.subscribe(new Subscriber<GroupedObservable<String, String>>() {
			@Override
			public void onCompleted() {
				
			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(GroupedObservable<String, String> stringStringGroupedObservable) {
				Log.e("TAG", "GroupByConcatExampleFragment onNext:"+stringStringGroupedObservable);

				if(i < 4){
					stringStringGroupedObservable.subscribe(new Subscriber<String>() {
						@Override
						public void onCompleted() {

						}

						@Override
						public void onError(Throwable e) {

						}

						@Override
						public void onNext(String s) {
							Log.e("TAG", "GroupByConcatExampleFragment 收到 onNext:" + s);
						}
					});
				}

				i++;
			}
		});

	}

	private Observable<GroupedObservable<String, String>> getGroupedObservableObservable() {
		return Observable.create(new Observable.OnSubscribe<String>() {
				@Override
				public void call(Subscriber<? super String> subscriber) {
					for (int i = 1; i < 5; i++) {
						for (int j = 1; j < 2; j++) {
							String s = i + "班;我是学员编号：" + j;
							Log.w("TAG", "GroupByConcatExampleFragment call 发送:"+s);
							subscriber.onNext(s);
						}
					}
					subscriber.onNext("1班;我是学员编号：100");
					//没有onCompleted只有一组,如果使用concat的话
	//    			subscriber.onCompleted();

				}
			}).groupBy(new Func1<String, String>() {
				@Override
				public String call(String s) {

					return s.substring(0,2);
	//				return s;
				}
			});
	}

	private void concat2() {
        Observable<String> stringObservable = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
//                subscriber.onError(new RuntimeException("keepon"));
                subscriber.onNext("10");
                subscriber.onNext("11");
                subscriber.onNext("12");
                //没有onCompleted，stringObservable2发送的是收不到的
                subscriber.onCompleted();

            }
        });
        Observable<String> stringObservable2 = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                subscriber.onNext("20");
                subscriber.onNext("21");
                subscriber.onNext("22");
                subscriber.onCompleted();
            }
        });
        Observable.concat(stringObservable, stringObservable2)
                .first(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return !TextUtils.isEmpty(s)&&s.startsWith("3");
                    }
                }).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                Log.e("TAG", "GroupByConcatExampleFragment onCompleted:");
            }

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "GroupByConcatExampleFragment onError:"+e.getMessage());
            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "GroupByConcatExampleFragment onNext:"+s);
            }
        });
    }
    //先发送第一个Observable，后发送第二个Observable(即使不同线程)
    private void concat() {
		AppInfo appInfo = mApps.get(0);
		AppInfo appInfo2 = mApps.get(2);
		Observable<AppInfo> appInfoObservable = Observable.create(new Observable.OnSubscribe<AppInfo>() {
			@Override
			public void call(Subscriber<? super AppInfo> subscriber) {
//				subscriber.onNext( null);
				subscriber.onNext(new AppInfo("zhang","zhangIcon",System.currentTimeMillis()) );
				subscriber.onNext(new AppInfo("zhang2","zhangIcon2",System.currentTimeMillis()) );
				Log.w("TAG", "OperatorConcat  GroupByConcatExampleFragment appInfoObservable call onNext:");
				SystemClock.sleep(5000);
				subscriber.onCompleted();
				Log.e("TAG", "OperatorConcat GroupByConcatExampleFragment call onCompleted -------》》》:");

			}
		});
		Observable<AppInfo> just = Observable.create(new Observable.OnSubscribe<AppInfo>() {
			@Override
			public void call(Subscriber<? super AppInfo> subscriber) {
				subscriber.onNext(new AppInfo("keeon","keepOnIcon",System.currentTimeMillis()) );
				Log.w("TAG", "OperatorConcat GroupByConcatExampleFragment   just call onNext:");
				subscriber.onCompleted();
			}
		}).subscribeOn(Schedulers.io());
		Observable<AppInfo> just2 = Observable.just(appInfo2);
		Observable.concat(appInfoObservable,just)
//				.first(new Func1<AppInfo, Boolean>() {
//			@Override
//			public Boolean call(AppInfo appInfo) {
//				if(appInfo!=null&&appInfo.getName().length()!=5){
//					Log.e("TAG", "GroupByConcatExampleFragment call:" + appInfo.getName().length());
//					return true;
//				}
//				return false;
//			}
//		})
				.subscribe(new Observer<AppInfo>() {
			@Override
			public void onCompleted() {
				Log.e("TAG", "GroupByConcatExampleFragment onCompleted:");
			}

			@Override
			public void onError(Throwable e) {
				Log.e("TAG", "GroupByConcatExampleFragment onError:");
			}

			@Override
			public void onNext(AppInfo appInfo) {
				Log.e("TAG", "OperatorConcat GroupByConcatExampleFragment concat onNext:"+appInfo.getName());
			}
		});
	}
}
