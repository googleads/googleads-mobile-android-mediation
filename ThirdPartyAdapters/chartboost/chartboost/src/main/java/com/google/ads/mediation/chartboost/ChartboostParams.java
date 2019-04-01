// Copyright 2016 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.chartboost;

import com.chartboost.sdk.CBLocation;
import com.chartboost.sdk.Chartboost.CBFramework;

/**
 * The {@link ChartboostParams} class is used to send network parameters and mediation/network
 * extras from {@link ChartboostAdapter} and {@link ChartboostMediationAdapter}
 * to {@link ChartboostSingleton}.
 */
public class ChartboostParams {

    /**
     * Chartboost App ID.
     */
    private String appId;

    /**
     * Chartboost App Signature.
     */
    private String appSignature;

    /**
     * Charboost location used to load ads.
     */
    private String cbLocation;

    /**
     * Chartboost readable framework.
     */
    private CBFramework cbFramework;

    /**
     * The version name for {@link #cbFramework}.
     */
    private String cbFrameworkVersion;

    /**
     * Default constructor, sets a default value for {@link #cbLocation}.
     */
    public ChartboostParams() {
        this.cbLocation = CBLocation.LOCATION_DEFAULT;
    }

    /**
     * @return {@link #appId}.
     */
    public String getAppId() {
        return appId;
    }

    /**
     * @param appId set to {@link #appId}.
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * @return {@link #appSignature}.
     */
    public String getAppSignature() {
        return appSignature;
    }

    /**
     * @param appSignature set to {@link #appSignature}
     */
    public void setAppSignature(String appSignature) {
        this.appSignature = appSignature;
    }

    /**
     * @return {@link #cbLocation}.
     */
    public String getLocation() {
        return cbLocation;
    }

    /**
     * @param location set to {@link #cbLocation}.
     */
    public void setLocation(String location) {
        this.cbLocation = location;
    }

    /**
     * @return {@link #cbFramework}.
     */
    public CBFramework getFramework() {
        return cbFramework;
    }

    /**
     * @param framework set to {@link #cbFramework}.
     */
    public void setFramework(CBFramework framework) {
        this.cbFramework = framework;
    }

    /**
     * @return {@link #cbFrameworkVersion}.
     */
    public String getFrameworkVersion() {
        return cbFrameworkVersion;
    }

    /**
     * @param version set to {@link #cbFrameworkVersion}.
     */
    public void setFrameworkVersion(String version) {
        this.cbFrameworkVersion = version;
    }
}
