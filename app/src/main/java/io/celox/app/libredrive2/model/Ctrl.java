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

package io.celox.app.libredrive2.model;

import android.support.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

/**
 * The type Ctrl.
 *
 * @author Martin Pfeffer <a href="mailto:martin.pfeffer@celox.io">martin.pfeffer@celox.io</a>
 * @see <a href="https://celox.io">https://celox.io</a>
 */
public class Ctrl {

    @NonNull
    private LatLng latLng;
    private int speed;
    @NonNull
    private String description;

    /**
     * Instantiates a new Ctrl.
     *
     * @param latLng      the lat lng
     * @param speed       the speed
     * @param description the description
     */
    public Ctrl(@NonNull LatLng latLng, int speed, @NonNull String description) {
        this.latLng = latLng;
        this.speed = speed;
        this.description = description;
    }

    /**
     * Gets lat lng.
     *
     * @return the lat lng
     */
    @NonNull
    public LatLng getLatLng() {
        return latLng;
    }

    /**
     * Sets lat lng.
     *
     * @param latLng the lat lng
     */
    public void setLatLng(@NonNull LatLng latLng) {
        this.latLng = latLng;
    }

    /**
     * Gets speed.
     *
     * @return the speed
     */
    public int getSpeed() {
        return speed;
    }

    /**
     * Sets speed.
     *
     * @param speed the speed
     */
    public void setSpeed(int speed) {
        this.speed = speed;
    }

    /**
     * Gets description.
     *
     * @return the description
     */
    @NonNull
    public String getDescription() {
        return description;
    }

    /**
     * Sets description.
     *
     * @param description the description
     */
    public void setDescription(@NonNull String description) {
        this.description = description;
    }
}
