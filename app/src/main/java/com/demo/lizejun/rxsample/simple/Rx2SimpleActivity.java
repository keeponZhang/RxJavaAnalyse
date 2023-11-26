package com.demo.lizejun.rxsample.simple;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.demo.lizejun.rxsample.R;
import com.demo.lizejun.rxsample.ZeMaoRx2Activity;
import com.demo.lizejun.rxsample.simple.bean.Course;
import com.demo.lizejun.rxsample.simple.bean.Student;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.AsyncSubject;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;


public class Rx2SimpleActivity extends AppCompatActivity {

	private int i;
	private Disposable mDisposable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple);
	}
	public void zemaoSample(View view) {
		Intent intent = new Intent(this,ZeMaoRx2Activity.class);
		startActivity(intent);
	}
	private Disposable mDisposable2;
	private static final String TAG = "Rx2SimpleActivity";
	Observer<String> observer = new Observer<String>() {


		@Override
		public void onComplete() {
			Log.e(TAG, "onCompleted");
			i = 0;
		}

		@Override
		public void onError(Throwable e) {
			Log.e(TAG, "onError");
			i = 0;
		}
		@Override
		public void onSubscribe(Disposable disposable) {
			//如果rxjava中间又创建了observer,这个是它上一级中observer的onSubsribe回调下来的
			mDisposable = disposable;
			//	我们可以把它理解成两根管道之间的一个机关, 当调用它的dispose()方法时, 它就会将两根管道切断, 从而导致下游收不到事件.
//	调用dispose()并不会导致上游不再继续发送事件, 上游会继续发送剩余的事件.
			//disposable参数是由上一级的Observer的onSubsrcibe方法里决定的（ObservableCreate是直接调用）
			Log.e(TAG, "observer onSubscribe disposable:"+disposable.getClass().getName());
		}

		@Override
		public void onNext(String s) {
			i++;
			if(i==1){
				mDisposable2.dispose();
			}

			if (i == 2) {
				Log.d(TAG, "onNext dispose："+mDisposable );
				mDisposable.dispose();
				Log.d(TAG, "isDisposed : " + mDisposable.isDisposed());
			}
			Log.e(TAG, "onNext:"+s);
		}
	};
	//doOnSubscribe所处线程是所处订阅的线程
	public void doOnSubscribe(View view) {
		i=0;
//	getObservableCreate().subscribeOn(Schedulers.io()).doOnSubscribe(new Consumer<Disposable>() {
//		@Override
//		public void accept(Disposable disposable) throws Exception {
//			Log.e(TAG, "accept disposable:"+disposable);
//		}
//	}).subscribe(observer);

//		getObservableCreate()


		// tes2();
		tes3();

// 		Observable.just("Keepon")
// 				.subscribeOn(Schedulers.io())
// 				.doOnSubscribe(new Consumer<Disposable>() {
// 			@Override
// 			//doOnSubscribe会创建DisposableLambdaObserver，DisposableLambdaObserver的onSubscribe方调用了Consumer.accept
// 			public void accept(Disposable disposable) throws Exception {
// 				Log.e("TAG", "Rx2SimpleActivity accept:" + Thread.currentThread().getName());
// 				mDisposable2 = disposable;
// 				Log.e(TAG, "accept disposable:"+disposable.getClass().getName());
// //				mDisposable2.dispose();
// 			}
// 		}).subscribeOn(Schedulers.newThread()).observeOn(Schedulers.computation()).doOnSubscribe(new Consumer<Disposable>() {
// 			@Override
// 			public void accept(Disposable disposable) throws Exception {
// 				Log.e("TAG", "Rx2SimpleActivity accept doOnSubscribe2:"+Thread.currentThread().getName());
// 			}
// 		}).subscribe(observer);

//	getObservableCreate().subscribe(observer);
//		getObservableJust().subscribe(observer);
//		getObservableFrom().subscribe(observer);
	}

	private void tes2() {
		Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> e) throws Exception {
				e.onNext("1");
				e.onNext("2");
				e.onNext("3");
				Log.e("TAG", "Rx2SimpleActivity subscribe e.onNext:" );
			}//delay其实上游已经发射了
		}).
				delay(5, TimeUnit.SECONDS)
				.subscribe(new Observer<String>() {
			@Override
			public void onSubscribe(Disposable d) {
				mDisposable2 = d;
			}

			@Override
			public void onNext(String value) {
				Log.e("TAG", "Rx2SimpleActivity onNext:"+value );
			}

			@Override
			public void onError(Throwable e) {

			}

			@Override
			public void onComplete() {

			}
		});
	}
	private void tes3() {
		Single.just("1")
				.delay(5, TimeUnit.SECONDS)
				.subscribe(new SingleObserver<String>() {
					@Override
					public void onSubscribe(Disposable d) {
						//这个是会变的
						Log.e("TAG", "Rx2SimpleActivity onSubscribe disposable:"+d );
						mDisposable2 = d;
					}

					@Override
					public void onSuccess(String value) {
						Log.e("TAG", "Rx2SimpleActivity onNext:"+value );
					}

					@Override
					public void onError(Throwable e) {

					}


				});
		SystemClock.sleep(100);
		Log.e("TAG", "--------Rx2SimpleActivity tes3 mDisposable2:"+mDisposable2 );
	}


	private Observable<String> getObservableJust() {
		//数量大于1的just会封装成fromArray
		Observable observable = Observable.just("Hello", "Hi", "Aloha");
		return observable;
	}
	private Observable<String> getObservableFrom() {
		String[] words = {"Hello", "Hi", "Aloha"};
		Observable observable = Observable.fromArray(words);
		return  observable;
	}
	private Observable<String> getObservableCreate() {
		//ObservableCreate不是在subscribeActual直接发数据，因为这时数据不知道，而是有套了一层ObservableOnSubscribe
		// ObservableJust是在subscribeActual发送数据的，因为ObservableJust持有要发送的数据
		return Observable.create(new ObservableOnSubscribe<String>() {
				//ObservableEmitter： Emitter是发射器的意思，那就很好猜了，这个就是用来发出事件的，
				// 它可以发出三种类型的事件，通过调用emitter的onNext(T value)、onComplete()和onError(Throwable error)就可以分别发出next事件、complete事件和error事件。
			@Override
			public void subscribe(ObservableEmitter<String> e) throws Exception {
				Log.d(TAG, "subscribe: 发送 Hello World");
				e.onNext("Hello World");
				//最为关键的是onComplete和onError必须唯一并且互斥, 即不能发多个onComplete, 也不能发多个onError, 也不能先发一个onComplete, 然后再发一个onError, 反之亦然(经测试，可以发多个onComplete,也可以先发onError再发onComplete)
//				e.onComplete();
//				当上游发送了一个onComplete后, 上游onComplete之后的事件将会继续发送, 而下游收到onComplete事件之后将不再继续接收事件.
//				e.onComplete();
//				当上游发送了一个onError后, 上游onError之后的事件将继续发送, 而下游收到onError事件之后将不再继续接收事件.
//	  			e.onError(new RuntimeException("发送错误"));
//				e.onError(new RuntimeException("发送错误2"));
				e.onNext("Hello World 2");
				e.onNext("Hello World 3");
				Log.d(TAG, "subscribe: 发送 Hello World 2");
				e.onNext("Hello World 3");
				Log.d(TAG, "subscribe: 发送 Hello World 3");
				Log.d(TAG, "subscribe: 发送 end");
			}
		});
	}
	public void map(View view) {
		Observable.create(new ObservableOnSubscribe<Integer>() {
			@Override
			public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
				Log.d(TAG, "flatMap onNext1:");
				emitter.onNext(1);
				//				Log.d(TAG, "flatMap onNext2:");
				//				emitter.onNext(2);
				//				Log.d(TAG, "flatMap onNext3:");
				//				emitter.onNext(3);
			}
		}).map(new Function<Integer, String>() {
			@Override
			public String apply(Integer integer) throws Exception {

				Log.d(TAG, "map apply:"+integer);
				return "map转换："+integer;
			}
		}).doOnSubscribe(new Consumer<Disposable>() {
			@Override
			public void accept(Disposable disposable) throws Exception {

			}
		}).subscribe(new Consumer<String>() {
			@Override
			public void accept(String s) throws Exception {
				Log.e(TAG, s);
			}
		});
	}
	public void flatMap(View view) {
		Observable.create(new ObservableOnSubscribe<Integer>() {
			@Override
			public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
				Log.d(TAG, "flatMap onNext1:");
				emitter.onNext(1);
//				Log.d(TAG, "flatMap onNext2:");
				emitter.onNext(2);
//				Log.d(TAG, "flatMap onNext3:");
//				emitter.onNext(3);
//				emitter.onNext(4);
//				emitter.onNext(5);
//				emitter.onNext(6);
			}
		}).flatMap(new Function<Integer, ObservableSource<String>>() {
			@Override
			public ObservableSource<String> apply(Integer integer) throws Exception {
				final List<String> list = new ArrayList<>();
				for (int i = 0; i < 3; i++) {
					list.add("I am value 父：" + integer + " 子："+i);
				}
				Log.d(TAG, "flatMap apply:"+integer);
				return Observable.fromIterable(list);
			}
		}).subscribe(new Consumer<String>() {
			@Override
			public void accept(String s) throws Exception {
				Log.e(TAG, s);
			}
		});
	}


	public void flatMap3(View view) {
		Observable.create(new ObservableOnSubscribe<Integer>() {
			@Override
			public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
				Log.d(TAG, "flatMap onNext1:");
				emitter.onNext(1);
				//				Log.d(TAG, "flatMap onNext2:");
				//				emitter.onNext(2);
				//				Log.d(TAG, "flatMap onNext3:");
				//				emitter.onNext(3);
			}
		}).flatMap(new Function<Integer, ObservableSource<String>>() {
			@Override
			public ObservableSource<String> apply(Integer integer) throws Exception {

				Log.d(TAG, "flatMap apply:"+integer);
				return Observable.just("flatmap转换："+integer);
			}
		}).subscribe(new Consumer<String>() {
			@Override
			public void accept(String s) throws Exception {
				Log.e(TAG, s);
			}
		});
	}
	public void flatMap2(View view) {
		List<Student> students = new ArrayList<Student>();
		students.add(new Student("1", "zhangsan")
				.addCourse(new Course("10", "数学"))
				.addCourse(new Course("11", "计算机")));
		students.add(new Student("2", "lisi")
				.addCourse(new Course("20", "语文")));
		students.add(new Student("3", "wangwu")
				.addCourse(new Course("30", "英语")));
		Observable.fromIterable(students)
				.flatMap(new Function<Student, ObservableSource<Course>>() {

					@Override
					public ObservableSource<Course> apply(Student student) throws Exception {
//						return Observable.fromIterable(student.getCourses());
						return Observable.fromIterable(student.getCourses()).delay((int) (Math.random() * 1000), TimeUnit.MILLISECONDS);
					}
				})
				.subscribe(new Consumer<Course>() {
					@Override
					public void accept(Course course) throws Exception {
						Log.e(TAG, "accept:"+course);
					}
				});
	}
	//concatMap即使延迟也能保持有序
	public void concatMap(View view) {
		List<Student> students = new ArrayList<Student>();
		students.add(new Student("1", "zhangsan")
				.addCourse(new Course("10", "数学"))
				.addCourse(new Course("11", "计算机")));
		students.add(new Student("2", "lisi")
				.addCourse(new Course("20", "语文")));
		students.add(new Student("3", "wangwu")
				.addCourse(new Course("30", "英语")));
		Observable.fromIterable(students)
				.concatMap(new Function<Student, ObservableSource<Course>>() {

					@Override
					public ObservableSource<Course> apply(Student student) throws Exception {
						//						return Observable.fromIterable(student.getCourses());
						return Observable.fromIterable(student.getCourses()).delay((int) (Math.random() * 1000), TimeUnit.MILLISECONDS);
					}
				})
				.subscribe(new Consumer<Course>() {
					@Override
					public void accept(Course course) throws Exception {
						Log.e(TAG, "accept:"+course);
					}
				});
	}
	public void concatMap2(View view) {
		Observable<Integer> sender = Observable.create(new ObservableOnSubscribe<Integer>() {
			@Override
			public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {

				// 发送多个数据

				e.onNext(1);
				e.onNext(2);
				e.onNext(3);

			}
		}).concatMap(new Function<Integer, ObservableSource<Integer>>() {
			@Override
			public ObservableSource<Integer> apply(@NonNull Integer integer) throws Exception {

				// 将多个数据合并

				List<Integer> list = new ArrayList<Integer>();

				list.add(integer);

				return Observable.fromIterable(list).delay((int) (Math.random() * 1000), TimeUnit.MILLISECONDS);    //使用fromIterable()，遍历集合
			}
		});


		// 用Consumer接收数据
		Consumer receiver = new Consumer<Integer>() {
			@Override
			public void accept(@NonNull Integer o) throws Exception {

				// 接收的数据是有序的
				Log.e(TAG,"receiver accept:  " + o);
			}
		};

		sender.subscribe(receiver);
	}
	public void flatMap4(View view) {
		final List<String> resultList = new CopyOnWriteArrayList<>();
		Observable.create(new ObservableOnSubscribe<String>() {
			                  @Override
			                  public void subscribe(ObservableEmitter<String> emitter) throws Exception {
				                  for (int i = 1; i < 6; i++) {
					                  emitter.onNext(String.valueOf(i));
				                  }
				                  emitter.onComplete();
			                  }
		                  }

		).flatMap(new Function<String, ObservableSource<String>>() {
			          @Override
			          public ObservableSource<String> apply(String s) throws Exception {
				          String tail = "";
				          ArrayList<String> newMap = new ArrayList<>();
				          for (int i = 0; i < 3; i++) {
					          tail = tail + "0";
					          newMap.add(s + tail);

				          }
				          return Observable.fromIterable(newMap).delay(50, TimeUnit.MILLISECONDS);			          }
		          }
		).doOnComplete(new Action() {
			@Override
			public void run() throws Exception {
				Log.d(TAG,resultList.toString());
			}
		}).subscribe(new Consumer<String>() {
			@Override
			public void accept(String s) throws Exception {
				resultList.add(s);
			}
		});

	}

