package com.google.ads.mediation.pangle;

import android.text.TextUtils;

import com.bytedance.sdk.openadsdk.api.PAGRequest;
import com.google.android.gms.ads.mediation.MediationAdConfiguration;

import java.util.HashMap;
import java.util.Map;

public class PangleRequestHelper {
    public static void fillWaterCoverParam(PAGRequest request, String bidResponse, MediationAdConfiguration adConfiguration) {
        // only accept bidding scene
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
        extraInfo.put("admob_watermark", watermark);
        request.setExtraInfo(extraInfo);
    }
}
