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

package com.google.ads.mediation.unity;

import com.unity3d.ads.mediation.IUnityAdsExtendedListener;

/**
 * An {@link IUnityAdsExtendedListener} used to mediate callbacks and data between
 * {@link UnitySingleton} and {@link UnityAdapter}.
 */
public interface UnityAdapterDelegate extends IUnityAdsExtendedListener {

    /**
     * @return the Unity Ads Placement ID associated with the adapter that implements this
     * interface.
     */
    String getPlacementId();
}
