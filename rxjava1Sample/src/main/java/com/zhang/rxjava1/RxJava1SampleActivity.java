package com.zhang.rxjava1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.zhang.rxjava1.bean.Class;
import com.zhang.rxjava1.bean.Group;
import com.zhang.rxjava1.bean.School;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class RxJava1SampleActivity extends AppCompatActivity {

	private ImageView mIv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mIv = (ImageView) findViewById(R.id.iv);
	}

	private static final String tag = "RxJava1SampleActivity";
	Observer<String> mObserver = new Observer<String>() {
		@Override
		public void onNext(String s) {
			Log.d(tag, "Item: " + s);
		}

		@Override
		public void onCompleted() {
			Log.d(tag, "Completed!");
		}

		@Override
		public void onError(Throwable e) {
			Log.d(tag, "Error!");
		}
	};

	Subscriber<String> subscriber        = new Subscriber<String>() {
		@Override
		public void onStart() {
			super.onStart();
			Log.d(tag, "onStart: ");
		}

		@Override
		public void onNext(String s) {
			Log.d(tag, "Item: " + s);
		}

		@Override
		public void onCompleted() {
			Log.d(tag, "Completed!");
		}

		@Override
		public void onError(Throwable e) {
			Log.d(tag, "Error!");
		}
	};
	Action1<String>    onNextAction      = new Action1<String>() {
		// onNext()
		@Override
		public void call(String s) {
			Log.d(tag, s);
		}
	};
	Action1<Throwable> onErrorAction     = new Action1<Throwable>() {
		// onError()
		@Override
		public void call(Throwable throwable) {
			// Error handling
		}
	};
	Action0            onCompletedAction = new Action0() {
		// onCompleted()
		@Override
		public void call() {
			Log.d(tag, "completed");
		}
	};

	public void observer(View view) {
		Observable observable;
		observable = getObservableCreat();
		Observer observer;
		//		observer= mObserver;
		//		observable.subscribe(observer);

		// 自动创建 Subscriber ，并使用 onNextAction 来定义 onNext()
		observable.subscribe(onNextAction);
		// 自动创建 Subscriber ，并使用 onNextAction 和 onErrorAction 来定义 onNext() 和 onError()
		observable.subscribe(onNextAction, onErrorAction);
		// 自动创建 Subscriber ，并使用 onNextAction、 onErrorAction 和 onCompletedAction 来定义 onNext()、 onError() 和 onCompleted()
		observable.subscribe(onNextAction, onErrorAction, onCompletedAction);
	}

	private Observable getObservabelJust() {
		Observable observable = Observable.just("Hello", "Hi", "Aloha");
		// 将会依次调用：
		// onNext("Hello");
		// onNext("Hi");
		// onNext("Aloha");
		// onCompleted();
		return observable;
	}

	private Observable getObservabelFrom() {
		String[] words = {"Hello", "Hi", "Aloha"};
		Observable observable = Observable.from(words);
		// 将会依次调用：
		// onNext("Hello");
		// onNext("Hi");
		// onNext("Aloha");
		// onCompleted();
		return observable;
	}

	private Observable getObservableCreat() {
		//OnSubscribe是一个接口，继承Action1
		Observable observable = Observable.create(new Observable.OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> subscriber) {
				subscriber.onNext("Hello");
				subscriber.onNext("Hi");
				subscriber.onNext("Aloha");
				subscriber.onCompleted();
			}
		});
		return observable;
	}

	public void observerJust(View view) {
		//just也是调的from,from创建用的是create(new OnSubscribeFromIterable<T>(iterable));
		//		Observable.just(1, 2, 3, 4)
		//				.subscribe(new Action1<Integer>() {
		//					@Override
		//					public void call(Integer number) {
		//						Log.d(tag, "number:" + number);
		//					}
		//				});
		//ScalarSynchronousObservable,ScalarSynchronousObservable构造方法会new一个OnSubscribe
		Observable.just(1)
				.subscribe(new Action1<Integer>() {
					@Override
					public void call(Integer number) {
						Log.d(tag, "number:" + number);
					}
				});
	}

	public void subscribeOnAndObserveOn(View view) {
		getObservableCreat()
				//会创建两个Observable
				.subscribeOn(Schedulers.io()) // 指定 subscribe() 发生在 IO 线程
				////会创建两个Observable
				.observeOn(AndroidSchedulers.mainThread()) // 指定 Subscriber 的回调发生在主线程
				.subscribe(new Action1<String>() {
					@Override
					public void call(String number) {
						Log.d(tag, "number:" + number + " " + Thread.currentThread().getName());
					}
				});
	}

	private static final String TAG = "RxJava1SampleActivity";

	public void map(View view) {
		Observable.just("http://ep.dzb.ciwong.com/rep/new/4055.jpg")// ObservableJust
				.map(new Func1<String, Bitmap>() {
					@Override
					public Bitmap call(String urlPath) {
						try {
							return BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

							//							Log.e(TAG, "subscribeMap map apply:" + Thread.currentThread().getName());
							//							URL url = new URL(urlPath);
							//							HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
							//							InputStream inputStream = null;
							//
							//							inputStream = urlConnection.getInputStream();
							//
							//							Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
							//							return bitmap;
						} catch (Exception e) {
							e.printStackTrace();
							return BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
						}
					}
				})
				.subscribe((new Action1<Bitmap>() {
					@Override
					public void call(final Bitmap bitmap) { // 参数类型 Bitmap
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								mIv.setImageBitmap(bitmap);
							}
						});

					}
				}));
	}

	public void flatMap(View view) {
		//
		Observable.from(new School().getClasses())
				//输入是Class类型，输出是ObservableSource<Group>类型
				.flatMap(new Func1<Class, Observable<Group>>() {
					@Override
					public Observable<Group> call(Class aClass) {
						Log.d(TAG, "flatMap call:" + aClass.toString());
						return Observable.from(aClass.getGroups());
					}
				}).subscribe(new Subscriber<Group>() {
			@Override
			public void onCompleted() {

			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(Group group) {
				Log.e(TAG, "flatMap onNext:" + group);
			}
		});
	}
	public void flatMap2(View view) {
		ArrayList<Integer> datas = new ArrayList<>();
		datas.add(1);
		datas.add(2);
		datas.add(3);
		//一共会创建4个Observable,1个ScalarSynchronousObservable，1个OnSubscribeFromIterable，2个lift生成的
		Observable.just(1)
				//输入是Class类型，输出是ObservableSource<Group>类型
				.flatMap(new Func1<Integer, Observable<String>>() {
					@Override
					public Observable<String> call(Integer integer) {
						Log.d(TAG, "flatMap call:" + integer);
						final List<String> list = new ArrayList<>();
						for (int i = 0; i < 3; i++) {
							list.add("I am value " + integer);
						}
						return Observable.from(list);
					}
				}).subscribe(new Subscriber<String>() {
			@Override
			public void onCompleted() {

			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onNext(String s) {
				Log.e(TAG, "flatMap onNext:" + s);
			}
		});
	}
}
