// Copyright 2022 Google LLC
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

package com.google.ads.mediation.pangle;

import com.bytedance.sdk.openadsdk.api.PAGConstant.PAGChildDirectedType;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.RequestConfiguration.TagForChildDirectedTreatment;

/** Stores the COPPA setting and configures it on the Pangle SDK. */
public class PanglePrivacyConfig {

  private static int coppa = -1;

  private final PangleSdkWrapper pangleSdkWrapper;

  public PanglePrivacyConfig(PangleSdkWrapper pangleSdkWrapper) {
    this.pangleSdkWrapper = pangleSdkWrapper;
  }

  /**
   * Sets the COPPA setting in Pangle SDK.
   *
   * <p>Also, caches the COPPA setting in a static field.
   *
   * @param coppa an {@code Integer} value that indicates whether the app should be treated as
   *     child-directed for purposes of the COPPA. {@link
   *     RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE} means true. {@link
   *     RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE} means false. {@link
   *     RequestConfiguration#TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED} means unspecified.
   */
  public void setCoppa(@TagForChildDirectedTreatment int coppa) {
    switch (coppa) {
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE:
        if (pangleSdkWrapper.isInitSuccess()) {
          pangleSdkWrapper.setChildDirected(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_CHILD);
        }
        PanglePrivacyConfig.coppa = 1;
        break;
      case RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE:
        if (pangleSdkWrapper.isInitSuccess()) {
          pangleSdkWrapper.setChildDirected(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_NON_CHILD);
        }
        PanglePrivacyConfig.coppa = 0;
        break;
      default:
        if (pangleSdkWrapper.isInitSuccess()) {
          pangleSdkWrapper.setChildDirected(PAGChildDirectedType.PAG_CHILD_DIRECTED_TYPE_DEFAULT);
        }
        PanglePrivacyConfig.coppa = -1;
        break;
    }
  }

  public static int getCoppa() {
    return coppa;
  }
}
