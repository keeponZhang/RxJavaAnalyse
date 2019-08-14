package com.demo.lizejun.rxsample.chapter3;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import com.demo.lizejun.rxsample.R;

import java.util.concurrent.TimeUnit;

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
import io.reactivex.subjects.PublishSubject;

public class SearchActivity extends AppCompatActivity {

    private EditText mEtSearch;
    private TextView mTvSearch;
    private PublishSubject<String> mPublishSubject;
    private DisposableObserver<String> DisposableObserver;
    private CompositeDisposable mCompositeDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mEtSearch = (EditText) findViewById(R.id.et_search);
        mTvSearch = (TextView) findViewById(R.id.tv_search_result);
        mEtSearch.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                startSearch(s.toString());
            }
        });
        mPublishSubject = PublishSubject.create();
        DisposableObserver = new DisposableObserver<String>() {

            @Override
            public void onNext(String s) {
                mTvSearch.setText(s);
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        };
        mPublishSubject.debounce(350, TimeUnit.MILLISECONDS)
                .filter(new Predicate<String>() {

                    @Override
                    public boolean test(String s) throws Exception {
                        return s.length() > 0;
                    }

                }).switchMap(new Function<String, ObservableSource<String>>() {

            @Override
            public ObservableSource<String> apply(String query) throws Exception {
                return getSearchObservable(query);
            }

        }).observeOn(AndroidSchedulers.mainThread()).subscribe(DisposableObserver);
        mCompositeDisposable = new CompositeDisposable();
        mCompositeDisposable.add(mCompositeDisposable);
//        switchMapTest();
        switchMapTest2();
    }

//    switch()和flatMap()很像，除了一点:当源Observable发射一个新的数据项时，如果旧数据项订阅还未完成，
// 就取消旧订阅数据和停止监视那个数据项产生的Observable,开始监视新的数据项.
    private void switchMapTest() {
//        这是在同一线程产生数据,所以当第二个数据项来临时，第一个已经完成了，同理，c,d,e都将完成
        Observable.just("A", "B", "C", "D", "E").switchMap(new Function<String, ObservableSource<String>>() {
            @Override
            public Observable<String> apply(String s) {
                Observable<String> ob = Observable.just(s);
                return ob;
            }
        }).subscribeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "SearchActivity------>onError()" + e);
            }

            @Override
            public void onComplete() {
                Log.e("TAG", "SearchActivity------>onCompleted()");
            }

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "SearchActivity ------>onNext:" + s);
            }
        });

    }
    private void switchMapTest2() {
//        SearchActivity ------>onNext:E
//        SearchActivity------>onCompleted()
        Observable.just("A", "B", "C", "D", "E").switchMap(new Function<String, ObservableSource<String>>() {
            @Override
            public Observable<String> apply(String s) {
                return Observable.just(s).subscribeOn(Schedulers.newThread());
            }
        }).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "SearchActivity------>onError()" + e);
            }

            @Override
            public void onComplete() {
                Log.e("TAG", "SearchActivity------>onCompleted()");
            }

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "SearchActivity ------>onNext:" + s);
            }
        });


//        当源Observable变换出的Observable同时进行时,
//        A --> 取消空的，没有可以取消的
//        B--> A被取消
//        C-->  B被取消
//        D-->  C被取消
//        E-->  D被取消
    }

    private void startSearch(String query) {
        mPublishSubject.onNext(query);
    }

    private Observable<String> getSearchObservable(final String query) {
        return Observable.create(new ObservableOnSubscribe<String>() {

            @Override
            public void subscribe(ObservableEmitter<String> observableEmitter) throws Exception {
                Log.e("SearchActivity", "开始请求，关键词为：" + query);
                try {
                    Thread.sleep(100 + (long) (Math.random() * 500));
                } catch (InterruptedException e) {
                    if (!observableEmitter.isDisposed()) {
                        observableEmitter.onError(e);
                    }
                }
                Log.d("SearchActivity", "结束请求，关键词为：" + query);
                observableEmitter.onNext("完成搜索，关键词为：" + query);
                observableEmitter.onComplete();
            }
        }).subscribeOn(Schedulers.io());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeDisposable.clear();
    }
}