//	Subject 发送onComplete事件后没有调用dispose();而CreateEmitter有
//	AsyncSubject:Observer会接收AsyncSubject的onComplete()之前的最后一个数据。
	public void asyncSubject(View view) {
		AsyncSubject<Integer> source = AsyncSubject.create();//AsyncSubject：不管在什么位置订阅，都只接接收到最后一条数据
// It will get only 4 and onComplete
		source.onNext(0);
		source.subscribe(getFirstObserver());
		source.onNext(1);
		source.onNext(2);
		source.onNext(3);
// It will also get only get 4 and onComplete
		source.subscribe(getSecondObserver());
		source.onNext(4);
		//不发送onComplete ，观察者收不到数据
		source.onComplete();
	}
	//BehaviorSubject：接收到订阅前的最后一条数据和订阅后的所有数据。
	public void behaviorSubject(View view) {
		BehaviorSubject<Integer> source = BehaviorSubject.create();
		source.onNext(-1);
		source.onNext(0);
// It will get 0,1, 2, 3, 4 and onComplete
		source.subscribe(getFirstObserver());
		source.onNext(1);
		source.onNext(2);
		source.onNext(3);
// It will get 3(last emitted)and 4(subsequent item) and onComplete
		source.subscribe(getSecondObserver());
		source.onNext(4);
		source.onComplete();
	}
