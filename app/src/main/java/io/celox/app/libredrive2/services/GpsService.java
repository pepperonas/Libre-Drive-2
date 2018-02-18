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

package io.celox.app.libredrive2.services;

import android.Manifest.permission;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.pepperonas.andbasx.base.ToastUtils;
import com.pepperonas.jbasx.math.GeographicUtils;

import java.util.ArrayList;
import java.util.List;

import io.celox.app.libredrive2.MainActivity;
import io.celox.app.libredrive2.R;
import io.celox.app.libredrive2.model.Ctrl;
import io.celox.app.libredrive2.utils.Const;
import io.celox.app.libredrive2.utils.DatabaseCtrls;

/**
 * @author Martin Pfeffer
 * <a href="mailto:martin.pfeffer@celox.io">martin.pfeffer@celox.io</a>
 * @see <a href="https://celox.io">https://celox.io</a>
 */
public class GpsService extends Service {

    private static final String TAG = "GpsService";

    public static final long GPS_UPDATE_FREQUENCY = 800L;
    private static final long GPS_OFFSET_TIME = 10L;

    private static final int START_FOREGROUND_ID = 1;

    private DatabaseCtrls mDatabaseCtrls;
    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");

        mDatabaseCtrls = new DatabaseCtrls(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ToastUtils.toastLongFromBackground(R.string.permission_required_location);
            Intent intentGps = new Intent(Const.FILTER_GPS_UPDATE);
            intentGps.putExtra(Const.IE_GPS_STATE, "CONNECTION FAILED (missing permissions)");
            sendBroadcast(intentGps);
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(GPS_UPDATE_FREQUENCY);
        locationRequest.setFastestInterval(GPS_UPDATE_FREQUENCY - GPS_OFFSET_TIME);

        mFusedLocationClient.requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    Log.v(TAG, "onSuccess: lat=" + location.getLatitude() + " lng=" + location.getLongitude());
                }
            }
        });
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                List<Ctrl> ctrls = new ArrayList<>();
                if (mDatabaseCtrls != null) {
                    ctrls = mDatabaseCtrls.getCtrlsInArea(location.getLatitude(), location.getLongitude(),
                            Const.CTRL_WARN_DISTANCE_IN_METERS);
                    Log.w(TAG, "onLocationChanged: ctrls=" + ctrls.size());
                }

                Intent gpsLocation = new Intent(Const.FILTER_LOCATION_BROADCAST);
                gpsLocation.putExtra("lat", location.getLatitude());
                gpsLocation.putExtra("lng", location.getLongitude());
                gpsLocation.putExtra("speed_ms", location.getSpeed());
                gpsLocation.putExtra("accuracy", location.getAccuracy());
                sendBroadcast(gpsLocation);

                if (ctrls.size() > 0) {
                    Ctrl closestCtrl = null;
                    int minDist = Integer.MAX_VALUE;
                    for (Ctrl ctrl : ctrls) {
                        int tmpDist = (int) GeographicUtils.distanceBetweenGeoPositionsInMeters(
                                ctrl.getLatLng().latitude, ctrl.getLatLng().longitude,
                                location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "onLocationChanged: tmpDist=" + tmpDist);
                        if (tmpDist < minDist) {
                            minDist = tmpDist;
                            closestCtrl = ctrl;
                            Log.i(TAG, "onLocationChanged: found new closest ctrl: " + closestCtrl.toString());
                        }
                    }

                    if (closestCtrl != null) {
                        Intent ctrlWarning = new Intent(Const.FILTER_WARNING_CTRL);
                        ctrlWarning.putExtra("ctrl_speed", closestCtrl.getSpeed());
                        ctrlWarning.putExtra("ctrl_description", closestCtrl.getDescription());
                        ctrlWarning.putExtra("distance", minDist);
                        sendBroadcast(ctrlWarning);
                    } else {
                        Log.w(TAG, "onLocationChanged: closestCtrl is null, this should not happen..");
                    }
                }
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        String channelId = "io.celox.libredrive2";
        String channelName = "GPS channel";
        NotificationChannel notificationChannel;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
            notificationChannel.enableLights(false);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.BADGE_ICON_NONE);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(notificationChannel);
            }
        }

        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        Notification notification;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notification = new Notification.Builder(getApplicationContext(), channelId)
                    .setContentTitle(getString(R.string.gps_service_notification_title))
                    .setChannelId(channelId)
                    .setContentText(getString(R.string.service_notification_content))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(icon)
                    .build();
        } else {
            notification = new Notification.Builder(getApplicationContext())
                    .setContentTitle(getString(R.string.gps_service_notification_title))
                    .setContentText(getString(R.string.service_notification_content))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(icon)
                    .build();
        }

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

        startForeground(START_FOREGROUND_ID, notification);

        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");

        mFusedLocationClient.removeLocationUpdates(mLocationCallback);

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
