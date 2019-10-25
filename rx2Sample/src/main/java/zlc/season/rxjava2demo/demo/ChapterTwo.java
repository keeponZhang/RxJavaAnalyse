package zlc.season.rxjava2demo.demo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import zlc.season.rxjava2demo.Api;
import zlc.season.rxjava2demo.RetrofitProvider;
import zlc.season.rxjava2demo.entity.LoginRequest;
import zlc.season.rxjava2demo.entity.LoginResponse;

import static zlc.season.rxjava2demo.MainActivity.TAG;

/**
 * Author: Season(ssseasonnn@gmail.com)
 * Date: 2016/12/9
 * Time: 16:48
 * FIXME
 */
public class ChapterTwo {
    public static void demo1() {
        Observable<Integer> observable = getObservable();
        Consumer<Integer> consumer = getConsumer();
        observable.subscribe(consumer);
    }

    public static void demo2() {
        Observable<Integer> observable = getObservable();
        Consumer<Integer> consumer = getConsumer();

        observable.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(consumer);
    }

    public static void demo3() {
        Observer<Integer> observer = getIntegerObserver();
//        多次指定上游的线程只有第一次指定的有效, 也就是说多次调用subscribeOn() 只有第一次的有效, 其余的会被忽略.
//        （注意：subscribeOn是包裹，上面的在里面，下面的在外面，这个相当于主线程里面包裹io线程，所以上层的发成在io线程，所以连续两个subscribeOn，只有上面那个起作用）
        Observable.just(100)
                .subscribeOn(Schedulers.newThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    //                    accept()是在onSubscribe
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        //需要在computation线程执行
                        Log.e("TAG", "ChapterTwo accept Thread.currentThread().getName():"+Thread.currentThread().getName());
                    }
                })
//                 虽然超过一个的 subscribeOn() 对事件处理的流程没有影响，但在流程之前却是可以利用的。
                //subscribeOn 对订阅有影响，subscribeOn中 subscribeActual 会调用下级Observer的onSubscribe方法（Observer接口定义的方法）方法调用

                //这个subscribeOn的意思是，上面的订阅在遇到上面一个subscribeOn时，发生订阅的线程都是在Schedulers.computation()线程，doOnSubscribe是在订阅过程中的onSubscribe方法调用的，换句话说doOnSubscribe会被下面最近的subscribeOn影响
                //如果没有一个subscribeOn，就是上面的订阅在遇到上面一个subscribeOn时，发生订阅的线程都是在调用demo3线程
                .subscribeOn(Schedulers.computation())
                //observeOn 本质是发送事件给最近一个观察者是做线程切换;还会把onSubscribe的调用链往下走
                .observeOn(Schedulers.io())
                .subscribe(observer);
    }
    public static void demo4() {
        Observer<Integer> observer = getIntegerObserver();
//        多次指定上游的线程只有第一次指定的有效, 也就是说多次调用subscribeOn() 只有第一次的有效, 其余的会被忽略.
//        （注意：subscribeOn是包裹，上面的在里面，下面的在外面，这个相当于主线程里面包裹io线程，所以上层的发成在io线程，所以连续两个subscribeOn，只有上面那个起作用）
        Observable.just(100)
                .subscribeOn(Schedulers.newThread())
                .doOnSubscribe(new Consumer<Disposable>() {
                    //                    accept()是在onSubscribe
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        //需要在computation线程执行
                        Log.e("TAG", "ChapterTwo doOnSubscribe1 accept Thread.currentThread().getName():"+Thread.currentThread().getName());
                    }
                })
//                 虽然超过一个的 subscribeOn() 对事件处理的流程没有影响，但在流程之前却是可以利用的。
                //subscribeOn 对订阅有影响，subscribeOn中 subscribeActual 会调用下级Observer的onSubscribe方法（Observer接口定义的方法）方法调用

                //这个subscribeOn的意思是，上面的订阅在遇到上面一个subscribeOn时，发生订阅的线程都是在Schedulers.computation()线程，doOnSubscribe是在订阅过程中的onSubscribe方法调用的，换句话说doOnSubscribe会被下面最近的subscribeOn影响
                //如果没有一个subscribeOn，就是上面的订阅在遇到上面一个subscribeOn时，发生订阅的线程都是在调用demo3线程
                .subscribeOn(Schedulers.computation())
                //上面触发调用的onSubscribe，遇到doOnSubscribe会停止往下传递
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        Log.e("TAG", "ChapterTwo doOnSubscribe2 accept Thread.currentThread().getName():"+Thread.currentThread().getName());

                    }
                })
                .subscribeOn(Schedulers.newThread())
                //observeOn 本质是发送事件给最近一个观察者是做线程切换；还会把onSubscribe的调用链往下走
                .observeOn(Schedulers.io())
                .subscribe(observer);
    }

    private static Observable<Integer> getObservable() {
        Observable<Integer> observable = Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                Log.d(TAG, "Observable thread is : " + Thread.currentThread().getName());
                Log.d(TAG, "emit 1");
                emitter.onNext(1);
            }
        });
        return observable;
    }
    private static Observable<Integer> getJustObservable() {
        Observable<Integer> just = Observable.just(1);
        return just;
    }

    private static Consumer<Integer> getConsumer() {
        Consumer<Integer> consumer = new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {
                Log.d(TAG, "Observer thread is :" + Thread.currentThread().getName());
                Log.d(TAG, "onNext: " + integer);
            }
        };
        return consumer;
    }

    @NonNull
    private static Observer<Integer> getIntegerObserver() {
        return new Observer<Integer>() {
                @Override
                public void onSubscribe(Disposable d) {

                }

                @Override
                public void onNext(Integer value) {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onComplete() {

                }
            };
    }

    public static void practice1(final Context context) {
        Api api = RetrofitProvider.get().create(Api.class);
        api.login(new LoginRequest())
                .subscribeOn(Schedulers.io())               //在IO线程进行网络请求
                .observeOn(AndroidSchedulers.mainThread())  //回到主线程去处理请求结果
                .subscribe(new Observer<LoginResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(LoginResponse value) {
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(context, "登录失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {
                        Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
