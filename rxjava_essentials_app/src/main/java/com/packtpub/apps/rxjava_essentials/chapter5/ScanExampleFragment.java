package com.packtpub.apps.rxjava_essentials.chapter5;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.packtpub.apps.rxjava_essentials.R;
import com.packtpub.apps.rxjava_essentials.apps.AppInfo;
import com.packtpub.apps.rxjava_essentials.apps.ApplicationAdapter;
import com.packtpub.apps.rxjava_essentials.apps.ApplicationsList;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func2;

public class ScanExampleFragment extends Fragment {

    @BindView(R.id.fragment_first_example_list)
    RecyclerView mRecyclerView;

    @BindView(R.id.fragment_first_example_swipe_container)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private ApplicationAdapter mAdapter;

    private ArrayList<AppInfo> mAddedApps = new ArrayList<>();

    public ScanExampleFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_example, container, false);
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
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));

        // Progress
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(true);
        mRecyclerView.setVisibility(View.GONE);

        List<AppInfo> apps = ApplicationsList.getInstance().getList();

        loadList(apps);
    }

    private void loadList(List<AppInfo> apps) {
        mRecyclerView.setVisibility(View.VISIBLE);
        Log.e("TAG", "ScanExampleFragment loadList:" + apps.size());
        Observable.from(apps)
                .scan(new Func2<AppInfo, AppInfo, AppInfo>() {
                    //返回值下次会作为appInfo1
                    @Override
                    public AppInfo call(AppInfo appInfo1, AppInfo appInfo2) {
                        Log.e("TAG", "ScanExampleFragment call appInfo:"+appInfo1.getName()+"  appInfo2="+appInfo2.getName());
                        if (appInfo1.getName().length() > appInfo2.getName().length()) {
                            return appInfo1;
                        } else {
                            return appInfo2;
                        }
                    }
                })
                //这里是去重的
                // .distinct()
                .subscribe(new Observer<AppInfo>() {
                    @Override
                    public void onCompleted() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getActivity(), "Something went south!", Toast.LENGTH_SHORT).show();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onNext(AppInfo appInfo) {
                        mAddedApps.add(appInfo);
                        mAdapter.addApplication(mAddedApps.size() - 1, appInfo);
                    }
                });

        scan();
    }

    private void scan() {
        //scan可以实现累加
        Observable.just(1,2,3,4,5)
                .scan(new Func2<Integer, Integer, Integer>() {
                    @Override
                    public Integer call(Integer sum, Integer item) {
                        Log.e("TAG", "ScanExampleFragment call sum:"+sum);
                        return sum + item;
                    }
                })
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        Log.d("RXJAVA", "Sequence completed.");
                    }
                    @Override
                    public void onError(Throwable e) {
                        Log.e("RXJAVA", "Something went south!");
                    }
                    @Override
                    public void onNext(Integer item) {
                        Log.e("TAG", "ScanExampleFragment onNext    :"+item);
                        Log.d("RXJAVA", "item is: " + item);
                    }
                });

//        RXJAVA: item is: 1
//        RXJAVA: item is: 3
//        RXJAVA: item is: 6
//        RXJAVA: item is: 10
//        RXJAVA: item is: 15
//        RXJAVA: Sequence completed.
    }
}
