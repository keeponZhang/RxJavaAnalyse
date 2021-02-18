package com.packtpub.apps.rxjava_essentials.example1;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.packtpub.apps.rxjava_essentials.R;

import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.subjects.AsyncSubject;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;

/**
 * createBy keepon
 */
public class EmptyNeverThrowFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_empty_never_throw, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        view.findViewById(R.id.testEmpty).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testEmpty();
            }
        });
        view.findViewById(R.id.testNever).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testNever();
            }
        });
        view.findViewById(R.id.testThrow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testThrow();
            }
        });
    }

    private void testEmpty() {
        Subscriber<String> delegatestringSubscriber = new Subscriber<String>() {
            @Override
            public String getName() {
                return "delegatestringSubscriber";
            }

            @Override
            public void onCompleted() {
                Log.e("TAG", "CreateFragment delegatestringSubscriber onCompleted:");
            }

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "CreateFragment delegatestringSubscriber onError:");
            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "CreateFragment delegatestringSubscriber onNext:");
            }
        };
        Observable<String> empty = Observable.empty();
        //这里其实会把Producer传到顶层的Observer，但是因为onCompleted没有调用op.onCompleted,
        // 所以delegatestringSubscriber是收不到的
        empty.subscribe(new Subscriber<String>(delegatestringSubscriber) {
            @Override
            public void onCompleted() {
                if(op!=null){

                }
                Log.e("TAG", "CreateFragment new onCompleted:");
            }

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "CreateFragment new onError:");
            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "CreateFragment new onNext:");
            }

            @Override
            public String getName() {
                return "观察者";
            }
        });


    }

    private void testNever() {
        Subscriber<String> delegatestringSubscriber = new Subscriber<String>() {
            @Override
            public String getName() {
                return "delegatestringSubscriber";
            }

            @Override
            public void onCompleted() {
                Log.e("TAG", "CreateFragment delegatestringSubscriber onCompleted:");
            }

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "CreateFragment delegatestringSubscriber onError:");
            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "CreateFragment delegatestringSubscriber onNext:");
            }
        };
        Observable<String> empty = Observable.never();
        empty.subscribe(new Subscriber<String>(delegatestringSubscriber) {
            @Override
            public void onCompleted() {
                Log.e("TAG", "CreateFragment new onCompleted:");
            }

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "CreateFragment new onError:");
            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "CreateFragment new onNext:");
            }

            @Override
            public String getName() {
                return "观察者";
            }
        });


    }

    private void testThrow() {
        Subscriber<String> delegatestringSubscriber = new Subscriber<String>() {
            @Override
            public String getName() {
                return "delegatestringSubscriber";
            }

            @Override
            public void onCompleted() {
                Log.e("TAG", "CreateFragment delegatestringSubscriber onCompleted:");
            }

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "CreateFragment delegatestringSubscriber onError:"+e);
            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "CreateFragment delegatestringSubscriber onNext:");
            }
        };
        Observable<String> empty = Observable.error(new RuntimeException("keepon"));
        empty.subscribe(new Subscriber<String>(delegatestringSubscriber) {
            @Override
            public void onCompleted() {
                Log.e("TAG", "CreateFragment new onCompleted:");
            }

            @Override
            public void onError(Throwable e) {
                Log.e("TAG", "CreateFragment new onError:"+e);
            }

            @Override
            public void onNext(String s) {
                Log.e("TAG", "CreateFragment new onNext:");
            }

            @Override
            public String getName() {
                return "观察者";
            }
        });


    }
}
