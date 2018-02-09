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

package io.celox.app.libredrive2.dialogs;

import android.content.Context;

import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.pepperonas.materialdialog.MaterialDialog;
import com.pepperonas.materialdialog.model.Changelog;
import com.pepperonas.materialdialog.model.ReleaseInfo;

import java.util.ArrayList;
import java.util.List;

import io.celox.app.libredrive2.R;
import io.celox.app.libredrive2.utils.Const;

/**
 * @author Martin Pfeffer
 * <a href="mailto:martin.pfeffer@celox.io">martin.pfeffer@celox.io</a>
 * @see <a href="https://celox.io">https://celox.io</a>
 */
public class DialogChangelog {

    @SuppressWarnings("unused")
    private static final String TAG = "DialogChangelog";

    public DialogChangelog(Context context) {

        new MaterialDialog.Builder(context)
                .title(R.string.dlg_title_changelog)
                .icon(new IconicsDrawable(context, CommunityMaterial.Icon.cmd_math_compass)
                        .colorRes(R.color.indigo_700)
                        .sizeDp(Const.NAV_DRAWER_ICON_SIZE))
                .changelogDialog(getChangelog(), context.getString(R.string.bullet_release_info))
                .positiveText(R.string.ok)
                .show();
    }

    private List<Changelog> getChangelog() {
        List<Changelog> changes = new ArrayList<>();

        changes.add(new Changelog(
                "0.0.1-beta", "2018-02-09",
                new ReleaseInfo(
                        "Initial release"
                )));

        return changes;
    }
}
