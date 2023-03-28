package com.google.ads.mediation.mintegral.mediation;

import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.mbridge.msdk.out.Campaign;
import com.mbridge.msdk.out.Frame;
import com.mbridge.msdk.out.NativeAdWithCodeListener;

import java.util.List;

public class MintegralNativeAdListener extends NativeAdWithCodeListener {

  protected  MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> adLoadCallback;
  protected MediationNativeAdCallback nativeCallback;
  private MintegralNativeAd mintegralNativeAd;

  public MintegralNativeAdListener(@NonNull MintegralNativeAd mintegralNativeAd){
    this.mintegralNativeAd = mintegralNativeAd;
    this.nativeCallback = mintegralNativeAd.nativeCallback;
    this.adLoadCallback = mintegralNativeAd.adLoadCallback;
  }

  @Override
  public void onAdLoaded(List<Campaign> list, int template) {
    if (list == null || list.size() == 0) {
      AdError adError = MintegralConstants.createAdapterError(MintegralConstants.ERROR_CODE_NO_FILL,
              "Mintegral SDK failed to return a native ad.");
      Log.w(TAG, adError.toString());
      adLoadCallback.onFailure(adError);
      return;
    }
    mintegralNativeAd.mapNativeAd(list.get(0));
    nativeCallback = adLoadCallback.onSuccess(mintegralNativeAd);
  }



  @Override
  public void onAdLoadErrorWithCode(int errorCode, String errorMessage) {
    AdError adError = MintegralConstants.createSdkError(errorCode,
            errorMessage);
    Log.w(TAG, adError.toString());
    adLoadCallback.onFailure(adError);
  }

  @Override
  public void onAdClick(Campaign campaign) {
    if (nativeCallback != null) {
      nativeCallback.reportAdClicked();
      nativeCallback.onAdLeftApplication();
    }
  }

  @Override
  public void onAdFramesLoaded(List<Frame> list) {
    // No-op, this callback is deprecated in Mintegral SDK.
  }

  @Override
  public void onLoggingImpression(int i) {
    if (nativeCallback != null) {
      nativeCallback.reportAdImpression();
    }
  }
}
