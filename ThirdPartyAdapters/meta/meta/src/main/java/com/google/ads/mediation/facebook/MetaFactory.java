package com.google.ads.mediation.facebook;

import android.content.Context;
import com.facebook.ads.AdView;
import com.facebook.ads.InterstitialAd;

/**  A factory for creating Meta Ads SDK objects. */
public class MetaFactory {

  public InterstitialAd createInterstitialAd(Context context, String placementId) {
    return new InterstitialAd(context, placementId);
  }

  public AdView createMetaAdView(Context context, String placementId, String bidPayload)
      throws Exception {
    return new AdView(context, placementId, bidPayload);
  }
}
