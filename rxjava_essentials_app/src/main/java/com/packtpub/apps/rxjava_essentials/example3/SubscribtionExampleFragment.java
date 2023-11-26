package com.packtpub.apps.rxjava_essentials.example3;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.packtpub.apps.rxjava_essentials.R;
import com.packtpub.apps.rxjava_essentials.apps.AppInfo;
import com.packtpub.apps.rxjava_essentials.apps.ApplicationAdapter;
import com.packtpub.apps.rxjava_essentials.apps.ApplicationsList;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;

public class SubscribtionExampleFragment extends Fragment {

    @BindView(R.id.fragment_first_example_list)
    RecyclerView mRecyclerView;

    @BindView(R.id.fragment_first_example_swipe_container)
    SwipeRefreshLayout mSwipeRefreshLayout;
    Unbinder unbinder;

    private ApplicationAdapter mAdapter;

    private ArrayList<AppInfo> mAddedApps = new ArrayList<>();

    private Subscription mTimeSubscription;
    private List<AppInfo> mApps;
    private AppInfo mAppOne;
    private AppInfo mAppTwo;
    private AppInfo mAppThree;

    public SubscribtionExampleFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_example_3, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        mAdapter = new ApplicationAdapter(new ArrayList<>(), R.layout.applications_list_item);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.myPrimaryColor));
        mSwipeRefreshLayout.setProgressViewOffset(false, 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24,
                        getResources().getDisplayMetrics()));

        // Progress
        mSwipeRefreshLayout.setEnabled(false);
        // mSwipeRefreshLayout.setRefreshing(true);
        mRecyclerView.setVisibility(View.GONE);

        mApps = ApplicationsList.getInstance().getList();

        mAppOne = mApps.get(0);

        mAppTwo = mApps.get(1);

        mAppThree = mApps.get(2);



    }






    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mTimeSubscription != null && !mTimeSubscription.isUnsubscribed()) {
            mTimeSubscription.unsubscribe();
        }
        unbinder.unbind();
    }

    @OnClick({ R.id.subscribeon})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.subscribeon:
                Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        subscriber.onNext("Keepon");
                    }
                }).subscribeOn(Schedulers.io()).subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String s) {
                        Log.e("TAG", "SubscribtionExampleFragment onNext:" + s);
                    }
                });
                break;
            default:
                break;
        }
    }


}
