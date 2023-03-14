package com.applovin.mediation.rtb;

import android.content.Context;
import androidx.annotation.NonNull;
import com.applovin.adview.AppLovinIncentivizedInterstitial;
import com.applovin.mediation.AppLovinUtils;
import com.applovin.sdk.AppLovinAd;
import com.google.ads.mediation.applovin.AppLovinRewardedRenderer;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

public final class AppLovinRtbRewardedRenderer extends AppLovinRewardedRenderer {

  /**
   * AppLovin rewarded ad object.
   */
  private AppLovinAd appLovinAd;

  public AppLovinRtbRewardedRenderer(
      @NonNull MediationRewardedAdConfiguration adConfiguration,
      @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> callback) {
    super(adConfiguration, callback);
  }

  @Override
  public void loadAd() {
    Context context = adConfiguration.getContext();
    appLovinSdk = AppLovinUtils.retrieveSdk(adConfiguration.getServerParameters(), context);

    // Create rewarded video object.
    incentivizedInterstitial = AppLovinIncentivizedInterstitial.create(appLovinSdk);

    // Load ad.
    appLovinSdk.getAdService().loadNextAdForAdToken(
        adConfiguration.getBidResponse(), AppLovinRtbRewardedRenderer.this);
  }

  @Override
  public void showAd(@NonNull Context context) {
    appLovinSdk.getSettings()
        .setMuted(AppLovinUtils.shouldMuteAudio(adConfiguration.getMediationExtras()));

    incentivizedInterstitial.show(AppLovinRtbRewardedRenderer.this.appLovinAd,
        context, AppLovinRtbRewardedRenderer.this, AppLovinRtbRewardedRenderer.this,
        AppLovinRtbRewardedRenderer.this, AppLovinRtbRewardedRenderer.this);
  }

  // region AppLovinAdLoadListener implementation
  @Override
  public void adReceived(@NonNull AppLovinAd appLovinAd) {
    AppLovinRtbRewardedRenderer.this.appLovinAd = appLovinAd;
    super.adReceived(AppLovinRtbRewardedRenderer.this.appLovinAd);
  }
  // endregion
}
