package com.google.ads.mediation.pangle.rtb;

import static com.google.ads.mediation.pangle.PangleConstants.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_BID_RESPONSE;
import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.pangle.PangleMediationAdapter.TAG;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAd;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerAdLoadListener;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerRequest;
import com.bytedance.sdk.openadsdk.api.banner.PAGBannerSize;
import com.google.ads.mediation.pangle.PangleAdapterUtils;
import com.google.ads.mediation.pangle.PangleConstants;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import java.util.ArrayList;

public class PangleRtbBannerAd
    implements MediationBannerAd, PAGBannerAdInteractionListener {

  private final MediationBannerAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
      adLoadCallback;
  private MediationBannerAdCallback bannerAdCallback;
  private FrameLayout wrappedAdView;

  public PangleRtbBannerAd(
      @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
      @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
          mediationAdLoadCallback) {
    this.adConfiguration = mediationBannerAdConfiguration;
    this.adLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    PangleAdapterUtils.setCoppa(adConfiguration.taggedForChildDirectedTreatment());
    PangleAdapterUtils.setUserData(adConfiguration.getMediationExtras());

    String placementId =
        adConfiguration.getServerParameters().getString(PangleConstants.PLACEMENT_ID);
    if (TextUtils.isEmpty(placementId)) {
      AdError error =
          PangleConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load banner ad from Pangle. Missing or invalid Placement ID.");
      Log.e(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    String bidResponse = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(bidResponse)) {
      AdError error =
          PangleConstants.createAdapterError(
              ERROR_INVALID_BID_RESPONSE,
              "Failed to load banner ad from Pangle. Missing or invalid bid response.");
      Log.w(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    Context context = adConfiguration.getContext();
    ArrayList<AdSize> supportedSizes = new ArrayList<>(3);
    supportedSizes.add(new AdSize(320, 50));
    supportedSizes.add(new AdSize(300, 250));
    supportedSizes.add(new AdSize(728, 90));
    AdSize closestSize =
        MediationUtils.findClosestSize(context, adConfiguration.getAdSize(), supportedSizes);
    if (closestSize == null) {
      AdError error =
          PangleConstants.createAdapterError(
              ERROR_BANNER_SIZE_MISMATCH,
              "Failed to request banner ad from Pangle. Invalid banner size.");
      Log.w(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    wrappedAdView = new FrameLayout(context);

    PAGBannerRequest request = new PAGBannerRequest(
        new PAGBannerSize(closestSize.getWidth(), closestSize.getHeight()));
    request.setAdString(bidResponse);
    PAGBannerAd.loadAd(
        placementId,
        request,
        new PAGBannerAdLoadListener() {
          @Override
          public void onError(int errorCode, String errorMessage) {
            AdError error = PangleConstants.createSdkError(errorCode, errorMessage);
            Log.w(TAG, error.toString());
            adLoadCallback.onFailure(error);
          }

          @Override
          public void onAdLoaded(PAGBannerAd pagBannerAd) {
            pagBannerAd.setAdInteractionListener(PangleRtbBannerAd.this);
            wrappedAdView.addView(pagBannerAd.getBannerView());
            bannerAdCallback = adLoadCallback.onSuccess(PangleRtbBannerAd.this);
          }
        });
  }

  @NonNull
  @Override
  public View getView() {
    return wrappedAdView;
  }

  @Override
  public void onAdShowed() {
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onAdClicked() {
    if (bannerAdCallback != null) {
      bannerAdCallback.reportAdClicked();
    }
  }

  @Override
  public void onAdDismissed() {
    // Google Mobile Ads SDK doesn't have a matching event.
  }
}
