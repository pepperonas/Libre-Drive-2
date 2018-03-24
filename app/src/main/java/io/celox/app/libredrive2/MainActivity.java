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
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pepperonas.aespreferences.AesPrefs;
import com.pepperonas.andbasx.base.ToastUtils;

import java.text.MessageFormat;

import io.celox.app.libredrive2.custom.BottomBarAdapter;
import io.celox.app.libredrive2.fragments.AboutFragment;
import io.celox.app.libredrive2.fragments.MapFragment;
import io.celox.app.libredrive2.fragments.NearbyFragment;
import io.celox.app.libredrive2.fragments.SettingsFragment;
import io.celox.app.libredrive2.services.GpsService;
import io.celox.app.libredrive2.utils.AesConst;
import io.celox.app.libredrive2.utils.Const;
import io.celox.app.libredrive2.utils.DatabaseCtrls;
import io.celox.app.libredrive2.utils.Utils;

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

    private DatabaseCtrls mDatabaseCtrls;
    private boolean mIsExitPressedOnce = false;

    private long mLastWarningReceived = System.currentTimeMillis();

    private int mCtrDistanceGrown = 0;

    private Handler mHandlerMainDriver = new Handler();
    private Runnable mMainDriver = new Runnable() {
        @Override
        public void run() {
            try {
                ensureResetWarning();
            } finally {
                mHandlerMainDriver.postDelayed(mMainDriver, Const.INTERVAL_MAIN_DRIVER);
            }
        }
    };

    private void ensureResetWarning() {
        if ((mLastWarningReceived + Const.DELAY_RESET_WARNING) < System.currentTimeMillis()
                || mCtrDistanceGrown >= Const.COUNTER_MOVE_FURTHER_AWAY) {

            TextView tvCtrlDescription = findViewById(R.id.tv_nearby_ctrl_description);
            TextView tvCtrlSpeed = findViewById(R.id.tv_nearby_ctrl_speed);
            TextView tvDistance = findViewById(R.id.tv_nearby_distance);
            LinearLayout llNearby = findViewById(R.id.ll_nearby);
            if (tvCtrlDescription != null) {
                tvCtrlDescription.setText("");
            }
            if (tvCtrlSpeed != null) {
                tvCtrlSpeed.setText("");
            }
            if (tvDistance != null) {
                tvDistance.setText("");
            }
            if (llNearby != null) {
                llNearby.setBackgroundColor(ContextCompat.getColor(this, R.color.green_200));
            }

            View swipeAccessor = findViewById(R.id.swipe_accessor_l);
            if (swipeAccessor != null) {
                swipeAccessor.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.green_200));
            }
        }
    }

    private BroadcastReceiver mGpsStateReceiver = new BroadcastReceiver() {

        private static final String TAG = "GpsStateReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(Const.IE_GPS_STATE);

            Log.i(TAG, "onReceive: state=" + state);
        }
    };

    private BroadcastReceiver mLocationChangedReceiver = new BroadcastReceiver() {
        @SuppressWarnings("unused")
        private static final String TAG = "LocationChangedReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra("lat", 0L);
            double lng = intent.getDoubleExtra("lng", 0L);
            float speedMs = intent.getFloatExtra("speed_ms", 0F);
            int speedKmh = (int) (speedMs * 3.6f);
            float accuracy = intent.getFloatExtra("accuracy", 0F);

            Log.i(TAG, "onReceive: lat=" + lat + ", lng=" + lng + ", speedMs=" + speedMs + ", accuracy=" + accuracy);

            TextView tvGpsSpeed = findViewById(R.id.tv_nearby_gps_speed);
            if (tvGpsSpeed != null) {
                tvGpsSpeed.setText(MessageFormat.format("{0} km/h", speedKmh));
            }
        }
    };

    private BroadcastReceiver mCtrlReceiver = new BroadcastReceiver() {
        @SuppressWarnings("unused")
        private static final String TAG = "CtrlReceiver";

        public long mLastNotificationPlayed = System.currentTimeMillis();
        public long mLastTts500MeterPlayed = System.currentTimeMillis();
        public long mLastTts200MeterPlayed = System.currentTimeMillis();
        public long mLastTtsAtYourPosition = System.currentTimeMillis();
        public int mLastDistance = Integer.MAX_VALUE;

        @Override
        public void onReceive(Context context, Intent intent) {
            mLastWarningReceived = System.currentTimeMillis();

            int ctrlSpeed = intent.getIntExtra("ctrl_speed", 0);
            String ctrlDescription = intent.getStringExtra("ctrl_description");
            int distance = intent.getIntExtra("distance", 0);

            if (distance > mLastDistance) {
                mCtrDistanceGrown++;
            } else {
                mCtrDistanceGrown = 0;
            }

            if (mCtrDistanceGrown >= Const.COUNTER_MOVE_FURTHER_AWAY) {
                Log.i(TAG, "onReceive: moved 3 times further away, will return.");
                mLastDistance = distance;
                return;
            }

            TextView tvCtrlDescription = findViewById(R.id.tv_nearby_ctrl_description);
            TextView tvCtrlSpeed = findViewById(R.id.tv_nearby_ctrl_speed);
            TextView tvDistance = findViewById(R.id.tv_nearby_distance);
            LinearLayout llNearby = findViewById(R.id.ll_nearby);
            if (tvCtrlDescription != null) {
                tvCtrlDescription.setText(ctrlDescription);
            }
            if (tvCtrlSpeed != null) {
                tvCtrlSpeed.setText(MessageFormat.format("{0} km/h", ctrlSpeed));
            }
            if (tvDistance != null) {
                tvDistance.setText(MessageFormat.format("{0} m", distance));
            }
            if (llNearby != null) {
                llNearby.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red_200));
            }

            View swipeAccessor = findViewById(R.id.swipe_accessor_l);
            if (swipeAccessor != null) {
                swipeAccessor.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red_200));
            }

            if (AesPrefs.getBooleanRes(R.string.PLAY_NOTIFICATION, true)) {
                long delta = getResources().getInteger(R.integer.delta_time_between_notifications_sec) * 1000L;
                if ((mLastNotificationPlayed + delta) < System.currentTimeMillis()) {
                    Log.i(TAG, "onReceive: playing notification...");
                    mLastNotificationPlayed = System.currentTimeMillis();
                    Utils.playNotification(getApplicationContext());
                } else {
                    Log.i(TAG, "onReceive: skipping notification...");
                }
            }

            if (AesPrefs.getBooleanRes(R.string.PLAY_TTS, true)) {
                String message = null;
                if (distance < 550 && distance > 450) {
                    long delta = getResources().getInteger(R.integer.delta_time_between_notifications_sec) * 1000L;
                    if ((mLastTts500MeterPlayed + delta) < System.currentTimeMillis()) {
                        if (mLastDistance > distance) {
                            Log.i(TAG, "onReceive: playing tts '500'...");
                            mLastTts500MeterPlayed = System.currentTimeMillis();
                            message = getString(R.string.warning_in) + " 500 " + getString(R.string.meters) + ".";
                        }
                    }
                } else if (distance < 250 && distance > 150) {
                    long delta = getResources().getInteger(R.integer.delta_time_between_notifications_sec) * 1000L;
                    if ((mLastTts200MeterPlayed + delta) < System.currentTimeMillis()) {
                        if (mLastDistance > distance) {
                            Log.i(TAG, "onReceive: playing tts '200'...");
                            mLastTts200MeterPlayed = System.currentTimeMillis();
                            message = getString(R.string.warning_in) + " 200 " + getString(R.string.meters) + ".";
                        }
                    }
                } else if (distance < 100 && distance > 40) {
                    long delta = getResources().getInteger(R.integer.delta_time_between_notifications_sec) * 1000L;
                    if ((mLastTtsAtYourPosition + delta) < System.currentTimeMillis()) {
                        if (mLastDistance > distance) {
                            Log.i(TAG, "onReceive: playing tts '200'...");
                            mLastTtsAtYourPosition = System.currentTimeMillis();
                            message = getString(R.string.warning_at_your_position) + ".";
                        }
                    }
                }

                if (message != null) {
                    try {
                        mTextToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
                    } catch (Exception e) {
                        Log.e(TAG, "onReceive: ", e);
                    }
                }

                mLastDistance = distance;
            }
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

    private TextToSpeech mTextToSpeech;

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

        mTextToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.i(TAG, "onInit: status=" + status);
            }
        });

        startMainDriver();
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
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy: ");

        stopGpsService();

        stopMainDriver();

        unregisterReceiver(mLocationChangedReceiver);
        unregisterReceiver(mCtrlReceiver);
        unregisterReceiver(mGpsStateReceiver);

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        touchTwiceToExit();
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
        registerReceiver(mCtrlReceiver, new IntentFilter(Const.FILTER_WARNING_CTRL));
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

    void startMainDriver() {
        mMainDriver.run();
    }

    void stopMainDriver() {
        mHandlerMainDriver.removeCallbacks(mMainDriver);
    }

    public DatabaseCtrls getDatabaseCtrls() {
        return mDatabaseCtrls;
    }
}
