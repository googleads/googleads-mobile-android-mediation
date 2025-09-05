package com.google.ads.mediation.facebook;

import android.content.Context;
import com.facebook.ads.AdView;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.MediaView;
import com.facebook.ads.RewardedVideoAd;

/**  A factory for creating Meta Ads SDK objects. */
public class MetaFactory {

  public InterstitialAd createAppOpenAd(Context context, String placementId) {
    return new InterstitialAd(context, placementId);
  }

  public InterstitialAd createInterstitialAd(Context context, String placementId) {
    return new InterstitialAd(context, placementId);
  }

  public RewardedVideoAd createRewardedAd(Context context, String placementId) {
    return new RewardedVideoAd(context, placementId);
  }

  public AdView createMetaAdView(Context context, String placementId, String bidPayload)
      throws Exception {
    return new AdView(context, placementId, bidPayload);
  }

  public MediaView createMediaView(Context context) {
    return new MediaView(context);
  }
}
