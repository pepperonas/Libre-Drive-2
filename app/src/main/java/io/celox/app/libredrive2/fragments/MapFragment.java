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

package io.celox.app.libredrive2.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

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
public class MapFragment extends android.support.v4.app.Fragment implements OnMapReadyCallback {

    @SuppressWarnings("unused")
    private static final String TAG = "MapFragment";

    private static final int MENU_MAP_LOCK = 2;
    private static final int MENU_ENABLE_TRAFFIC = 3;

    private GoogleMap mGoogleMap;

    private DatabaseCtrls mDatabaseCtrls;

    private LatLng mLastUserPosition = null;

    private boolean mIsRepositioning = true;

    private int mCtr = 0;

    private BroadcastReceiver mLocationChangedReceiver = new BroadcastReceiver() {
        public boolean mUpdatePositionOnce = true;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (getActivity() == null) {
                Log.w(TAG, "onReceive: missing activity.");
                return;
            }
            try {
                if (mGoogleMap == null) return;

                if (mDatabaseCtrls == null) {
                    mDatabaseCtrls = ((MainActivity) getActivity()).getDatabaseCtrls();
                }

                double lat = intent.getDoubleExtra("lat", 0d);
                double lng = intent.getDoubleExtra("lng", 0d);
                if (lat != 0d && lng != 0d) {
                    mLastUserPosition = new LatLng(lat, lng);
                }

                int warn = intent.getIntExtra("ctrl", -1);

                if (warn != -1) {
                    Log.i(TAG, "onReceive: Ctrl found!");
                }

                if (mIsRepositioning || mUpdatePositionOnce) {
                    mUpdatePositionOnce = false;
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 16));

                    if (mDatabaseCtrls == null) return;

                    List<Ctrl> ctrlList = mDatabaseCtrls.getCtrlsInArea(lat, lng, 1500);
                    if (getResources().getBoolean(R.bool.show_ctrls_in_map)) {
                        for (Ctrl ctrl : ctrlList) {
                            String snippet = getSnippet(ctrl);
                            mGoogleMap.addMarker(new MarkerOptions()
                                    .position(ctrl.getLatLng())
                                    .title(ctrl.getDescription())
                                    .snippet(snippet));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "mLocationChangedReceiver: ", e);
            }
        }
    };

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fg_map, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getActivity() != null) {
            ((MainActivity) getActivity()).startGpsService();
        } else {
            Log.w(TAG, "onActivityCreated: missing activity.");
        }
        getActivity().registerReceiver(mLocationChangedReceiver, new IntentFilter(Const.FILTER_LOCATION_BROADCAST));

        SupportMapFragment mapFrag = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

        mIsRepositioning = getActivity().getResources().getBoolean(R.bool.update_position_on_map_automatically);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_MAP_LOCK, 2, R.string.menu_map_unlock);
        menu.add(0, MENU_ENABLE_TRAFFIC, 3, R.string.menu_traffic_disable);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        Log.i(TAG, "onPrepareOptionsMenu: ");

        try {
            menu.findItem(MENU_MAP_LOCK).setTitle(mIsRepositioning ? R.string.menu_map_lock : R.string.menu_map_unlock);
        } catch (Exception e) {
            Log.e(TAG, "onPrepareOptionsMenu: Missing option 'Stop tracking'.");
        }

        try {
            MenuItem trafficItem = menu.findItem(MENU_ENABLE_TRAFFIC);
            trafficItem.setTitle(trafficItem.getTitle().toString().equals(getString(R.string.menu_traffic_enable)) ?
                    R.string.menu_traffic_disable : R.string.menu_traffic_enable);
        } catch (Exception e) {
            Log.e(TAG, "onPrepareOptionsMenu: Missing option 'Enable traffic'.");
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_MAP_LOCK: {
                mIsRepositioning = !mIsRepositioning;
                break;
            }
            case MENU_ENABLE_TRAFFIC: {
                if (mGoogleMap != null) {
                    mGoogleMap.setTrafficEnabled(item.getTitle().equals(getString(R.string.menu_traffic_enable)));
                }
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            getActivity().registerReceiver(mLocationChangedReceiver, new IntentFilter(Const.FILTER_LOCATION_BROADCAST));
        } else {
            Log.w(TAG, "onResume: missing activity.");
        }
    }

    @Override
    public void onPause() {
        try {
            if (getActivity() != null) {
                getActivity().unregisterReceiver(mLocationChangedReceiver);
            } else {
                Log.w(TAG, "onPause: missing activity.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error on unregister receiver.");
        }

        super.onPause();
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        if (getActivity() == null) {
            Log.w(TAG, "onMapReady: can't set up map - missing activity.");
            return;
        }

        mGoogleMap = googleMap;

        mGoogleMap.setTrafficEnabled(true);

        if (isAdded()) {
            if (mDatabaseCtrls == null) {
                mDatabaseCtrls = ((MainActivity) getActivity()).getDatabaseCtrls();
            }

            mGoogleMap.setOnMarkerClickListener(
                    new GoogleMap.OnMarkerClickListener() {
                        boolean doNotMoveCameraToCenterMarker = true;

                        public boolean onMarkerClick(Marker marker) {
                            marker.showInfoWindow();
                            return doNotMoveCameraToCenterMarker;
                        }
                    });

            mGoogleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                @Override
                public void onCameraIdle() {
                    fireOnce();
                }
            });
        }
    }

    private void fireOnce() {
        android.util.Log.i(TAG, "fireOnce(" + (mCtr++) + ")");
        mGoogleMap.clear();
        updateMap();
    }

    private void updateMap() {
        if (getActivity() == null) {
            Log.w(TAG, "updateMap: missing activity.");
            return;
        }
        try {
            if (mLastUserPosition != null) {
                // show users marker
                LatLng latLng = new LatLng(mLastUserPosition.latitude, mLastUserPosition.longitude);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(getString(R.string.your_position));
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                mGoogleMap.addMarker(markerOptions);
            }

            // show ctrls circle
            final Circle circle = mGoogleMap.addCircle(new CircleOptions()
                    .center(mGoogleMap.getCameraPosition().target)
                    .radius(Const.MAP_MARKER_RADIUS)
                    .strokeWidth(Const.MAP_CTRLS_CIRCLE_LINE_WIDTH)
                    .strokeColor(ContextCompat.getColor(getActivity(), R.color.map_ctrls_circle))
            );

            // animate the camera
            float z2 = mGoogleMap.getCameraPosition().zoom;
            double base = Const.MAP_MARKER_RADIUS / zoomToDistFactor(19d);
            int newR = (int) (base * zoomToDistFactor(z2));
            circle.setRadius(newR);

            android.util.Log.i(TAG, "updateMap: " + newR);

            if (newR > 70000) {
                newR = 70000;
            }

            // add ctrls marker
            List<Ctrl> ctrlList = mDatabaseCtrls.getCtrlsInArea(
                    mGoogleMap.getCameraPosition().target.latitude,
                    mGoogleMap.getCameraPosition().target.longitude,
                    newR);

            getActivity().setTitle(getString(R.string.map) + " (" + ctrlList.size() + ")");

            int added = 0;
            for (Ctrl ctrl : ctrlList) {
                if (added > Const.MAX_CTRLS_IN_MAP) {
                    return;
                }
                added++;

                String snippet = getSnippet(ctrl);
                if (getResources().getBoolean(R.bool.show_ctrls_in_map)) {
                    mGoogleMap.addMarker(new MarkerOptions()
                            .position(ctrl.getLatLng())
                            .title(ctrl.getDescription())
                            .snippet(snippet));
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private String getSnippet(Ctrl ctrl) {
        if (ctrl.getSpeed() == 0) return null;
        return getString(R.string.max) + " " + ctrl.getSpeed() + getString(R.string.kmh);
    }

    private double zoomToDistFactor(double z) {
        return Math.pow(2, 8 - z) / 1.6446;
    }

}