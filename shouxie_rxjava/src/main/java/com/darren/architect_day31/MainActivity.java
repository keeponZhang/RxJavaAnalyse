package com.darren.architect_day31;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity {
    private ImageView mImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImage = (ImageView) findViewById(R.id.image);


    }


//    private static final String TAG = "MainActivity";
    private static final String TAG = "TAG";
    public void subscribeOnlyJust(View view) {
//        subscribeOnlyJust();

        new Thread(new Runnable() {
            @Override
            public void run() {
                subscribeOnlyJust();
            }
        }).start();
    }

    private void subscribeOnlyJust() {
        //此时ObservableJust接收的observer是一个LambdaObserver
        //无线程切换，发送先传给你跟接收线程一样
        Observable.just("http://img.taopic.com/uploads/allimg/130331/240460-13033106243430.jpg")// ObservableJust
        .subscribe(new Consumer<String>() {
            @Override
            public void onNext(String item) throws Exception {
                Log.e(TAG,"subscribeOnlyJust onNext=="+item+"  " +Thread.currentThread().getName());
            }
        });
    }

    public void subscribeMapObserverSubscribeOn(View view) {
        // 1.观察者 Observable 被观察对象
        // Observer 观察者
        // subscribe 注册订阅
        Observable.just("http://img.taopic.com/uploads/allimg/130331/240460-13033106243430.jpg")// ObservableJust
                .map(new Function<String, Bitmap>() { // 事件变换 // ObservableMap  source -> ObservableJust
                    @Override
                    public Bitmap apply(@NonNull String urlPath) throws Exception {
                        Log.e("apply1", Thread.currentThread().getName());
                        URL url = new URL(urlPath);
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        InputStream inputStream = urlConnection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        return bitmap;
                    }
                })
                .subscribeOn(Schedulers.io())
                .map(new Function<Bitmap, Bitmap>() { // ObservableMap
                    @Override
                    public Bitmap apply(@NonNull Bitmap bitmap) throws Exception {
                        Log.e("apply2", Thread.currentThread().getName());

                        return bitmap;
                    }
                })
                .map(new Function<Bitmap, Bitmap>() {
                    @Override
                    public Bitmap apply(Bitmap bitmap) throws Exception {
                        Log.e("apply3", Thread.currentThread().getName());
                        return bitmap;
                    }
                })//subscribeOn是包裹，上面的在里面，下面的在外面，这个相当于主线程里面包裹io线程，所以上层的发成在io线程，所以连续两个subscribeOn，只有上面那个起作用
                .subscribeOn(Schedulers.mainThread())
                .observerOn(Schedulers.mainThread())
                .subscribe(new Consumer<Bitmap>() { // ObservableMap
                    @Override
                    public void onNext(Bitmap bitmap) throws Exception {
                        Log.e("onNext", Thread.currentThread().getName());// 子线程 or 主线程 ？ 1 2
                        mImage.setImageBitmap(bitmap);
                    }
                });
    }


    public void subscribeSubscribeOn(View view) {
        //这里Observable.just会产生一个Observable，subscribeOn也会产生一个Observable，订阅的发起点是从最后一个Observable发起的，一层一层往上订阅
        //ObservableJust的subscribeActual方法的参数是LambdaObserver（这里与下面的observer相同是因为subscribeOn只是做一个线程切换），
        // 即调用source.subscribe(observer)是只是把下层的observer直接往上传
        //ObservableJust发送的事件不经过ObservableSchedulers，因为ObservableSchedulers并没有新的Observer,ObservableJust持有的Obsrver就是底层最原始的Observer
        Observable.just("http://img.taopic.com/uploads/allimg/130331/240460-13033106243430.jpg")// ObservableJust
                //ObservableSchedulers的subscribeActual方法的参数是LambdaObserver
                .subscribeOn(Schedulers.io())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void onNext(String item) throws Exception {
                        Log.e(TAG,"subscribeSubscribeOn onNext=="+item+"  " +Thread.currentThread().getName());
                    }
                });
    }
    public void subscribeObserverOn(View view) {
        //ObservableJust的subscribeActual方法的参数是ObserverOnObservable$ObserverOnObserver,这个Observer是ObserverOnObservable通过静态代理LambdaObserver实现的
        Observable.just("http://img.taopic.com/uploads/allimg/130331/240460-13033106243430.jpg")// ObservableJust
                //ObserverOnObservable的subscribeActual方法的参数是LambdaObserver
                .observerOn(Schedulers.io())
                //下面接收事件的线程会改变（原理就是把发送事件放在一个runable中的run方法执行，
                //  切换到主线程中，就在runable中使用handler，切换到子线程中，就把该runable放到线程池中执行。
                .subscribe(new Consumer<String>() {
                    @Override
                    public void onNext(String item) throws Exception {
                        Log.e(TAG,"subscribeObserverOn onNext=="+item+"  " +Thread.currentThread().getName());
                    }
                });
    }

    public void subscribeSubscribeOnAndObserverOn(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //ObservableJust的subscribeActual方法的参数是ObserverOnObservable$ObserverOnObserver,这个Observer是ObserverOnObservable通过静态代理LambdaObserver实现的
                //ObservableJust发送在主线程
                Observable.just("http://img.taopic.com/uploads/allimg/130331/240460-13033106243430.jpg")// ObservableJust
                        //ObserverOnObservable的subscribeActual方法的参数是LambdaObserver
                        //ObservableSchedulers和ObservableJust订阅在主线程
                        .subscribeOn(Schedulers.mainThread())
                        //ObserverOnObservable订阅在子线程
                        .observerOn(Schedulers.io())
                        //下面接收事件的线程会改变（原理就是把发送事件放在一个runable中的run方法执行，
                        //  切换到主线程中，就在runable中使用handler，切换到子线程中，就把该runable放到线程池中执行。
                        //ObserverOnObservable接到ObservableJust发送的事件，把该事件发送给下层的observer时，进行了线程切换，所以ObserverOnObservable放事件是切换到了io线程，下面的观察者也io线程收到事件
                        .subscribe(new Consumer<String>() {
                            @Override
                            public void onNext(String item) throws Exception {
                                Log.e(TAG,"subscribeObserverOn onNext=="+item+"  " +Thread.currentThread().getName());
                            }
                        });
            }
        }).start();

    }

    public void subscribeSubscribeOnAndObserverOn2(View view) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //ObservableJust的subscribeActual方法的参数是ObserverOnObservable$ObserverOnObserver,这个Observer是ObserverOnObservable通过静态代理LambdaObserver实现的
                //ObservableJust发送在主线程
                Observable.just("http://img.taopic.com/uploads/allimg/130331/240460-13033106243430.jpg")// ObservableJust
                        //ObserverOnObservable的subscribeActual方法的参数是LambdaObserver
                        //ObservableSchedulers和ObservableJust订阅在主线程
                        .subscribeOn(Schedulers.mainThread())
                        //连续两个subscribeOn，后面那个subscribeOn只影响当前ObserverOnObservable的订阅线程
                        .subscribeOn(Schedulers.cpu())
                        .subscribe(new Consumer<String>() {
                            @Override
                            public void onNext(String item) throws Exception {
                                Log.e(TAG,"subscribeObserverOn onNext=="+item+"  " +Thread.currentThread().getName());
                            }
                        });
            }
        }).start();

    }
    public void subscribeSubscribeOnAndObserverOn3(View view) {
        //ObservableJust的subscribeActual方法的参数是ObservableMap$MapObserver,这个Observer是ObserverOnObservable通过静态代理LambdaObserver实现的
        // ObservableJust subscribeActual:IO线程
        //发送线程一般来说是最上层开始发送事件，即observer.onNext(item)调用方法所处的线程，如果整个调用链没有observerOn，下面层级的观察者也是处于这个线程，调用了observerOn，observerOn下面接收事件的对象所处的线程受observerOn指定的线程影响
        Observable.just("http://img.taopic.com/uploads/allimg/130331/240460-13033106243430.jpg")// ObservableJust
                // ObservableMap的subscribeActual方法的参数是LambdaObserver
                //ObservableMap$MapObserver收到ObservableJust发送的事件，会调用function.apply转换，转换后的事件类型才是下一级observer订阅的类型，然后发送给下一级observer
                //ObservableSchedulers subscribeActual:IO线程
                .subscribeOn(Schedulers.io())
                //ObservableMap subscribeActual:CPU线程
                .map(new Function<String, Bitmap>() { // 事件变换 // ObservableMap  source -> ObservableJust
                    @Override
                    public Bitmap apply(@NonNull String urlPath) throws Exception {
                        Log.e(TAG,"subscribeMap map apply:"+ Thread.currentThread().getName());
                        URL url = new URL(urlPath);
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        InputStream inputStream = urlConnection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        return bitmap;
                    }
                })
               //ObservableSchedulers subscribeActual:CPU线程
                .subscribeOn(Schedulers.cpu())
                .subscribe(new Consumer<Bitmap>() {
                    @Override
                    public void onNext(Bitmap bitmap) throws Exception {
                        Log.e(TAG,"subscribeMap onNext=="+ Thread.currentThread().getName());
                        mImage.setImageBitmap(bitmap);
                    }
                });

    }

    public void subscribeMap(View view) {
        //ObservableJust的subscribeActual方法的参数是ObservableMap$MapObserver,这个Observer是ObserverOnObservable通过静态代理LambdaObserver实现的
        Observable.just("http://img.taopic.com/uploads/allimg/130331/240460-13033106243430.jpg")// ObservableJust
              // ObservableMap的subscribeActual方法的参数是LambdaObserver
                //ObservableMap$MapObserver收到ObservableJust发送的事件，会调用function.apply转换，转换后的事件类型才是下一级observer订阅的类型，然后发送给下一级observer
                .map(new Function<String, Bitmap>() { // 事件变换 // ObservableMap  source -> ObservableJust
                    @Override
                    public Bitmap apply(@NonNull String urlPath) throws Exception {
                        Log.e(TAG,"subscribeMap map apply:"+ Thread.currentThread().getName());
                        URL url = new URL(urlPath);
                        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                        InputStream inputStream = urlConnection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        return bitmap;
                    }
                })
                .subscribe(new Consumer<Bitmap>() { // ObservableMap
                    @Override
                    public void onNext(Bitmap bitmap) throws Exception {
                        Log.e(TAG,"subscribeMap onNext=="+ Thread.currentThread().getName());
                        mImage.setImageBitmap(bitmap);
                    }
                });
    }
}
