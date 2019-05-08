package com.demo.lizejun.rxsample.simple;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.demo.lizejun.rxsample.R;
import com.demo.lizejun.rxsample.simple.bean.Course;
import com.demo.lizejun.rxsample.simple.bean.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.subjects.AsyncSubject;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;


public class SimpleActivity extends AppCompatActivity {

	private int i;
	private Disposable mDisposable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple);
	}
	private Disposable mDisposable2;
	private static final String TAG = "SimpleActivity";
	Observer<String> observer = new Observer<String>() {

		private Disposable mD;

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
	public void observer(View view) {
		i=0;
//	getObservable().subscribeOn(Schedulers.io()).doOnSubscribe(new Consumer<Disposable>() {
//		@Override
//		public void accept(Disposable disposable) throws Exception {
//			Log.e(TAG, "accept disposable:"+disposable);
//		}
//	}).subscribe(observer);

		getObservable()
				.doOnSubscribe(new Consumer<Disposable>() {
			@Override
			//doOnSubscribe会创建DisposableLambdaObserver，DisposableLambdaObserver的onSubscribe方调用了Consumer.accept
			public void accept(Disposable disposable) throws Exception {
				mDisposable2 = disposable;
				Log.e(TAG, "accept disposable:"+disposable.getClass().getName());
//				mDisposable2.dispose();
			}
		}).subscribe(observer);

//	getObservable().subscribe(observer);
//		getObservableJust().subscribe(observer);
//		getObservableFrom().subscribe(observer);
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
	private Observable<String> getObservable() {
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
//	AsyncSubject:Observer会接收AsyncSubject的onComplete()之前的最后一个数据。
	public void asyncSubject(View view) {
		AsyncSubject<String> subject = AsyncSubject.create();
		subject.onNext("asyncSubject1");
		subject.onNext("asyncSubject2");
//		subject.onComplete()必须要调用才会开始发送数据，否则Subscriber将不接收任何数据
//		subject.onComplete();
		subject.subscribe(new Consumer<String>() {
			@Override
			public void accept(@NonNull String s) throws Exception {
				Log.e(TAG,"asyncSubject:" + s);
			}
		}, new Consumer<Throwable>() {
			@Override
			public void accept(@NonNull Throwable throwable) throws Exception {
				Log.e(TAG,"asyncSubject onError");
				  //不输出（异常才会输出）
			}
		}, new Action() {
			@Override
			public void run() throws Exception {
				Log.e(TAG,"asyncSubject:complete");
			}
		});

		subject.onNext("asyncSubject3");
		subject.onComplete();
		subject.onNext("asyncSubject4");
	}

	public void behaviorSubject(View view) {
		BehaviorSubject<String> subject = BehaviorSubject.createDefault("behaviorSubject1");
		subject.onNext("behaviorSubject2");

		subject.subscribe(new Consumer<String>() {
			@Override
			public void accept(@NonNull String s) throws Exception {
				Log.e("TAG", "behaviorSubject:" +s);
			 //输出asyncSubject:asyncSubject3
			}
		}, new Consumer<Throwable>() {
			@Override
			public void accept(@NonNull Throwable throwable) throws Exception {
				Log.e("TAG", "behaviorSubject onError" );
				 //不输出（异常才会输出）
			}
		}, new Action() {
			@Override
			public void run() throws Exception {
				Log.e("TAG", "behaviorSubject:complete:" );
			 //输出 behaviorSubject onComplete
			}
		});

		subject.onNext("behaviorSubject3");
		subject.onNext("behaviorSubject4");
	}
// PublishSubject:Observer只接收PublishSubject被订阅之后发送的数据。
	public void publishSubject(View view) {
		PublishSubject<String> subject = PublishSubject.create();
		subject.onNext("publicSubject1");
		subject.onNext("publicSubject2");
		subject.onComplete();

		subject.subscribe(new Consumer<String>() {
			@Override
			public void accept(@NonNull String s) throws Exception {
				Log.e(TAG, "publicSubject:"+s);
			}
		}, new Consumer<Throwable>() {
			@Override
			public void accept(@NonNull Throwable throwable) throws Exception {
				Log.e(TAG, "publicSubject onError:" );
			  //不输出（异常才会输出）
			}
		}, new Action() {
			@Override
			public void run() throws Exception {
				Log.e(TAG, "publicSubject:complete" );
			  //输出 publicSubject onComplete
			}
		});

		subject.onNext("publicSubject3");
		subject.onNext("publicSubject4");
	}

	public void replaySubject(View view) {
	}

	public void doOnNext(View view) {
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
		}).doOnNext(new Consumer<String>() {
			@Override
			public void accept(String s) throws Exception {

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
