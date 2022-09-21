package com.applovin.mediation;

import static android.util.Log.DEBUG;

import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

/**
 * The {@link AppLovinInterstitialAdListener} class is used to forward Interstitial ad events from
 * the AppLovin SDK to the Google Mobile Ads SDK.
 */
class AppLovinInterstitialAdListener
    implements AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener {

  private final ApplovinAdapter adapter;
  private final MediationInterstitialListener mediationInterstitialListener;

  AppLovinInterstitialAdListener(
      ApplovinAdapter adapter, MediationInterstitialListener mediationInterstitialListener) {
    this.adapter = adapter;
    this.mediationInterstitialListener = mediationInterstitialListener;
  }

  // Ad Display Listener.
  @Override
  public void adDisplayed(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Interstitial displayed.");
    mediationInterstitialListener.onAdOpened(adapter);
  }

  @Override
  public void adHidden(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Interstitial dismissed.");
    adapter.unregister();
    mediationInterstitialListener.onAdClosed(adapter);
  }

  // Ad Click Listener.
  @Override
  public void adClicked(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Interstitial clicked.");
    mediationInterstitialListener.onAdClicked(adapter);
    mediationInterstitialListener.onAdLeftApplication(adapter);
  }

  // Ad Video Playback Listener.
  @Override
  public void videoPlaybackBegan(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Interstitial video playback began.");
  }

  @Override
  public void videoPlaybackEnded(AppLovinAd ad, double percentViewed, boolean fullyWatched) {
    ApplovinAdapter.log(
        DEBUG, "Interstitial video playback ended at playback percent: " + percentViewed + "%.");
  }
}
