package com.zhang.rxjava1;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.zhang.rxjava1.bean.Student;

import java.util.ArrayList;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class Rx1Sample2Activity extends AppCompatActivity {
	private static final String tag = "Rx1Sample2Activity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rx_java1_sample);
	}
	// 创建被观察者
	Observable<String> observable = Observable.create(new Observable.OnSubscribe<String>() {
		@Override
		public void call(Subscriber<? super String> subscriber) {
			Log.d(tag, "发射事件1 -- " + Thread.currentThread().getName());
			subscriber.onNext("item 1");
			Log.d(tag, "发射事件2 -- " + Thread.currentThread().getName());
			subscriber.onNext("item 2");
			Log.d(tag, "发射事件3 -- " + Thread.currentThread().getName());
			subscriber.onNext("item 3");
			Log.d(tag, "发射事件4 -- " + Thread.currentThread().getName());
			subscriber.onNext("item 4");
		}
	});

	// 创建观察者
	Observer<String> observer = new Observer<String>() {


		@Override
		public void onNext(String s) {
			Log.d(tag, "接受事件 : " + s + " -- " + Thread.currentThread().getName());
		}

		@Override
		public void onCompleted() {

		}

		@Override
		public void onError(Throwable e) {
		}


	};

	public void flatMap4(View view) {
		//不切线程
//		observable.subscribe(observer);
		//切换线程
		observable
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(observer);

		observable
				.flatMap(new Func1<String, Observable<String>>() {
					@Override
					public Observable<String> call(String s) {
						final String str = s;
						return  Observable.create(new Observable.OnSubscribe<String>() {
							@Override
							public void call(Subscriber<? super String> subscriber) {
								Log.d(tag, "发射 " + str + "-sub -- " + Thread.currentThread().getName());
								subscriber.onNext(str + "-sub");
							}
						});
					}
				})
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(observer);
	}

	private Observable<Student> getDataFromLocal(){
        Observable<Student> studentObservable = Observable.create(new Observable.OnSubscribe<Student>() {
            @Override
            public void call(Subscriber<? super Student> subscriber) {
                try {
                    Log.e("TAG", "Rx1Sample2Activity call 开始加载本地数据:"+System.currentTimeMillis());
                    Student keepon_from_net = new Student("keepon from local");
                    Thread.sleep(600);
                    subscriber.onNext(keepon_from_net);
                    subscriber.onCompleted();
                    Log.e("TAG", "Rx1Sample2Activity call 加载本地数据结束:"+System.currentTimeMillis());
                }catch (Exception e){

                }

            }
        });
        return  studentObservable.subscribeOn(Schedulers.io());
	}
	private Observable <Student> getDataFromLocal2(){
		ArrayList<Student> students = new ArrayList<>();
		students.add(new Student("zhangsan"));
		students.add(new Student("keepon"));
		Observable<Student> from = Observable.from(students);
		return  from;
	}
	private Observable <Student> getDataFromNet2(){
		ArrayList<Student> students = new ArrayList<>();
		students.add(new Student("zhangsan2"));
		students.add(new Student("keepon2"));
		Observable<Student> from = Observable.from(students);
		return  from;
	}

	//从网络拿数据
	private Observable<Student> getDataFromNet(){
		Observable<Student> studentObservable = Observable.create(new Observable.OnSubscribe<Student>() {
			@Override
			public void call(Subscriber<? super Student> subscriber) {
			    try {

                    Log.e("TAG", "Rx1Sample2Activity call 开始加载网络数据:"+System.currentTimeMillis());
//                     Student keepon_from_net = new Student("keepon from net");
                    Student keepon_from_net = new Student("");
                    Thread.sleep(500);
                    subscriber.onNext(keepon_from_net);
                    subscriber.onCompleted();
                    Log.e("TAG", "Rx1Sample2Activity call 加载网络数据结束:"+System.currentTimeMillis());
                }catch (Exception e){

                }

            }
		});
		return  studentObservable.subscribeOn(Schedulers.io());
	}

	private static final String TAG = "Rx1Sample2Activity";
	public void merge(View view) {
		//merge 合并多个Observable的发射物
		//merge括号里是一个Observable(OnSubscribeFromIterable)，merge会用到lift，又会new一个Observable,
		Observable.merge(getDataFromLocal(),getDataFromNet())
				.subscribe(new Observer<Student>() {
					@Override
					public void onCompleted() {

					}

					@Override
					public void onError(Throwable e) {

					}

					@Override
					public void onNext(Student student) {
						Log.e(TAG, " merge onNext:" + student);
					}
				});
	}
	public void merge2(View view) {
		//merge 合并多个Observable的发射物
		//merge括号里是一个Observable(OnSubscribeFromIterable)，merge会用到lift，又会new一个Observable,
		//merge里面的元素会通过from，变成要发射的Observable
		Observable.merge(getDataFromNet(),getDataFromLocal2())
				.subscribe(new Observer<Student>() {
					@Override
					public void onCompleted() {

					}

					@Override
					public void onError(Throwable e) {
                        Log.e("TAG", "Rx1Sample2Activity onError:");
					}

					@Override
					public void onNext(Student student) {
						Log.e("TAG", "Rx1Sample2Activity merge onNext:" + student);
					}
				});
	}

	public void concat(View view) {
		//merge 合并多个Observable的发射物
		//merge括号里是一个Observable(OnSubscribeFromIterable)，merge会用到lift，又会new一个Observable,
		//merge里面的元素会通过from，变成要发射的Observable
		Observable.concat(getDataFromNet(),getDataFromLocal())
                //filter(predicate).take(1) 发送满足条件一个时，会解除绑定
                //  if (++count >= limit) {
                //    completed = true;
                //   }
                // if (completed) {
                //           child.onCompleted();
                //           unsubscribe();
                //  }
                .first(new Func1<Student, Boolean>() {
                    @Override
                    public Boolean call(Student student) {
                        Log.e("TAG", "Rx1Sample2Activity concat call first -------:");
                        if(student!=null&&!TextUtils.isEmpty(student.getName())){
                            return true;
                        }
                        return false;
                    }
                })
				.subscribe(new Observer<Student>() {
					@Override
					public void onCompleted() {

					}

					@Override
					public void onError(Throwable e) {
                        Log.e("TAG", "Rx1Sample2Activity concat onError:");
					}

					@Override
					public void onNext(Student student) {
						Log.e("TAG", " Rx1Sample2Activityconcat concat onNext:" + student);
					}
				});
	}

    public void concatEager (View view) {
        //merge 合并多个Observable的发射物
        //merge括号里是一个Observable(OnSubscribeFromIterable)，merge会用到lift，又会new一个Observable,
        //merge里面的元素会通过from，变成要发射的Observable
        Observable.merge(getDataFromNet(),getDataFromLocal())
                //filter(predicate).take(1) 发送满足条件一个时，会解除绑定
                //  if (++count >= limit) {
                //    completed = true;
                //   }
                // if (completed) {
                //           child.onCompleted();
                //           unsubscribe();
                //  }
                .subscribe(new Observer<Student>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("TAG", "Rx1Sample2Activity concatEager onError:");
                    }

                    @Override
                    public void onNext(Student student) {
                        Log.e(TAG, "Rx1Sample2Activity  concatEager  onNext:" + student);
                    }
                });
    }
}
