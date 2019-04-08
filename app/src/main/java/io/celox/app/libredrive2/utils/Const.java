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

/**
 * @author Martin Pfeffer
 * <a href="mailto:martin.pfeffer@celox.io">martin.pfeffer@celox.io</a>
 * @see <a href="https://celox.io">https://celox.io</a>
 */
public class Const {

    public static final String DB_NAME = "ctrls.db";

    public static final String FILTER_GPS_UPDATE = "filter_gps_update";

    public static final String FILTER_LOCATION_BROADCAST = "filter_location_broadcast";
    public static final String IE_GPS_STATE = "ie_gps_state";

    public static final String FILTER_WARNING_CTRL = "filter_warning_ctrl";

    public static final int CTRL_WARN_DISTANCE_IN_METERS = 500;
    public static final long DELAY_ON_BACK_PRESSED = 2000;

    public static final float MAP_CTRLS_CIRCLE_LINE_WIDTH = 5f;
    public static final int MAX_CTRLS_IN_MAP = 500;
    public static final double MAP_MARKER_RADIUS = 30d;
    public static final int NAV_DRAWER_ICON_SIZE = 24;
    public static final long INTERVAL_MAIN_DRIVER = 1000;
    public static final long DELAY_RESET_WARNING = 60 * 1000;
    public static final int COUNTER_MOVE_FURTHER_AWAY = 3;
}
