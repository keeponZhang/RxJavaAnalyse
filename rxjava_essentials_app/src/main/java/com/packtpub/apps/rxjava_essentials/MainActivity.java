package com.packtpub.apps.rxjava_essentials;

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;

import com.packtpub.apps.rxjava_essentials.chapter4.DistinctExampleFragment;
import com.packtpub.apps.rxjava_essentials.chapter4.FilterFirstSingleSampleTimeoutDebounceFragment;
import com.packtpub.apps.rxjava_essentials.chapter4.TakeRepeatRangeDeferExampleFragment;
import com.packtpub.apps.rxjava_essentials.chapter5.GroupByConcatExampleFragment;
import com.packtpub.apps.rxjava_essentials.chapter5.MapFlatMapCastFragment;
import com.packtpub.apps.rxjava_essentials.chapter5.ScanExampleFragment;
import com.packtpub.apps.rxjava_essentials.chapter6.AndThenWhenRetrySubjectFragment;
import com.packtpub.apps.rxjava_essentials.chapter6.CombineLatestSwithStartWithExampleFragment;
import com.packtpub.apps.rxjava_essentials.chapter6.JoinExampleFragment;
import com.packtpub.apps.rxjava_essentials.chapter6.MergeExampleFragment;
import com.packtpub.apps.rxjava_essentials.chapter6.ZipBufferWindowExampleFragment;
import com.packtpub.apps.rxjava_essentials.chapter7.LongTaskFragment;
import com.packtpub.apps.rxjava_essentials.chapter7.NetworkTaskFragment;
import com.packtpub.apps.rxjava_essentials.chapter7.SharedPreferencesListFragment;
import com.packtpub.apps.rxjava_essentials.chapter8.SoActivity;
import com.packtpub.apps.rxjava_essentials.example1.CreateFragment;
import com.packtpub.apps.rxjava_essentials.example2.FromFragment;
import com.packtpub.apps.rxjava_essentials.example3.JustTimerExampleFragment;
import com.packtpub.apps.rxjava_essentials.navigation_drawer.NavigationDrawerCallbacks;
import com.packtpub.apps.rxjava_essentials.navigation_drawer.NavigationDrawerFragment;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity implements NavigationDrawerCallbacks {

    @BindView(R.id.toolbar_actionbar)
    Toolbar mToolbar;

    @BindView(R.id.drawer)
    DrawerLayout mDrawerLayout;

    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.fragment_drawer);
        mNavigationDrawerFragment.setup(R.id.fragment_drawer, mDrawerLayout, mToolbar);

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(com.packtpub.apps.rxjava_essentials.R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        FragmentManager fragmentManager = getFragmentManager();
        switch (position) {
            case 0:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new CreateFragment())
                    .commit();
                break;
            case 1:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new FromFragment())
                    .commit();
                break;
            case 2:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new JustTimerExampleFragment())
                    .commit();
                break;
            case 3:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new FilterFirstSingleSampleTimeoutDebounceFragment())
                    .commit();
                break;
            case 4:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new TakeRepeatRangeDeferExampleFragment())
                    .commit();
                break;
            case 5:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new DistinctExampleFragment())
                    .commit();
                break;
            case 6:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new MapFlatMapCastFragment())
                    .commit();
                break;
            case 7:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new ScanExampleFragment())
                    .commit();
                break;
            case 8:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new GroupByConcatExampleFragment())
                    .commit();
                break;
            case 9:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new MergeExampleFragment())
                    .commit();
                break;
            case 10:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new ZipBufferWindowExampleFragment())
                    .commit();
                break;
            case 11:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new JoinExampleFragment())
                    .commit();
                break;
            case 12:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new CombineLatestSwithStartWithExampleFragment())
                    .commit();
                break;
            case 13:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new AndThenWhenRetrySubjectFragment())
                    .commit();
                break;
            case 14:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new SharedPreferencesListFragment())
                    .commit();
                break;
            case 15:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new LongTaskFragment())
                    .commit();
                break;
            case 16:
                fragmentManager.beginTransaction()
                    .replace(R.id.container, new NetworkTaskFragment())
                    .commit();
                break;
            case 17:
                startActivity(new Intent(this, SoActivity.class));
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mNavigationDrawerFragment.isDrawerOpen()) {
            mNavigationDrawerFragment.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }
}
