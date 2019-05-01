package com.zhang.rxjava1;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.zhang.rxjava1.bean.Student;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class RxJava1Sample2Activity extends AppCompatActivity {
	private static final String tag = "RxJava1Sample2Activity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rx_java1_sample2);
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
		return Observable.just(new Student("zhangsan"));


	}

	//从网络拿数据
	private Observable<Student> getDataFromNet(){
		return Observable.just(new Student("keepon"));
	}

	private static final String TAG = "RxJava1Sample2Activity";
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
}
