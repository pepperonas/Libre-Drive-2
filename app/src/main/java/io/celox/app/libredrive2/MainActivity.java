/*
 * Copyright (c) 2018 Martin Pfeffer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.celox.app.libredrive2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.pepperonas.aespreferences.AesPrefs;
import com.pepperonas.andbasx.base.ToastUtils;

import io.celox.app.libredrive2.custom.BottomBarAdapter;
import io.celox.app.libredrive2.fragments.AboutFragment;
import io.celox.app.libredrive2.fragments.MapFragment;
import io.celox.app.libredrive2.fragments.NearbyFragment;
import io.celox.app.libredrive2.fragments.SettingsFragment;
import io.celox.app.libredrive2.services.GpsService;
import io.celox.app.libredrive2.utils.AesConst;
import io.celox.app.libredrive2.utils.Const;
import io.celox.app.libredrive2.utils.DatabaseCtrls;

/**
 * The type Main activity.
 *
 * @author Martin Pfeffer
 * <a href="mailto:martin.pfeffer@celox.io">martin.pfeffer@celox.io</a>
 * @see <a href="https://celox.io">https://celox.io</a>
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int PERMISSION_INITIAL_REQUEST = 1;

    private ViewPager mViewPager;
    private DrawerLayout mDrawerLayout;

    private DatabaseCtrls mDatabaseCtrls;
    private boolean mIsExitPressedOnce = false;

    private BroadcastReceiver mGpsStateReceiver = new BroadcastReceiver() {

        private static final String TAG = "GpsStateReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(Const.IE_GPS_STATE);

            Log.i(TAG, "onReceive: state=" + state);
        }
    };

    private BroadcastReceiver mLocationChangedReceiver = new BroadcastReceiver() {

        private static final String TAG = "LocationChangedReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra("lat", 0L);
            double lng = intent.getDoubleExtra("lng", 0L);
            float speed = intent.getFloatExtra("speed", 0F);
            float accuracy = intent.getFloatExtra("accuracy", 0F);
            int ctrls = intent.getIntExtra("ctrl", -1);

            Log.i(TAG, "onReceive: lat=" + lat + ", lng=" + lng + ", speed=" + speed + ", accuracy=" + accuracy + ", ctrls=" + ctrls);
        }
    };

    private BottomNavigationView.OnNavigationItemSelectedListener
            mOnBottomNavigationItemSelectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.bottom_nav_nearby:
                    mViewPager.setCurrentItem(0);
                    setTitle(R.string.nearby);
                    return true;
                case R.id.bottom_nav_map:
                    mViewPager.setCurrentItem(1);
                    setTitle(R.string.map);
                    return true;
                case R.id.bottom_nav_settings:
                    mViewPager.setCurrentItem(2);
                    setTitle(R.string.settings);
                    return true;
                case R.id.bottom_nav_about:
                    mViewPager.setCurrentItem(3);
                    setTitle(R.string.about);
                    return true;
            }
            return false;
        }
    };

    private NavigationView.OnNavigationItemSelectedListener
            mOnNavigationViewItemSelectedListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            item.setChecked(true);
            mDrawerLayout.closeDrawers();

            switch (item.getItemId()) {
                case R.id.drawer_nearby:
                    mViewPager.setCurrentItem(0);
                    return true;
                case R.id.drawer_map:
                    mViewPager.setCurrentItem(1);
                    return true;
                case R.id.drawer_settings:
                    mViewPager.setCurrentItem(2);
                    // TODO: create preference fragment
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ensureRuntimePermissions();

        try {
            mDatabaseCtrls = new DatabaseCtrls(this);
            Log.i(TAG, "onCreate: " + mDatabaseCtrls.getCtrlsCount() + " controls found in database.");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
        }

        final BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnBottomNavigationItemSelectedListener);
        final BottomBarAdapter bottomBarAdapter = new BottomBarAdapter(getSupportFragmentManager());
        NearbyFragment nearbyFragment = new NearbyFragment();
        MapFragment mapFragment = new MapFragment();
        SettingsFragment settingsFragment = new SettingsFragment();
        AboutFragment aboutFragment = new AboutFragment();
        bottomBarAdapter.addFragments(nearbyFragment);
        bottomBarAdapter.addFragments(mapFragment);
        bottomBarAdapter.addFragments(settingsFragment);
        bottomBarAdapter.addFragments(aboutFragment);

        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setAdapter(bottomBarAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            // the guidelines say that we don't want to implement to same navigation twice, so this code should be replaced.
            // See it as a showcase how the selection of the bottom navigation can be kept synchronized.
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                Log.d(TAG, "onPageSelected: " + position);
                switch (position) {
                    case 0:
                        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_nearby);
                        break;
                    case 1:
                        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_map);
                        break;
                    case 2:
                        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_settings);
                        break;
                    case 3:
                        bottomNavigationView.setSelectedItemId(R.id.bottom_nav_about);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) { }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar,
                R.string.nav_drawer_open, R.string.nav_drawer_close
        );
        mDrawerLayout.addDrawerListener(drawerToggle);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        } else {
            Log.w(TAG, "onCreate: Missing ActionBar...");
        }

        drawerToggle.syncState();
        NavigationView navigationView = findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(mOnNavigationViewItemSelectedListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: ");

        stopGpsService();

        unregisterReceiver(mLocationChangedReceiver);
        unregisterReceiver(mGpsStateReceiver);

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer == null) return;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            touchTwiceToExit();
        }
    }

    /**
     * Check double-press preference and close the app if needed.
     */
    private void touchTwiceToExit() {
        if (!AesPrefs.getBoolean(AesConst.TOUCH_TWICE, true)) {
            mIsExitPressedOnce = true;
        }
        if (mIsExitPressedOnce) {
            if (getResources().getBoolean(R.bool.is_dev)) {
                super.onBackPressed();
                return;
            } else {
                super.onBackPressed();
            }
        }

        ToastUtils.toastShort(R.string.touch_twice_to_close);

        mIsExitPressedOnce = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mIsExitPressedOnce = false;
            }
        }, Const.DELAY_ON_BACK_PRESSED);
    }

    private void startAppWithValidPermissions() {
        Log.i(TAG, "startAppWithValidPermissions: starting app...");

        startGpsService();

        registerReceiver(mLocationChangedReceiver, new IntentFilter(Const.FILTER_LOCATION_BROADCAST));
        registerReceiver(mGpsStateReceiver, new IntentFilter(Const.FILTER_GPS_UPDATE));
    }

    private void ensureRuntimePermissions() {
        try {
            AesPrefs.initBoolean(AesConst.PERMISSIONS_GRANTED, false);
        } catch (Exception e) {
            android.util.Log.e(TAG, "ensureRuntimePermissions: ");
        }
        if (!AesPrefs.getBoolean(AesConst.PERMISSIONS_GRANTED, false)) {
            String[] permissions = new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    //                    android.Manifest.permission.READ_PHONE_STATE
            };
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_INITIAL_REQUEST);
        } else {
            startAppWithValidPermissions();
        }
    }

    public void startGpsService() {
        Intent serviceIntent = new Intent(this, GpsService.class);
        startService(serviceIntent);
    }

    private void stopGpsService() {
        Intent serviceIntent = new Intent(this, GpsService.class);
        stopService(serviceIntent);
    }

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    protected void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        try {
            if (requestCode == PERMISSION_INITIAL_REQUEST) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "onRequestPermissionsResult: permissions granted");

                    // store that permissions granted
                    AesPrefs.putBoolean(AesConst.PERMISSIONS_GRANTED, true);
                    startAppWithValidPermissions();
                } else {
                    Log.w(TAG, "onReq-Permission: " + getString(R.string.permissions_missing));
                    ToastUtils.toastLong(R.string.permissions_missing);
                    ensureRuntimePermissions();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onRequestPermissionsResult: " + e.getMessage());
        }
    }

    public DatabaseCtrls getDatabaseCtrls() {
        return mDatabaseCtrls;
    }
}
