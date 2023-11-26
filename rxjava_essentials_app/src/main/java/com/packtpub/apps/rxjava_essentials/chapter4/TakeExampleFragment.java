package com.packtpub.apps.rxjava_essentials.chapter4;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

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
import rx.Observer;

public class TakeExampleFragment extends Fragment {

    @BindView(R.id.fragment_first_example_list)
    RecyclerView mRecyclerView;

    @BindView(R.id.fragment_first_example_swipe_container)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.takeLast)
    Button mTakeLast;
    Unbinder unbinder;

    private ApplicationAdapter mAdapter;

    private ArrayList<AppInfo> mAddedApps = new ArrayList<>();
    private List<AppInfo> mApps;

    public TakeExampleFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =
                inflater.inflate(R.layout.fragment_example_takelast_repeat_range, container, false);
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
        mSwipeRefreshLayout.setRefreshing(true);
        mRecyclerView.setVisibility(View.GONE);

        mApps = ApplicationsList.getInstance().getList().subList(0,2);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    // take中实现很简单，记录发送的个数，到了之后发送onCompleted
    private void take() {
        Log.e("TAG", "TakeExampleFragment take:" + mApps.size());
        Observable.from(mApps)
                .take(3)
                .subscribe(new Observer<AppInfo>() {
                    @Override
                    public void onCompleted() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getActivity(), "Something went south!", Toast.LENGTH_SHORT)
                                .show();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onNext(AppInfo appInfo) {
                        mAddedApps.add(appInfo);
                        mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
                    }
                });
    }

    //takelast 和skiplast的实现有点一样，又有点不一样
    //都使用了队列，skiplast队列数量达到，取第一个发送；takelast队列数量达到，直接扔掉，然后在代理subcriber onComplete发送队列的数据
    public void takeLast() {
        //		mAdapter.clearData();
        Log.e("TAG", "TakeExampleFragment takeLast:"+mApps.size());
        //takeLast(3)传的数据可以大于mApps的数量
        Observable.from(mApps)
                .takeLast(3)
                .subscribe(new Observer<AppInfo>() {
                    @Override
                    public void onCompleted() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getActivity(), "Something went south!", Toast.LENGTH_SHORT)
                                .show();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onNext(AppInfo appInfo) {
                        Log.e("TAG",
                                "TakeRepeatRangeDeferExampleFragment onNext:" + appInfo.getName() +
                                        " " + Thread.currentThread().getName());
                        mAddedApps.add(appInfo);
                        mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick({R.id.takeLast, R.id.take})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.takeLast:
                takeLast();
                break;
            case R.id.take:
                take();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + view.getId());
        }
    }


}
