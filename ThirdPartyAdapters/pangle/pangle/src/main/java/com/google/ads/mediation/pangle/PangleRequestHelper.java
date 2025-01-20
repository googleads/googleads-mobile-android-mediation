// Copyright 2023 Google LLC
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

import android.text.TextUtils;
import androidx.annotation.VisibleForTesting;
import com.bytedance.sdk.openadsdk.api.PAGRequest;
import com.google.android.gms.ads.mediation.MediationAdConfiguration;
import java.util.HashMap;
import java.util.Map;

public class PangleRequestHelper {

    @VisibleForTesting public static final String ADMOB_WATERMARK_KEY = "admob_watermark";

    public static void setWatermarkString(PAGRequest request, String bidResponse, MediationAdConfiguration adConfiguration) {
        // Only bidding ads require watermark.
        if (TextUtils.isEmpty(bidResponse)) {
            return;
        }
        String watermark = adConfiguration.getWatermark();
        if (TextUtils.isEmpty(watermark)) {
            return;
        }
        Map<String, Object> extraInfo = request.getExtraInfo();
        if (extraInfo == null) {
            extraInfo = new HashMap<>();
        }
        extraInfo.put(ADMOB_WATERMARK_KEY, watermark);
        request.setExtraInfo(extraInfo);
    }

    private PangleRequestHelper() {}
}
