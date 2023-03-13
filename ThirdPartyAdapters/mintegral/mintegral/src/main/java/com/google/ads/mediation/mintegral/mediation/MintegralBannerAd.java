package com.google.ads.mediation.mintegral.mediation;

import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.mbridge.msdk.out.BannerAdWithCodeListener;
import com.mbridge.msdk.out.MBBannerView;
import com.mbridge.msdk.out.MBridgeIds;


public abstract class MintegralBannerAd extends BannerAdWithCodeListener implements MediationBannerAd {

  protected MediationBannerAdConfiguration adConfiguration;
  protected final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      adLoadCallback;
  protected MBBannerView mbBannerView;
  protected MediationBannerAdCallback bannerAdCallback;


  public MintegralBannerAd(@NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
          mediationAdLoadCallback) {
    this.adConfiguration = mediationBannerAdConfiguration;
    this.adLoadCallback = mediationAdLoadCallback;
  }

  /**
   * Loads a Mintegral banner ad.
   */
  public abstract void loadAd();

  @NonNull
  @Override
  public View getView() {
    return mbBannerView;
  }

  @Override
  public void onLoadFailedWithCode(MBridgeIds mBridgeIds, int errorCode, String errorMessage) {
    AdError error = MintegralConstants.createSdkError(errorCode,errorMessage);
    Log.w(TAG, error.toString());
    adLoadCallback.onFailure(error);
  }

  @Override
  public void onLoadSuccessed(MBridgeIds mBridgeIds) {
    if (adLoadCallback != null) {
      bannerAdCallback = adLoadCallback.onSuccess(this);
    }
  }

  @Override
  public void onLogImpression(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onClick(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onLeaveApp(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdLeftApplication();
    }
  }

  @Override
  public void showFullScreen(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdOpened();
    }
  }

  @Override
  public void closeFullScreen(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdClosed();
    }
  }

  @Override
  public void onCloseBanner(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdClosed();
    }
  }
}
