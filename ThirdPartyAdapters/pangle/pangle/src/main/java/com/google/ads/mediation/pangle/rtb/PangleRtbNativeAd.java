package com.google.ads.mediation.pangle.rtb;

import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_BID_RESPONSE;
import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.pangle.PangleMediationAdapter.TAG;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdData;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdLoadListener;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeRequest;
import com.google.ads.mediation.pangle.PangleAdapterUtils;
import com.google.ads.mediation.pangle.PangleConstants;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.formats.NativeAd.Image;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdAssetNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PangleRtbNativeAd extends UnifiedNativeAdMapper {

  private static final double PANGLE_SDK_IMAGE_SCALE = 1.0;
  private final MediationNativeAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
      adLoadCallback;
  private MediationNativeAdCallback callback;
  private PAGNativeAd pagNativeAd;

  public PangleRtbNativeAd(
      @NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull
          MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
              mediationAdLoadCallback) {
    adConfiguration = mediationNativeAdConfiguration;
    adLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    PangleAdapterUtils.setCoppa(adConfiguration.taggedForChildDirectedTreatment());

    String placementId =
        adConfiguration.getServerParameters().getString(PangleConstants.PLACEMENT_ID);
    if (TextUtils.isEmpty(placementId)) {
      AdError error =
          PangleConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load native ad from Pangle. Missing or invalid Placement ID.");
      Log.e(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    String bidResponse = adConfiguration.getBidResponse();
    if (TextUtils.isEmpty(bidResponse)) {
      AdError error =
          PangleConstants.createAdapterError(
              ERROR_INVALID_BID_RESPONSE,
              "Failed to load native ad from Pangle. Missing or invalid bid response.");
      Log.w(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    PAGNativeRequest request = new PAGNativeRequest();
    request.setAdString(bidResponse);
    PAGNativeAd.loadAd(
        placementId,
        request,
        new PAGNativeAdLoadListener() {
          @Override
          public void onError(int errorCode, String message) {
            AdError error = PangleConstants.createSdkError(errorCode, message);
            Log.w(TAG, error.toString());
            adLoadCallback.onFailure(error);
          }

          @Override
          public void onAdLoaded(PAGNativeAd pagNativeAd) {
            mapNativeAd(pagNativeAd);
            callback = adLoadCallback.onSuccess(PangleRtbNativeAd.this);
          }
        });
  }

  private void mapNativeAd(PAGNativeAd ad) {
    this.pagNativeAd = ad;
    // Set data.
    PAGNativeAdData nativeAdData = pagNativeAd.getNativeAdData();
    setHeadline(nativeAdData.getTitle());
    setBody(nativeAdData.getDescription());
    setCallToAction(nativeAdData.getButtonText());
    if (nativeAdData.getIcon() != null) {
      setIcon(
          new PangleNativeMappedImage(
              null, Uri.parse(nativeAdData.getIcon().getImageUrl()), PANGLE_SDK_IMAGE_SCALE));
    }

    // Pangle does its own click event handling.
    setOverrideClickHandling(true);

    // Add Native Feed Main View.
    setMediaView(nativeAdData.getMediaView());

    // Set logo.
    setAdChoicesContent(nativeAdData.getAdLogoView());
  }

  @Override
  public void trackViews(
      @NonNull View containerView,
      @NonNull Map<String, View> clickableAssetViews,
      @NonNull Map<String, View> nonClickableAssetViews) {
    // Set click interaction.
    HashMap<String, View> copyClickableAssetViews = new HashMap<>(clickableAssetViews);
    // Exclude Pangle's Privacy Information Icon image and text from click events.
    copyClickableAssetViews.remove(NativeAdAssetNames.ASSET_ADCHOICES_CONTAINER_VIEW);
    copyClickableAssetViews.remove("3012");
    ArrayList<View> assetViews = new ArrayList<>(copyClickableAssetViews.values());
    View creativeBtn = copyClickableAssetViews.get(NativeAdAssetNames.ASSET_CALL_TO_ACTION);
    ArrayList<View> creativeViews = new ArrayList<>();
    if (creativeBtn != null) {
      creativeViews.add(creativeBtn);
    }
    pagNativeAd.registerViewForInteraction(
        (ViewGroup) containerView,
        assetViews,
        creativeViews,
        null,
        new PAGNativeAdInteractionListener() {
          @Override
          public void onAdClicked() {
            if (callback != null) {
              callback.reportAdClicked();
            }
          }

          @Override
          public void onAdShowed() {
            if (callback != null) {
              callback.reportAdImpression();
            }
          }

          @Override
          public void onAdDismissed() {
            // Google Mobile Ads SDK doesn't have a matching event.
          }
        });

    // Set ad choices click listener to show Pangle's Privacy Policy page.
    getAdChoicesContent()
        .setOnClickListener(
            new OnClickListener() {
              @Override
              public void onClick(View v) {
                pagNativeAd.showPrivacyActivity();
              }
            });
  }

  public class PangleNativeMappedImage extends Image {

    private final Drawable drawable;
    private final Uri imageUri;
    private final double scale;

    private PangleNativeMappedImage(Drawable drawable, Uri imageUri, double scale) {
      this.drawable = drawable;
      this.imageUri = imageUri;
      this.scale = scale;
    }

    @NonNull
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
