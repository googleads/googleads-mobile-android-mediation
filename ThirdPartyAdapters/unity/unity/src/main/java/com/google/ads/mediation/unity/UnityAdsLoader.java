package com.google.ads.mediation.unity;

import static com.google.ads.mediation.unity.UnityAdsAdapterUtils.getMediationInfo;

import com.unity3d.ads.BannerAd;
import com.unity3d.ads.BannerConfiguration;
import com.unity3d.ads.BannerShowListener;
import com.unity3d.ads.BannerSize;
import com.unity3d.ads.InterstitialAd;
import com.unity3d.ads.LoadConfiguration;
import com.unity3d.ads.LoadListener;
import com.unity3d.ads.RewardedAd;

/**
 * Wrapper class for UnityAds Ad loading and showing
 */
class UnityAdsLoader {


    /**
     * Loads an interstitial ad using the new Unity Ads API.
     *
     * @param placementId The placement ID
     * @param adMarkup    The ad markup for RTB (nullable)
     * @param objectId    The unique object ID for tracking
     * @param listener    The load listener
     */
    public void loadInterstitial(
        String placementId,
        String adMarkup,
        String objectId,
        LoadListener<InterstitialAd> listener) {

      LoadConfiguration.Builder builder = new LoadConfiguration.Builder(placementId)
        .withMediationInfo(getMediationInfo());

        if (adMarkup != null) {
          builder.withAdMarkup(adMarkup);
        }

        LoadConfiguration config = builder.build();
        InterstitialAd.load(config, listener);
    }

    /**
     * Loads a rewarded ad using the new Unity Ads API.
     *
     * @param placementId The placement ID
     * @param adMarkup    The ad markup for RTB (nullable)
     * @param objectId    The unique object ID for tracking
     * @param listener    The load listener
     */
    public void loadRewarded(
        String placementId,
        String adMarkup,
        String objectId,
        LoadListener<RewardedAd> listener) {

      LoadConfiguration.Builder builder = new LoadConfiguration.Builder(placementId)
        .withMediationInfo(getMediationInfo());

      if (adMarkup != null) {
        builder.withAdMarkup(adMarkup);
      }

      LoadConfiguration config = builder.build();
      RewardedAd.load(config, listener);
    }

    /**
     * Loads a banner ad using the new Unity Ads API.
     *
     * @param placementId   The placement ID
     * @param bannerSize    The Unity banner size
     * @param adMarkup      The ad markup for RTB (nullable)
     * @param loadListener  The load listener
     * @param showListener  The show listener
     */
    public void loadBanner(
        String placementId,
        BannerSize bannerSize,
        String adMarkup,
        LoadListener<BannerAd> loadListener,
        BannerShowListener showListener) {

      BannerConfiguration.Builder builder =
              new BannerConfiguration.Builder(placementId, bannerSize, showListener)
                .withMediationInfo(getMediationInfo());

      if (adMarkup != null) {
        builder.withAdMarkup(adMarkup);
      }

      BannerConfiguration config = builder.build();
      BannerAd.load(config, loadListener);
    }
}
