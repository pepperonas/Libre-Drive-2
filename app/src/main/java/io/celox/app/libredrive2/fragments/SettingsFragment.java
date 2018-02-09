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

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.pepperonas.aespreferences.AesPrefs;
import com.pepperonas.andbasx.base.ToastUtils;

import io.celox.app.libredrive2.MainActivity;
import io.celox.app.libredrive2.R;
import io.celox.app.libredrive2.dialogs.DialogChangelog;
import io.celox.app.libredrive2.utils.AesConst;
import io.celox.app.libredrive2.utils.Const;
import io.celox.app.libredrive2.utils.Utils;

/**
 * @author Martin Pfeffer
 * <a href="mailto:martin.pfeffer@celox.io">martin.pfeffer@celox.io</a>
 * @see <a href="https://celox.io">https://celox.io</a>
 */
public class SettingsFragment extends
        com.github.machinarius.preferencefragment.PreferenceFragment implements
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        int horizontalMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        int verticalMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
        int topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                (int) getResources().getDimension(R.dimen.activity_vertical_margin), getResources().getDisplayMetrics());

        if (v != null) {
            v.setPadding(horizontalMargin, topMargin, horizontalMargin, verticalMargin);
        }
        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: ");

        addPreferencesFromResource(R.xml.fragment_preference);

        addPrefIcons();
        addChangelogPref();
        addBuildPref();

        findPreference(getString(R.string.TOUCH_TWICE_TO_EXIT)).setOnPreferenceClickListener(this);

        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference(getString(R.string.PREF_CAT_LOCKED));
        preferenceCategory.setEnabled(false);

        //        AesPrefs.setChangeListenersOnPreferences(this,
        //                preference);
        //

        updateSummaries();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        MainActivity main = (MainActivity) getActivity();
        if (main != null) {
            main.setTitle(getString(R.string.settings));
        } else {
            Log.w(TAG, "onActivityCreated: missing activity.");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        updateSummaries();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause: ");

        super.onPause();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(getString(R.string.TOUCH_TWICE_TO_EXIT))) {
            AesPrefs.putBoolean(AesConst.TOUCH_TWICE, ((CheckBoxPreference) preference).isChecked());
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, final Object newValue) {
        //       if (preference.getKey().equals(getString(R.string.OBD_SAMPLING_RATE))) {
        //            AesPrefs.put(AesConst.OBD_SAMPLING_RATE, (String) newValue);
        //        }
        updateSummaries();

        return true;
    }

    private void updateSummaries() {
        // TODO: 09.02.18 03:55 implement
    }

    private void addPrefIcons() {
        if (getActivity() == null) {
            Log.w(TAG, "addPrefIcons: missing activity.");
            return;
        }

        int color = R.color.indigo_700;
        findPreference(getString(R.string.CHANGELOG)).setIcon(new IconicsDrawable(getActivity(),
                CommunityMaterial.Icon.cmd_math_compass).colorRes(color).sizeDp(Const.NAV_DRAWER_ICON_SIZE));
        findPreference(getString(R.string.BUILD_VERSION)).setIcon(new IconicsDrawable(getActivity(),
                CommunityMaterial.Icon.cmd_leaf).colorRes(color).sizeDp(Const.NAV_DRAWER_ICON_SIZE));
    }

    private void addChangelogPref() {
        Preference p = findPreference(getString(R.string.CHANGELOG));
        p.setTitle(R.string.pref_title_changelog);

        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new DialogChangelog(getContext());
                return true;
            }
        });
    }

    private void addBuildPref() {
        Preference p = findPreference(getString(R.string.BUILD_VERSION));
        p.setTitle(R.string.pref_title_build_version);

        p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                processHitOnBuild();
                return true;
            }
        });

        if (getContext() != null) {
            String buildVersion = Utils.getBuildVersion(getContext());
            p.setSummary(buildVersion);
        }
    }

    private double mLastClickedBuild = 0;
    private int mHiddenCounter = 0;

    private void processHitOnBuild() {

        if ((mLastClickedBuild + 1000) > System.currentTimeMillis() || mLastClickedBuild == 0) {
            mHiddenCounter++;
        } else {
            mHiddenCounter = 0;
        }
        if (mHiddenCounter == 7) {
            ToastUtils.toastLong("nice!");
            unlockLockedCategory();
        }

        mHiddenCounter = mHiddenCounter > 7 ? 0 : mHiddenCounter;
        mLastClickedBuild = System.currentTimeMillis();
    }

    private void unlockLockedCategory() {
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference(getString(R.string.PREF_CAT_LOCKED));
        preferenceCategory.setEnabled(true);
    }

}

