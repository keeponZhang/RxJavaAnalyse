/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.trello.rxlifecycle2.sample;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public class RxLifeCycleActivity extends RxAppCompatActivity {

    private static final String TAG = "RxLifecycleAndroid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate()-----------");

        setContentView(R.layout.activity_main);

        // Specifically bind this until onPause()
//        Observable.interval(1, TimeUnit.SECONDS)
//            .doOnDispose(new Action() {
//                @Override
//                public void run() throws Exception {
//                    Log.i(TAG, "Unsubscribing subscription from onCreate()");
//                }
//            })
////            .compose(this.<Long>bindUntilEvent(ActivityEvent.PAUSE))
//            .compose(this.<Long>bindToLifecycle())
//            .subscribe(new Consumer<Long>() {
//                @Override
//                public void accept(Long num) throws Exception {
//                    Log.i(TAG, "Started in onCreate(), running until onPause(): " + num);
//                }
//            });
        test2();
    }

    private  boolean isHasSubscribe = false;
    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart()---------------");
        if(isHasSubscribe){
            return;
        }
        isHasSubscribe = true;
        // Using automatic unsubscription, this should determine that the correct time to
        // unsubscribe is onStop (the opposite of onStart).
//        test();

    }

    private void test() {
        Observable.interval(1, TimeUnit.SECONDS)
            .doOnDispose(new Action() {
                @Override
                public void run() throws Exception {
                    Log.i(TAG, "Unsubscribing subscription from onStart()");
                }
            })
           .compose(this.<Long>bindToLifecycle())
            .subscribe(new Consumer<Long>() {
                @Override
                public void accept(Long num) throws Exception {
                    Log.i(TAG, "Started in onStart(), running until in onStop(): " + num);
                }
            });
    }
    private void test2() {
        Observable.create(new ObservableOnSubscribe<Long>() {
            @Override
            public void subscribe(ObservableEmitter<Long> e) throws Exception {
                for (int i = 1; i < 2; i++) {

                    e.onNext((long) i);
                }
            }
        })
                .doOnDispose(new Action() {
                    @Override
                    public void run() throws Exception {
                        Log.i(TAG, "Unsubscribing subscription from onStart()");
                    }
                })
                .compose(this.<Long>bindToLifecycle())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long num) throws Exception {
                        Log.i(TAG, "Started in onStart(), running until in onStop(): " + num);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume()--------------");

        // `this.<Long>` is necessary if you're compiling on JDK7 or below.
        //
        // If you're using JDK8+, then you can safely remove it.
//        Observable.interval(1, TimeUnit.SECONDS)
//            .doOnDispose(new Action() {
//                @Override
//                public void run() throws Exception {
//                    Log.i(TAG, "Unsubscribing subscription from onResume()");
//                }
//            })
//            .compose(this.<Long>bindUntilEvent(ActivityEvent.DESTROY))
//            .subscribe(new Consumer<Long>() {
//                @Override
//                public void accept(Long num) throws Exception {
//                    Log.i(TAG, "Started in onResume(), running until in onDestroy(): " + num);
//                }
//            });
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG, "onPause()-------------");
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.d(TAG, "onStop()------------------");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy()--------------------");
    }

    public void jump(View view) {
        startActivity(new Intent(this,RxLifecycle2Activity.class));
    }
}
