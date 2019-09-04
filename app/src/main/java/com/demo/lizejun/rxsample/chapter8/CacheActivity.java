package com.demo.lizejun.rxsample.chapter8;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.demo.lizejun.rxsample.R;
import com.demo.lizejun.rxsample.network.entity.NewsAdapter;
import com.demo.lizejun.rxsample.network.entity.NewsResultEntity;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class CacheActivity extends AppCompatActivity {

    private static final String TAG = CacheActivity.class.getSimpleName();

    private Button mBtContactRefresh;
    private Button mBtContactEagerRefresh;
    private Button mBtMergeRefresh;
    private Button mBtPublishRefresh;
    private NewsAdapter mNewsAdapter;
    private List<NewsResultEntity> mNewsResultEntities = new ArrayList<>();
    private CompositeDisposable mCompositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cache);
        mBtContactRefresh = (Button) findViewById(R.id.bt_contact_refresh);
        mBtContactEagerRefresh = (Button) findViewById(R.id.bt_contact_eager_refresh);
        mBtMergeRefresh = (Button) findViewById(R.id.bt_merge_refresh);
        mBtPublishRefresh = (Button) findViewById(R.id.bt_publish_refresh);
        findViewById(R.id.bt_merge_take_until).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshArticleUseMergeTakeUntil();
            }
        });
        mBtContactRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshArticleUseContact();
            }
        });
        mBtContactEagerRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshArticleUseContactEager();
            }
        });
        mBtMergeRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshArticleUseMerge();
            }
        });
        mBtPublishRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshArticleUsePublish();
            }
        });
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv_news);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mNewsAdapter = new NewsAdapter(mNewsResultEntities);
        recyclerView.setAdapter(mNewsAdapter);
        mCompositeDisposable = new CompositeDisposable();
    }

    private void refreshArticleUseContact() {
        Observable<List<NewsResultEntity>> contactObservable = Observable.concat(
                getCacheArticle(500).subscribeOn(Schedulers.io()), getNetworkArticle(2000).subscribeOn(Schedulers.io()));
        DisposableObserver<List<NewsResultEntity>> disposableObserver = getArticleObserver();
        contactObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
    }

    private void refreshArticleUseContactEager() {
        List<Observable<List<NewsResultEntity>>> observables = new ArrayList<>();
        observables.add(getCacheArticle(2500).subscribeOn(Schedulers.io()));
        observables.add(getNetworkArticle(2000).subscribeOn(Schedulers.io()));
        Observable<List<NewsResultEntity>> contactObservable = Observable.concatEager(observables);
        DisposableObserver<List<NewsResultEntity>> disposableObserver = getArticleObserver();
        contactObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
    }

    private void refreshArticleUseMerge() {
        Observable<List<NewsResultEntity>> contactObservable = Observable.merge(
                getCacheArticle(2000).subscribeOn(Schedulers.io()), getNetworkArticle(500).subscribeOn(Schedulers.io()));
        DisposableObserver<List<NewsResultEntity>> disposableObserver = getArticleObserver();
        contactObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
    }

    //takeUntil 也触发订阅了一次
    private void refreshArticleUseMergeTakeUntil() {
        Observable<List<NewsResultEntity>> net = getNetworkArticle(500).subscribeOn(Schedulers.io());
        Observable<List<NewsResultEntity>> cache = getCacheArticle(2000).subscribeOn(Schedulers.io());
        DisposableObserver<List<NewsResultEntity>> disposableObserver = getArticleObserver();
        Observer<List<NewsResultEntity>> normalArticleObserver = getNormalArticleObserver();
        //这两个net虽然是同一个对象，当订阅时是分开的
        Observable<List<NewsResultEntity>> contactObservable = Observable.merge(
                cache, net).takeUntil(net);
        contactObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(normalArticleObserver);
    }
//    但是上面有一点缺陷，就是调用merge和takeUntil会发生两次订阅，这时候就需要使用publish操作符，它接收一个Function函数，
//  该函数返回一个Observable，该Observable是对原Observable，
//    也就是上面网络源的Observable转换之后的结果，该Observable可以被takeUntil和merge操作符所共享，从而实现只订阅一次的效果。
    private void refreshArticleUsePublish() {
        Observable<List<NewsResultEntity>> publishObservable = getNetworkArticle(500).subscribeOn(Schedulers.io()).publish(new Function<Observable<List<NewsResultEntity>>, ObservableSource<List<NewsResultEntity>>>() {

            @Override
            public ObservableSource<List<NewsResultEntity>> apply(Observable<List<NewsResultEntity>> network) throws Exception {
                return Observable.merge(network, getCacheArticle(2000).subscribeOn(Schedulers.io()).takeUntil(network));
            }

        });
        DisposableObserver<List<NewsResultEntity>> disposableObserver = getArticleObserver();
        publishObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(disposableObserver);
    }


    private Observable<List<NewsResultEntity>> getCacheArticle(final long simulateTime) {
        return Observable.create(new ObservableOnSubscribe<List<NewsResultEntity>>() {
            @Override
            public void subscribe(ObservableEmitter<List<NewsResultEntity>> observableEmitter) throws Exception {
                try {
                    Log.d(TAG, "开始加载缓存数据");
                    Thread.sleep(simulateTime);
                    List<NewsResultEntity> results = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        NewsResultEntity entity = new NewsResultEntity();
                        entity.setType("缓存");
                        entity.setDesc("序号=" + i);
                        results.add(entity);
                    }
                    Log.w("TAG", "CacheActivity subscribe 发送缓存数据 results:" + results);
                    observableEmitter.onNext(results);
                    observableEmitter.onComplete();
                    Log.d(TAG, "结束加载缓存数据");
                } catch (InterruptedException e) {
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onError(e);
                    }
                }
            }
        });
    }

    private Observable<List<NewsResultEntity>> getNetworkArticle(final long simulateTime) {
        return Observable.create(new ObservableOnSubscribe<List<NewsResultEntity>>() {
            @Override
            public void subscribe(ObservableEmitter<List<NewsResultEntity>> observableEmitter) throws Exception {
                try {
                    Log.d(TAG, "开始加载网络数据");
                    Thread.sleep(simulateTime);
                    List<NewsResultEntity> results = new ArrayList<>();
                    for (int i = 0; i < 3; i++) {
                        NewsResultEntity entity = new NewsResultEntity();
                        entity.setType("网络");
                        entity.setDesc("序号=" + i);
                        results.add(entity);
                    }
                    //a.正常情况。
                    Log.e("TAG", "CacheActivity subscribe 发送网络数据 results:" + results+" observableEmitter="+observableEmitter);
                    observableEmitter.onNext(results);
                    observableEmitter.onComplete();
                    //b.发生异常。
                    //observableEmitter.onError(new Throwable("netWork Error"));
                    Log.d(TAG, "结束加载网络数据");
                } catch (InterruptedException e) {
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onError(e);
                    }
                }
            }
        });
