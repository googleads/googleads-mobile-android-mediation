package com.google.ads.mediation.mintegral.rtb;

import static com.google.ads.mediation.mintegral.MintegralMediationAdapter.TAG;
import static com.mbridge.msdk.MBridgeConstans.NATIVE_VIDEO_SUPPORT;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.ads.mediation.mintegral.MintegralConstants;
import com.google.ads.mediation.mintegral.MintegralUtils;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.formats.NativeAd.Image;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAdAssetNames;
import com.mbridge.msdk.MBridgeConstans;
import com.mbridge.msdk.nativex.view.MBMediaView;
import com.mbridge.msdk.out.Campaign;
import com.mbridge.msdk.out.Frame;
import com.mbridge.msdk.out.MBBidNativeHandler;
import com.mbridge.msdk.out.NativeListener;
import com.mbridge.msdk.out.OnMBMediaViewListener;
import com.mbridge.msdk.widget.MBAdChoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MintegralRtbNativeAd extends UnifiedNativeAdMapper implements
    NativeListener.NativeAdListener, OnMBMediaViewListener {

  private Campaign campaign;
  private final MediationNativeAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> adLoadCallback;
  private MediationNativeAdCallback nativeCallback;
  private MBBidNativeHandler mbBidNativeHandler;
  private static final double MINTEGRAL_SDK_IMAGE_SCALE = 1.0;

  public MintegralRtbNativeAd(
      @NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mediationAdLoadCallback) {
    adConfiguration = mediationNativeAdConfiguration;
    adLoadCallback = mediationAdLoadCallback;
  }

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

  @NonNull
  private void mapNativeAd(@NonNull Campaign ad) {
    campaign = ad;
    if (campaign.getAppName() != null) {
      setHeadline(campaign.getAppName());
    }
    if (campaign.getAppDesc() != null) {
      setBody(campaign.getAppDesc());
    }
    if (campaign.getAdCall() != null) {
      setCallToAction(campaign.getAdCall());
    }
    setStarRating(campaign.getRating());
    if (!TextUtils.isEmpty(campaign.getIconUrl())) {
      setIcon(new MBridgeNativeMappedImage(null, Uri.parse(campaign.getIconUrl()),
          MINTEGRAL_SDK_IMAGE_SCALE));
    }
    MBMediaView mbMediaView = new MBMediaView(adConfiguration.getContext());
    boolean muted = MintegralUtils.shouldMuteAudio(adConfiguration.getMediationExtras());
    mbMediaView.setVideoSoundOnOff(!muted);
    mbMediaView.setNativeAd(campaign);
    setMediaView(mbMediaView);

    MBAdChoice mbAdChoice = new MBAdChoice(adConfiguration.getContext());
    mbAdChoice.setCampaign(campaign);
    setAdChoicesContent(mbAdChoice);
    setOverrideClickHandling(true);
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

  /**
   * Traverse all sub views of the view to add click event listening to all views
   *
   * @param view View of advertising area
   * @return Return a list containing all the views that need to respond to the click
   */
  private List traversalView(View view) {
    List<View> viewList = new ArrayList<View>();
    if (view == null) {
      return viewList;
    }
    if (view instanceof MediaView) {
      viewList.add(view);
    } else if (view instanceof ViewGroup) {
      ViewGroup viewGroup = (ViewGroup) view;
      for (int i = 0; i < viewGroup.getChildCount(); i++) {
        if (viewGroup.getChildAt(i) instanceof ViewGroup) {
          viewList.addAll(traversalView(viewGroup.getChildAt(i)));
        } else {
          viewList.add(viewGroup.getChildAt(i));
        }
      }
    } else if (view instanceof View) {
      viewList.add(view);
    }
    return viewList;
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
    mapNativeAd(list.get(0));
    nativeCallback = adLoadCallback.onSuccess(MintegralRtbNativeAd.this);
  }

  @Override
  public void onAdLoadError(String errorMessage) {
    AdError adError = MintegralConstants.createAdapterError(MintegralConstants.ERROR_MINTEGRAL_SDK,
        errorMessage);
    Log.w(TAG, adError.toString());
    adLoadCallback.onFailure(adError);
  }

  @Override
  public void onAdClick(Campaign campaign) {
    if (nativeCallback != null) {
      nativeCallback.reportAdClicked();
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

  @Override
  public void onEnterFullscreen() {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onExitFullscreen() {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onStartRedirection(Campaign campaign, String s) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onFinishRedirection(Campaign campaign, String s) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onRedirectionFailed(Campaign campaign, String s) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onVideoAdClicked(Campaign campaign) {
    if (nativeCallback != null) {
      nativeCallback.reportAdClicked();
    }
  }

  @Override
  public void onVideoStart() {
    if (nativeCallback != null) {
      nativeCallback.onVideoPlay();
    }
  }

  public class MBridgeNativeMappedImage extends Image {

    private final Drawable drawable;
    private final Uri imageUri;
    private final double scale;

    public MBridgeNativeMappedImage(Drawable drawable, Uri imageUri, double scale) {
      this.drawable = drawable;
      this.imageUri = imageUri;
      this.scale = scale;
    }

    @Override
    public Drawable getDrawable() {
      return drawable;
    }

    @NonNull
    @Override
    public Uri getUri() {
      return imageUri;
    }

    @Override
    public double getScale() {
      return scale;
    }
  }
}
