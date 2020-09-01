package com.google.ads.mediation.nend;

import static com.google.ads.mediation.nend.NendMediationAdapter.KEY_API_KEY;
import static com.google.ads.mediation.nend.NendMediationAdapter.KEY_SPOT_ID;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;

class AdUnitMapper {

  final int spotId;
  final String apiKey;

  private AdUnitMapper(Bundle serverParameters) {
    this.apiKey = serverParameters.getString(KEY_API_KEY);
    this.spotId = Integer.parseInt(serverParameters.getString(KEY_SPOT_ID, "0"));
  }

  @Nullable
  static AdUnitMapper createAdUnitMapper(Bundle serverParameters) {
    AdUnitMapper mapper = new AdUnitMapper(serverParameters);

    if (TextUtils.isEmpty(mapper.apiKey)) {
      Log.w(
          NendMediationAdapter.TAG, "Failed to request ad from Nend: Missing or invalid API Key.");
      return null;
    }

    if (mapper.spotId <= 0) {
      Log.w(
          NendMediationAdapter.TAG, "Failed to request ad from Nend: Missing or invalid Spot ID.");
      return null;
    }

    return mapper;
  }
}
