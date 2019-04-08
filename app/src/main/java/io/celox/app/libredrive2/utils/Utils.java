/*
 * Copyright (c) 2019 Martin Pfeffer
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

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.celox.app.libredrive2.BuildConfig;
import io.celox.app.libredrive2.R;

/**
 * @author Martin Pfeffer
 * <a href="mailto:martin.pfeffer@celox.io">martin.pfeffer@celox.io</a>
 * @see <a href="https://celox.io">https://celox.io</a>
 */
public class Utils {

    public static String getBuildVersion(@NonNull Context context) {
        String summary = BuildConfig.VERSION_NAME + "-" + context.getString(R.string.flv_variant);
        Date date = new Date(BuildConfig.APP_CREATED);
        SimpleDateFormat sdf = new SimpleDateFormat("yy.MM.dd.HH.mm.ss", Locale.GERMANY);
        return summary + "-" + sdf.format(date);
    }

    public static void playNotification(@NonNull Context context) {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(context, notification);
        r.play();
    }
}
