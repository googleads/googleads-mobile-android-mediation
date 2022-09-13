package com.google.ads.mediation.mintegral.rtb;


import static com.google.ads.mediation.mintegral.MintegralConstants.ERROR_BANNER_SIZE_MISMATCH;
import static com.google.ads.mediation.mintegral.MintegralConstants.ERROR_INVALID_BID_RESPONSE;
import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.mbridge.msdk.out.BannerAdListener;
import com.mbridge.msdk.out.BannerSize;
import com.mbridge.msdk.out.MBBannerView;
import com.mbridge.msdk.out.MBridgeIds;

import java.util.ArrayList;

public class MintegralRtbBannerAd implements MediationBannerAd, BannerAdListener {

  private MBBannerView mbBannerView;
  private final MediationBannerAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> adLoadCallback;
  private MediationBannerAdCallback bannerAdCallback;


  public MintegralRtbBannerAd(
          @NonNull MediationBannerAdConfiguration mediationBannerAdConfiguration,
          @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
                  mediationAdLoadCallback) {
    this.adConfiguration = mediationBannerAdConfiguration;
    this.adLoadCallback = mediationAdLoadCallback;
  }

  /**
   * Retrieves the bannerView pixels
   *
   * @param context the context object.
   * @param dpValue the bannerView dip size
   * @return the Mintegral bannerView pixels
   */
  private static int convertDpToPixels(Context context, float dpValue) {
    final float scale = context.getResources().getDisplayMetrics().density;
    return (int) (dpValue * scale + 0.5f);
  }


  private void initBannerAd() {
    BannerSize bannerSize = null;
    ArrayList<AdSize> supportedSizes = new ArrayList<>(3);
    supportedSizes.add(new AdSize(320, 50));
    supportedSizes.add(new AdSize(300, 250));
    supportedSizes.add(new AdSize(728, 90));
    AdSize closestSize = MediationUtils.findClosestSize(adConfiguration.getContext(), adConfiguration.getAdSize(), supportedSizes);
    if (closestSize == null) {
      AdError error = MintegralConstants.createAdapterError(
              ERROR_BANNER_SIZE_MISMATCH,
              "Failed to request banner ad from Mintegral. Invalid banner size.");
      Log.w(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    if (closestSize.equals(AdSize.BANNER)) { // 320 * 50
      bannerSize = new BannerSize(BannerSize.STANDARD_TYPE, 0, 0);
    }
    if (closestSize.equals(AdSize.MEDIUM_RECTANGLE)) { // 300 * 250
      bannerSize = new BannerSize(BannerSize.MEDIUM_TYPE, 0, 0);
    }
    if (closestSize.equals(AdSize.LEADERBOARD)) { // 728 * 90
      bannerSize = new BannerSize(BannerSize.SMART_TYPE, closestSize.getWidth(), 0);
    }
    if (bannerSize == null) {
      bannerSize = new BannerSize(BannerSize.DEV_SET_TYPE, closestSize.getWidth(), closestSize.getHeight());
    }

    String adUnitId = adConfiguration.getServerParameters().getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters().getString(MintegralConstants.PLACEMENT_ID);
    if (TextUtils.isEmpty(adUnitId)) {
      AdError error = MintegralConstants.createAdapterError(MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS, "Failed to load banner ad from MIntegral. Missing or invalid adUnitId");
      adLoadCallback.onFailure(error);
      return;
    }
    if (TextUtils.isEmpty(placementId)) {
      AdError error = MintegralConstants.createAdapterError(MintegralConstants.ERROR_INVALID_SERVER_PARAMETERS, "Failed to load banner ad from MIntegral. Missing or invalid placementId");
      adLoadCallback.onFailure(error);
      return;
    }
    mbBannerView = new MBBannerView(adConfiguration.getContext());
    mbBannerView.init(bannerSize, placementId, adUnitId);
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
    layoutParams.width = convertDpToPixels(adConfiguration.getContext(), bannerSize.getWidth());
    layoutParams.height = convertDpToPixels(adConfiguration.getContext(), bannerSize.getHeight());
    mbBannerView.setLayoutParams(layoutParams);
  }

  public void loadAd() {
    cleanLeakedBannerRes();
    initBannerAd();
    String token = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(token)) {
      callFailureCallback(ERROR_INVALID_BID_RESPONSE, "Failed to load Banner ad from MIntegral. Missing or invalid bid response.");
      return;
    }
    mbBannerView.setBannerAdListener(this);
    mbBannerView.loadFromBid(token);
  }

  @NonNull
  @Override
  public View getView() {
    return mbBannerView;
  }

  @Override
  public void onLoadFailed(MBridgeIds mBridgeIds, String errorMessage) {
    AdError error = MintegralConstants.createSdkError(errorMessage);
    Log.w(TAG, error.toString());
    adLoadCallback.onFailure(error);
  }

  @Override
  public void onLoadSuccessed(MBridgeIds mBridgeIds) {
    if (adLoadCallback == null) {
      return;
    }
    bannerAdCallback = adLoadCallback.onSuccess(this);
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
    //No-op, AdMob has no corresponding method
  }

  @Override
  public void closeFullScreen(MBridgeIds mBridgeIds) {
    //No-op, AdMob has no corresponding method
  }

  @Override
  public void onCloseBanner(MBridgeIds mBridgeIds) {
    if (bannerAdCallback != null) {
      bannerAdCallback.onAdClosed();
    }
  }

  private void callFailureCallback(int errorCode, String errorMessage) {
    AdError error = MintegralConstants.createAdapterError(errorCode, errorMessage);
    adLoadCallback.onFailure(error);
  }

  /**
   * Workaround to finish and clean {@link MintegralRtbBannerAd} if {@link
   * MBBannerView #release()} is not called and adapter was garbage collected.
   */
  private void cleanLeakedBannerRes() {
    if (mbBannerView == null) {
      return;
    }
    mbBannerView.release();
  }
}