//                .onErrorResumeNext(new Function<Throwable, ObservableSource<? extends List<NewsResultEntity>>>() {
//
//            @Override
//            public ObservableSource<? extends List<NewsResultEntity>> apply(Throwable throwable) throws Exception {
//                Log.d(TAG, "网络请求发生错误throwable=" + throwable);
//                return Observable.never();
//            }
//        });
    }

    private DisposableObserver<List<NewsResultEntity>> getArticleObserver() {
        return new DisposableObserver<List<NewsResultEntity>>() {

            @Override
            public void onNext(List<NewsResultEntity> newsResultEntities) {
                Log.e("TAG", "CacheActivity 收到 onNext:" + newsResultEntities);
                mNewsResultEntities.clear();
                mNewsResultEntities.addAll(newsResultEntities);
                mNewsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Throwable throwable) {
                Log.d(TAG, "加载错误, e=" + throwable);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "加载完成 ObservableTakeUntil");
            }
        };
    }

    private Observer<List<NewsResultEntity>> getNormalArticleObserver() {
        return new Observer<List<NewsResultEntity>>() {

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(List<NewsResultEntity> newsResultEntities) {
                Log.e("TAG", "CacheActivity 收到 onNext:" + newsResultEntities);
                mNewsResultEntities.clear();
                mNewsResultEntities.addAll(newsResultEntities);
                mNewsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Throwable throwable) {
                Log.d(TAG, "加载错误, e=" + throwable);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "加载完成 ObservableTakeUntil");
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.clear();
    }

    //注意括号要对应对
    public void takeUtil(View view) {
        Observable<List<NewsResultEntity>> networkArticle = getNetworkArticle(800);
        DisposableObserver<List<NewsResultEntity>> disposableObserver = getArticleObserver();
        //这里用merge
        Observable.merge(networkArticle, getCacheArticle(2000))
                .takeUntil(networkArticle)
                .subscribe(disposableObserver);
    }

    public void takeUtil2(View view) {
        Observable<List<NewsResultEntity>> networkArticle = getNetworkArticle(800);
        Observable<List<NewsResultEntity>> cacheArticle = getCacheArticle(2000);
        Observer<List<NewsResultEntity>> observer = getNormalArticleObserver();
        Log.w("TAG", "CacheActivity takeUtil2 observer:" + observer + "  networkArticle=" + networkArticle + " cacheArticle=" + cacheArticle);
        Observable.merge(networkArticle, cacheArticle)
                .takeUntil(networkArticle)
//                .filter(new Predicate<List<NewsResultEntity>>() {
//                    @Override
//                    public boolean test(List<NewsResultEntity> newsResultEntities) throws Exception {
//                        return true;
//                    }
//                })
                .subscribe(observer);
    }

    public void flatmap(View view) {
        final Observable<List<NewsResultEntity>> networkArticle = getNetworkArticle(800);
        final Observable<List<NewsResultEntity>> cacheArticle = getCacheArticle(2000);
        Observer<List<NewsResultEntity>> observer = getNormalArticleObserver();
        Log.w("TAG", "CacheActivity flatmap observer:" + observer);
        Observable.create(new ObservableOnSubscribe<Observable>() {
            @Override
            public void subscribe(ObservableEmitter<Observable> e) throws Exception {
                e.onNext(networkArticle);
                e.onNext(cacheArticle);
            }
        }).flatMap(new Function<Observable, ObservableSource<List<NewsResultEntity>>>() {
            @Override
            public ObservableSource<List<NewsResultEntity>> apply(Observable observable) throws Exception {
                return observable;
            }
        })
                .filter(new Predicate<List<NewsResultEntity>>() {
                    @Override
                    public boolean test(List<NewsResultEntity> newsResultEntities) throws Exception {
                        return true;
                    }
                })
                .subscribe(observer);

    }
}
