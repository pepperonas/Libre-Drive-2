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
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.pepperonas.aespreferences.AesPrefs;
import com.pepperonas.andbasx.base.ToastUtils;

import io.celox.app.libredrive2.MainActivity;
import io.celox.app.libredrive2.R;
import io.celox.app.libredrive2.utils.AesConst;
import io.celox.app.libredrive2.utils.Const;
import io.celox.app.libredrive2.utils.DatabaseCtrls;

/**
 * @author Martin Pfeffer
 * <a href="mailto:martin.pfeffer@celox.io">martin.pfeffer@celox.io</a>
 * @see <a href="https://celox.io">https://celox.io</a>
 */
public class GpsService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "GpsService";

    public static final long GPS_UPDATE_FREQUENCY = 800L;
    private static final long GPS_OFFSET_TIME = 10L;

    private static final int START_FOREGROUND_ID = 1;

    private GoogleApiClient mGoogleApiClient;
    private DatabaseCtrls mDatabaseCtrls;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (AesPrefs.getBoolean(AesConst.CTRLS_DATABASE_LOADED, false)) {
            mDatabaseCtrls = new DatabaseCtrls(this);
        }

        mGoogleApiClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        String channelId = "io.celox.libredrive";
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
            notification = new Notification.Builder(getApplicationContext())
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

        // disconnecting the client invalidates it
        mGoogleApiClient.disconnect();

        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "onLocationChanged: ");

        if (location != null) {
            int ctrl = -1;
            if (mDatabaseCtrls != null) {
                ctrl = mDatabaseCtrls.isCloseToCtrl(location.getLatitude(), location.getLongitude(), Const.CTRL_WARN_DISTANCE_IN_METERS);
                Log.w(TAG, "onCreate: " + ctrl);
            }

            Intent gpsLocation = new Intent(Const.FILTER_LOCATION_BROADCAST);
            gpsLocation.putExtra("lat", location.getLatitude());
            gpsLocation.putExtra("lng", location.getLongitude());
            gpsLocation.putExtra("speed", location.getSpeed());
            gpsLocation.putExtra("accuracy", location.getAccuracy());
            gpsLocation.putExtra("ctrl", ctrl);
            sendBroadcast(gpsLocation);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: ");

        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // missing permission to get location
            ToastUtils.toastLongFromBackground(R.string.permission_required_location);
            sendBroadcastGpsState("CONNECTION FAILED (missing permissions)");
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(GPS_UPDATE_FREQUENCY);
        locationRequest.setFastestInterval(GPS_UPDATE_FREQUENCY - GPS_OFFSET_TIME);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);

        sendBroadcastGpsState("CONNECTED");
    }

    private void sendBroadcastGpsState(String which) {
        Intent intentGps = new Intent(Const.FILTER_GPS_UPDATE);
        intentGps.putExtra(Const.IE_GPS_STATE, which);
        sendBroadcast(intentGps);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: " + connectionResult.getErrorMessage() + " | " + connectionResult.toString());
        sendBroadcastGpsState("CONNECTION FAILED");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