// PublishSubject:Observer只接收PublishSubject被订阅之后发送的数据。
	public void publishSubject(View view) {
		PublishSubject<Integer> source = PublishSubject.create();//PublicSubject：接收到订阅之后的所有数据。
		source.onNext(0);
// It will get 1, 2, 3, 4 and onComplete
		source.subscribe(getFirstObserver());
		source.onNext(1);
		source.onNext(2);
		source.onNext(3);
		source.onComplete();
// It will get 4 and onComplete for second observer also.
		source.subscribe(getSecondObserver());
		source.onNext(4);
		source.onComplete();
	}
	//ReplaySubject：接收到所有的数据，包括订阅之前的所有数据和订阅之后的所有数据。
	public void replaySubject(View view) {
		ReplaySubject<Integer> source = ReplaySubject.create();
// It will get 1, 2, 3, 4
		source.onNext(1);
		source.subscribe(getFirstObserver());
		source.onNext(2);
		source.onNext(3);
		source.onNext(4);
		source.onComplete();
// It will also get 1, 2, 3, 4 as we have used replay Subject
		source.subscribe(getSecondObserver());
	}

	private Observer<? super Integer> getFirstObserver() {
		return new Observer<Integer>() {
			@Override
			public void onSubscribe(Disposable d) {

			}

			@Override
			public void onNext(Integer value) {
				Log.e("TAG", "Rx2SimpleActivity getFirstObserver onNext:"+value);
			}

			@Override
			public void onError(Throwable e) {
				Log.w("TAG", "Rx2SimpleActivity getFirstObserver onError:");
			}

			@Override
			public void onComplete() {
				Log.d("TAG", "Rx2SimpleActivity getFirstObserver onComplete:");
			}
		};
	}
	private Observer<? super Integer> getSecondObserver() {
		return new Observer<Integer>() {
			@Override
			public void onSubscribe(Disposable d) {

			}

			@Override
			public void onNext(Integer value) {
				Log.e("TAG", "Rx2SimpleActivity getSecondObserver onNext:"+value);
			}

			@Override
			public void onError(Throwable e) {
				Log.w("TAG", "Rx2SimpleActivity getSecondObserver onError:");
			}

			@Override
			public void onComplete() {
				Log.d("TAG", "Rx2SimpleActivity getSecondObserver onComplete:");
			}
		};
	}

	public void doOnNext(View view) {
		Observable.create(new ObservableOnSubscribe<Integer>() {
			@Override
			public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
				Log.d(TAG, "flatMap onNext1:"+Thread.currentThread().getName());
				emitter.onNext(1);
				//				Log.d(TAG, "flatMap onNext2:");
				//				emitter.onNext(2);
				//				Log.d(TAG, "flatMap onNext3:");
				//				emitter.onNext(3);
			}
		}).map(new Function<Integer, String>() {
			@Override
			public String apply(Integer integer) throws Exception {

				Log.d(TAG, "map apply:"+integer);
				return "map转换："+integer;
			}
		}).doOnNext(new Consumer<String>() {
			@Override
			public void accept(String s) throws Exception {
				Log.e("TAG", "Rx2SimpleActivity doOnNext accept:");
				s = s + "doOnNext";
			}
		}).subscribe(new Consumer<String>() {
			@Override
			public void accept(String s) throws Exception {
				Log.e(TAG, s);
			}
		});
	}

	public void zip(View view) {
		Observable<Integer> observable1 = Observable.create(new ObservableOnSubscribe<Integer>() {
			@Override
			public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
				Log.d(TAG, "emit 1");
				emitter.onNext(1);
				Log.d(TAG, "emit 2");
				emitter.onNext(2);
				Log.d(TAG, "emit 3");
				emitter.onNext(3);
				Log.d(TAG, "emit 4");
				emitter.onNext(4);
				Log.d(TAG, "emit complete1");
				emitter.onComplete();
			}
		});

		Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> emitter) throws Exception {
				Log.d(TAG, "emit A");
				emitter.onNext("A");
				Log.d(TAG, "emit B");
				emitter.onNext("B");
				Log.d(TAG, "emit C");
				emitter.onNext("C");
				Log.d(TAG, "emit complete2");
				emitter.onComplete();
			}
		});

		Observable.zip(observable1, observable2, new BiFunction<Integer, String, String>() {
			@Override
			public String apply(Integer integer, String s) throws Exception {
				return integer + s;
			}
		}).subscribe(new Observer<String>() {
			@Override
			public void onSubscribe(Disposable d) {
				Log.d(TAG, "onSubscribe");
			}

			@Override
			public void onNext(String value) {
				Log.d(TAG, "onNext: " + value);
			}

			@Override
			public void onError(Throwable e) {
				Log.d(TAG, "onError");
			}

			@Override
			public void onComplete() {
				Log.d(TAG, "onComplete");
			}
		});
	}

	public static void demo2() {
		Observable<Integer> observable1 = Observable.create(new ObservableOnSubscribe<Integer>() {
			@Override
			public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
				Log.d(TAG, "emit 1");
				emitter.onNext(1);
				Thread.sleep(1000);

				Log.d(TAG, "emit 2");
				emitter.onNext(2);
				Thread.sleep(1000);

				Log.d(TAG, "emit 3");
				emitter.onNext(3);
				Thread.sleep(1000);

				Log.d(TAG, "emit 4");
				emitter.onNext(4);
				Thread.sleep(1000);

				Log.d(TAG, "emit complete1");
				emitter.onComplete();
			}
		});

		Observable<String> observable2 = Observable.create(new ObservableOnSubscribe<String>() {
			@Override
			public void subscribe(ObservableEmitter<String> emitter) throws Exception {
				Log.d(TAG, "emit A");
				emitter.onNext("A");
				Thread.sleep(1000);

				Log.d(TAG, "emit B");
				emitter.onNext("B");
				Thread.sleep(1000);

				Log.d(TAG, "emit C");
				emitter.onNext("C");
				Thread.sleep(1000);

				Log.d(TAG, "emit complete2");
				emitter.onComplete();
			}
		});

		Observable.zip(observable1, observable2, new BiFunction<Integer, String, String>() {
			@Override
			public String apply(Integer integer, String s) throws Exception {
				return integer + s;
			}
		}).subscribe(new Observer<String>() {
			@Override
			public void onSubscribe(Disposable d) {
				Log.d(TAG, "onSubscribe");
			}

			@Override
			public void onNext(String value) {
				Log.d(TAG, "onNext: " + value);
			}

			@Override
			public void onError(Throwable e) {
				Log.d(TAG, "onError");
			}

			@Override
			public void onComplete() {
				Log.d(TAG, "onComplete");
			}
		});
	}
}
