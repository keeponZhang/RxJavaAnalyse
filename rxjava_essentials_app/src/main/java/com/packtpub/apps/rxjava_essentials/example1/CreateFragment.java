package com.packtpub.apps.rxjava_essentials.example1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
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

import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.packtpub.apps.rxjava_essentials.App;
import com.packtpub.apps.rxjava_essentials.R;
import com.packtpub.apps.rxjava_essentials.Utils;
import com.packtpub.apps.rxjava_essentials.apps.AppInfo;
import com.packtpub.apps.rxjava_essentials.apps.AppInfoRich;
import com.packtpub.apps.rxjava_essentials.apps.ApplicationAdapter;
import com.packtpub.apps.rxjava_essentials.apps.ApplicationsList;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class CreateFragment extends Fragment {

    @BindView(R.id.fragment_first_example_list)
    RecyclerView mRecyclerView;

    @BindView(R.id.fragment_first_example_swipe_container)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private ApplicationAdapter mAdapter;

    private File mFilesDir;

    public CreateFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_example_1, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        view.findViewById(R.id.refreshTheList).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshTheList();
            }
        });

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

        getFileDir()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(file -> {
                    mFilesDir = file;
                    refreshTheList();
                });
    }



    private Observable<File> getFileDir() {
        return Observable.create(subscriber -> {
            //一开始是空的
            subscriber.onNext(App.instance.getFilesDir());
            subscriber.onCompleted();
        });
    }

    private void refreshTheList() {
        getApps()
                //toSortedList也是自带的一个操作符
                .toSortedList()
                //toSortedList发送的是一个集合
                .subscribe(new Observer<List<AppInfo>>() {
                    @Override
                    public void onCompleted() {
                        Toast.makeText(getActivity(), "Here is the list!", Toast.LENGTH_LONG)
                                .show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(getActivity(), "Something went wrong!", Toast.LENGTH_SHORT)
                                .show();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onNext(List<AppInfo> appInfos) {
                        mRecyclerView.setVisibility(View.VISIBLE);
                        mAdapter.addApplications(appInfos);
                        mSwipeRefreshLayout.setRefreshing(false);
                        storeList(appInfos);
                    }
                });
    }

    private void storeList(List<AppInfo> appInfos) {
        //内存保留一份
        ApplicationsList.getInstance().setList(appInfos);

        Schedulers.io().createWorker().schedule(() -> {
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            Type appInfoType = new TypeToken<List<AppInfo>>() {
            }.getType();
            sharedPref.edit().putString("APPS", new Gson().toJson(appInfos, appInfoType)).apply();
        });
    }

    private Observable<AppInfo> getApps() {

        return Observable.create(subscriber -> {
            List<AppInfoRich> apps = new ArrayList<>();

            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> infos =
                    getActivity().getPackageManager().queryIntentActivities(mainIntent, 0);
            for (ResolveInfo info : infos) {
                apps.add(new AppInfoRich(getActivity(), info));
            }

            for (AppInfoRich appInfo : apps) {
                Bitmap icon = Utils.drawableToBitmap(appInfo.getIcon());
                String name = appInfo.getName();
                String iconPath = mFilesDir + "/" + name;
                Utils.storeBitmap(App.instance, icon, name);
                Log.d("TAG", "CreateFragment getApps:" + Thread.currentThread().getName());
                if (subscriber.isUnsubscribed()) {
                    return;
                }
                subscriber.onNext(new AppInfo(name, iconPath, appInfo.getLastUpdateTime()));
            }
            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        });


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        ButterKnife.reset(this);
    }
}
