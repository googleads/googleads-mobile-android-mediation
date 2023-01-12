package com.google.ads.mediation.mintegral.rtb;

import static com.mbridge.msdk.MBridgeConstans.NATIVE_VIDEO_SUPPORT;

import android.view.View;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.ads.mediation.mintegral.mediation.MintegralNativeAd;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdAssetNames;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.out.MBBidNativeHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MintegralRtbNativeAd extends MintegralNativeAd {

  private MBBidNativeHandler mbBidNativeHandler;

  public MintegralRtbNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration, @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mediationAdLoadCallback) {
    super(mediationNativeAdConfiguration, mediationAdLoadCallback);
  }

  @Override
  public void loadAd() {
    String adUnitId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.AD_UNIT_ID);
    String placementId = adConfiguration.getServerParameters()
        .getString(MintegralConstants.PLACEMENT_ID);
    String bidToken = adConfiguration.getBidResponse();
    AdError error =
        MintegralUtils.validateMintegralAdLoadParams(
            adUnitId, placementId, bidToken);
    if (error != null) {
      adLoadCallback.onFailure(error);
      return;
    }
    Map<String, Object> nativeProperties = MBBidNativeHandler.getNativeProperties(placementId,
        adUnitId);
    // Configure the properties of the Mintegral native ad, where video ad will be supported and
    // only one ad will be returned in each ad request.
    nativeProperties.put(NATIVE_VIDEO_SUPPORT, true);
    nativeProperties.put(MBridgeConstans.PROPERTIES_AD_NUM, 1);
    mbBidNativeHandler = new MBBidNativeHandler(nativeProperties, adConfiguration.getContext());
    mbBidNativeHandler.setAdListener(this);
    mbBidNativeHandler.bidLoad(bidToken);
  }

  @Override
  public void trackViews(@NonNull View view, @NonNull Map<String, View> clickableAssetViews,
      @NonNull Map<String, View> map1) {
    // Set click interaction.
    HashMap<String, View> copyClickableAssetViews = new HashMap<>(clickableAssetViews);

    // Exclude Mintegral's Privacy Information Icon image and text from click events.
    copyClickableAssetViews.remove(NativeAdAssetNames.ASSET_ADCHOICES_CONTAINER_VIEW);
    copyClickableAssetViews.remove("3012");

    ArrayList<View> assetViews = new ArrayList<>(copyClickableAssetViews.values());
    if (mbBidNativeHandler != null) {
      mbBidNativeHandler.registerView(null, assetViews, campaign);
    }
  }

  @Override
  public void untrackView(View view) {
    if (mbBidNativeHandler != null) {
      mbBidNativeHandler.unregisterView(view, traversalView(view), campaign);
    }
  }
}
