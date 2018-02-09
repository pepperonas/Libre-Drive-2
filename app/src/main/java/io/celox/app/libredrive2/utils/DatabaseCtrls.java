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

package io.celox.app.libredrive2.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.celox.app.libredrive2.model.Ctrl;

/**
 * @author Martin Pfeffer
 * <a href="mailto:martin.pfeffer@celox.io">martin.pfeffer@celox.io</a>
 * @see <a href="https://celox.io">https://celox.io</a>
 */
public class DatabaseCtrls extends SQLiteAssetHelper {

    @SuppressWarnings("unused")
    private static final String TAG = "DatabaseCtrls";

    private static final int MIN_LAT = 0, MIN_LNG = 1, MAX_LAT = 2, MAX_LNG = 3;

    private static final String TABLE_CTRLS = "ctrls";

    private Context mContext;

    public DatabaseCtrls(Context context) {
        super(context, Const.DB_NAME, null, 1);
        mContext = context;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /**
     * Is close to ctrl int.
     *
     * @param latitude  the latitude
     * @param longitude the longitude
     * @param range     the range
     * @return the int
     */
    public int isCloseToCtrl(double latitude, double longitude, int range) {
        if (mContext == null) {
            return -1;
        }

        double area[] = getSearchArea(latitude, longitude, range);
        String where = "la < " + area[MAX_LAT] + " AND la > " + area[MIN_LAT] + " AND lo < " + area[MAX_LNG] + " AND lo > " + area[MIN_LNG];

        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_CTRLS + " WHERE " + where, null);
        Log.i(TAG, "Warnings: " + cursor.getCount());

        if (cursor.getCount() > 0) {
            try {
                cursor.moveToPosition(-1);
                cursor.moveToNext();
                return cursor.getInt(4);
            } catch (Exception e) {
                Log.e(TAG, "isCloseToCtrl: " + e.getMessage());
                return -1;
            } finally {
                cursor.close();
            }
        }
        return -1;
    }

    /**
     * Gets ctrls in area.
     *
     * @param latitude  the latitude
     * @param longitude the longitude
     * @param range     the range
     * @return the ctrls in area
     */
    public List<Ctrl> getCtrlsInArea(double latitude, double longitude, int range) {
        List<Ctrl> ctrlList = new ArrayList<>();

        double area[] = getSearchArea(latitude, longitude, range);
        String where = "la < " + area[MAX_LAT] + " AND la > " + area[MIN_LAT] + " AND lo <" + area[MAX_LNG] + " AND lo >" + area[MIN_LNG];

        Log.i(TAG, "getCtrlsInArea: " + range);

        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM " + TABLE_CTRLS + " WHERE " + where, null);
        Log.i(TAG, "Warnings: " + cursor.getCount());

        if (cursor.getCount() > 0) {
            try {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    ctrlList.add(new Ctrl(new LatLng(cursor.getDouble(1), cursor.getDouble(2)),
                            cursor.getInt(4), cursor.getString(8)));
                }
            } catch (Exception e) {
                Log.e(TAG, "isCloseToCtrl: " + e.getMessage());

            }
        }
        Log.i(TAG, "getCtrlsInArea: " + ctrlList.toString());

        return ctrlList;
    }

    private double[] getSearchArea(final double latitude, final double longitude, final int distanceInMeters) {
        double[] area = new double[4];
        final double latRadian = Math.toRadians(latitude);

        final double degLatKm = 110.574235;
        final double degLngKm = 110.572833 * Math.cos(latRadian);
        final double deltaLat = distanceInMeters / 1000.0 / degLatKm;
        final double deltaLong = distanceInMeters / 1000.0 / degLngKm;

        final double minLat = latitude - deltaLat;
        final double minLng = longitude - deltaLong;
        final double maxLat = latitude + deltaLat;
        final double maxLng = longitude + deltaLong;

        area[0] = minLat;
        area[1] = minLng;
        area[2] = maxLat;
        area[3] = maxLng;

        return area;
    }

    public Cursor getCtrlById(int id) {
        String where = "id = " + id + "";
        SQLiteDatabase db = getReadableDatabase();
        return db.query(TABLE_CTRLS, null, where, null, null, null, null);
    }

    public int getCtrlsCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_CTRLS, null, null, null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public void insertCtrls(final Iterable<HashMap<String, String>> args) {
        final SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        //        db.delete(TABLE_CTRLS, null, null);

        for (AbstractMap<String, String> qvs : args) {
            ContentValues values = new ContentValues();
            values.put("id", qvs.get("id"));
            values.put("la", qvs.get("la"));
            values.put("lo", qvs.get("lo"));
            values.put("ve", qvs.get("ve"));
            values.put("sp", qvs.get("sp"));
            values.put("ty", qvs.get("ty"));
            values.put("co", qvs.get("co"));
            values.put("ne", qvs.get("ne"));
            values.put("st", qvs.get("st"));
            db.insert(TABLE_CTRLS, null, values);
        }

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    /**
     * Aufgerufen, wenn Datenbank nicht mehr benötigt wird.
     */
    public void closeDatabase() {
        close();
    }
}
