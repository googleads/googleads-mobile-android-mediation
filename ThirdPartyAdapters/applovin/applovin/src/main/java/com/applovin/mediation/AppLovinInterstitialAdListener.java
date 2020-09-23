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

  private final ApplovinAdapter mAdapter;
  private final MediationInterstitialListener mMediationInterstitialListener;

  AppLovinInterstitialAdListener(
      ApplovinAdapter adapter, MediationInterstitialListener mediationInterstitialListener) {
    mAdapter = adapter;
    mMediationInterstitialListener = mediationInterstitialListener;
  }

  // Ad Display Listener.
  @Override
  public void adDisplayed(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Interstitial displayed.");
    mMediationInterstitialListener.onAdOpened(mAdapter);
  }

  @Override
  public void adHidden(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Interstitial dismissed.");
    mAdapter.unregister();
    mMediationInterstitialListener.onAdClosed(mAdapter);
  }

  // Ad Click Listener.
  @Override
  public void adClicked(AppLovinAd ad) {
    ApplovinAdapter.log(DEBUG, "Interstitial clicked.");

    mMediationInterstitialListener.onAdClicked(mAdapter);
    mMediationInterstitialListener.onAdLeftApplication(mAdapter);
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
