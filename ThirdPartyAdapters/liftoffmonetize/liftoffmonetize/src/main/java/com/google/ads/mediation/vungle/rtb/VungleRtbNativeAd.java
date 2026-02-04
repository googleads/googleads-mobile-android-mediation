// Copyright 2022 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.vungle.rtb;

import static com.google.ads.mediation.vungle.VungleConstants.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleConstants.KEY_PLACEMENT_ID;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.runtimeGmaSdkListensToAdapterReportedImpressions;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import com.google.ads.mediation.vungle.VungleFactory;
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.formats.NativeAd.Image;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdAssetNames;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.vungle.ads.BaseAd;
import com.vungle.ads.NativeAd;
import com.vungle.ads.NativeAdListener;
import com.vungle.ads.VungleError;
import com.vungle.ads.internal.ui.view.MediaView;
import com.vungle.ads.nativead.NativeVideoListener;
import com.vungle.ads.nativead.NativeVideoOptions;
import java.util.ArrayList;
import java.util.Map;

/**
 * Loads Liftoff's native ad and maps Liftoff's native ad assets to Google SDK's native ad assets.
 *
 * <p>Note: Though this class is named "Rtb", it is used for loading both waterfall and RTB native
 * ads. TODO: Fix the name of this class.
 */
public class VungleRtbNativeAd extends UnifiedNativeAdMapper implements NativeAdListener {

  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
      adLoadCallback;
  private MediationNativeAdCallback nativeAdCallback;

  private NativeAd nativeAd;
  private MediaView mediaView;
  private String adMarkup;

  private final VungleFactory vungleFactory;

  public VungleRtbNativeAd(
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback,
      VungleFactory vungleFactory) {
    this.adLoadCallback = callback;
    this.vungleFactory = vungleFactory;
  }

  public void render(@NonNull MediationNativeAdConfiguration adConfiguration) {
    Bundle serverParameters = adConfiguration.getServerParameters();
    NativeAdOptions nativeAdOptions = adConfiguration.getNativeAdOptions();
    VideoOptions googleVideoOptions = nativeAdOptions.getVideoOptions();
    final Context context = adConfiguration.getContext();

    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load bidding native ad from Liftoff Monetize. "
              + "Missing or invalid app ID configured for this ad source instance "
              + "in the AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.d(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    String placementId = serverParameters.getString(KEY_PLACEMENT_ID);
    if (TextUtils.isEmpty(placementId)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load bidding native ad from Liftoff Monetize. "
              + "Missing or Invalid placement ID configured for this ad source instance "
              + "in the AdMob or Ad Manager UI.", ERROR_DOMAIN);
      Log.d(TAG, error.toString());
      adLoadCallback.onFailure(error);
      return;
    }

    adMarkup = adConfiguration.getBidResponse();

    int privacyIconPlacement = nativeAdOptions.getAdChoicesPlacement();
    int adOptionsPosition;
    switch (privacyIconPlacement) {
      case NativeAdOptions.ADCHOICES_TOP_LEFT:
        adOptionsPosition = NativeAd.TOP_LEFT;
        break;
      case NativeAdOptions.ADCHOICES_BOTTOM_LEFT:
        adOptionsPosition = NativeAd.BOTTOM_LEFT;
        break;
      case NativeAdOptions.ADCHOICES_BOTTOM_RIGHT:
        adOptionsPosition = NativeAd.BOTTOM_RIGHT;
        break;
      case NativeAdOptions.ADCHOICES_TOP_RIGHT:
      default:
        adOptionsPosition = NativeAd.TOP_RIGHT;
        break;
    }

    String watermark = adConfiguration.getWatermark();

    VungleInitializer.getInstance()
        .initialize(
            appID,
            context,
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                nativeAd = vungleFactory.createNativeAd(context, placementId);
                nativeAd.setAdOptionsPosition(adOptionsPosition);
                nativeAd.setAdListener(VungleRtbNativeAd.this);
                if (googleVideoOptions != null) {
                  NativeVideoOptions vngVideoOptions = nativeAd.getVideoOptions();
                  boolean startMuted = googleVideoOptions.getStartMuted();
                  vngVideoOptions.setStartMuted(startMuted);
                }
                mediaView = new MediaView(context);
                if (!TextUtils.isEmpty(watermark)) {
                  nativeAd.getAdConfig().setWatermark(watermark);
                }
                nativeAd.load(adMarkup);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.d(TAG, error.toString());
                adLoadCallback.onFailure(error);
              }
            });
  }

  @Override
  public void onAdLoaded(@NonNull BaseAd baseAd) {
    mapNativeAd();
    nativeAdCallback = adLoadCallback.onSuccess(VungleRtbNativeAd.this);
  }

  @Override
  public void onAdFailedToLoad(@NonNull BaseAd baseAd, @NonNull VungleError vungleError) {
    AdError error = VungleMediationAdapter.getAdError(vungleError);
    adLoadCallback.onFailure(error);
  }

  @Override
  public void onAdFailedToPlay(@NonNull BaseAd baseAd, @NonNull VungleError vungleError) {
    AdError error = VungleMediationAdapter.getAdError(vungleError);
    Log.w(TAG, error.toString());
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdClicked(@NonNull BaseAd baseAd) {
    if (nativeAdCallback != null) {
      nativeAdCallback.reportAdClicked();
      nativeAdCallback.onAdOpened();
    }
  }

  @Override
  public void onAdLeftApplication(@NonNull BaseAd baseAd) {
    if (nativeAdCallback != null) {
      nativeAdCallback.onAdLeftApplication();
    }
  }

  @Override
  public void onAdImpression(@NonNull BaseAd baseAd) {
    if (nativeAdCallback != null) {
      nativeAdCallback.reportAdImpression();
    }
  }

  @Override
  public void onAdStart(@NonNull BaseAd baseAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void onAdEnd(@NonNull BaseAd baseAd) {
    // Google Mobile Ads SDK doesn't have a matching event.
  }

  @Override
  public void trackViews(@NonNull View view, @NonNull Map<String, View> clickableAssetViews,
      @NonNull Map<String, View> nonClickableAssetViews) {
    super.trackViews(view, clickableAssetViews, nonClickableAssetViews);
    Log.d(TAG, "trackViews()");
    if (!(view instanceof ViewGroup)) {
      return;
    }

    ViewGroup adView = (ViewGroup) view;

    if (nativeAd == null) {
      return;
    }

    View overlayView = adView.getChildAt(adView.getChildCount() - 1);

    if (!(overlayView instanceof FrameLayout)) {
      Log.d(TAG, "Vungle requires a FrameLayout to render the native ad.");
      return;
    }

    View iconView = null;
    ArrayList<View> assetViews = new ArrayList<>();
    for (Map.Entry<String, View> clickableAssets : clickableAssetViews.entrySet()) {
      assetViews.add(clickableAssets.getValue());

      if (clickableAssets.getKey().equals(NativeAdAssetNames.ASSET_ICON)) {
        iconView = clickableAssets.getValue();
      } else if (clickableAssets.getKey().equals(NativeAdAssetNames.ASSET_MEDIA_VIDEO)) {
        // enable liftoff mediaView clickable if Google MediaView click is enabled.
        assetViews.add(mediaView);
      }
    }

    ImageView iconImageView = null;
    if (iconView instanceof ImageView) {
      iconImageView = (ImageView) iconView;
    } else {
      Log.d(TAG, "The view to display a Vungle native icon image is not a type of ImageView, "
          + "so it can't be registered for click events.");
    }
    nativeAd.registerViewForInteraction((FrameLayout) overlayView, mediaView, iconImageView,
        assetViews);
  }

  @Override
  public void untrackView(@NonNull View view) {
    super.untrackView(view);
    Log.d(TAG, "untrackView()");
    if (nativeAd == null) {
      return;
    }

    nativeAd.unregisterView();
  }

  private void mapNativeAd() {
    setHeadline(nativeAd.getAdTitle());
    setBody(nativeAd.getAdBodyText());
    setCallToAction(nativeAd.getAdCallToActionText());
    Double starRating = nativeAd.getAdStarRating();
    if (starRating != null) {
      setStarRating(starRating);
    }
    setAdvertiser(nativeAd.getAdSponsoredText());
    setHasVideoContent(nativeAd.hasVideoContent());
    setMediaView(mediaView);
    mediaView.setNativeVideoListener(new NativeVideoListener() {
      @Override
      public void onVideoPlay() {
        nativeAdCallback.onVideoPlay();
      }

      @Override
      public void onVideoPause() {
        nativeAdCallback.onVideoPause();
      }

      @Override
      public void onVideoEnd() {
        nativeAdCallback.onVideoComplete();
      }

      @Override
      public void onVideoMute() {
        nativeAdCallback.onVideoMute();
      }

      @Override
      public void onVideoUnmute() {
        nativeAdCallback.onVideoUnmute();
      }
    });

    String iconUrl = nativeAd.getAppIcon();
    if (!TextUtils.isEmpty(iconUrl) && iconUrl.startsWith("file://")) {
      setIcon(new VungleNativeMappedImage(Uri.parse(iconUrl)));
    }

    setMediaContentAspectRatio(nativeAd.getMediaAspectRatio());

    if (runtimeGmaSdkListensToAdapterReportedImpressions()) {
      setOverrideImpressionRecording(true);
    }
    setOverrideClickHandling(true);
  }

  private static class VungleNativeMappedImage extends Image {

    private Uri imageUri;

    public VungleNativeMappedImage(Uri imageUrl) {
      this.imageUri = imageUrl;
    }

    @Override
    public Drawable getDrawable() {
      return null;
    }

    @Override
    public Uri getUri() {
      return imageUri;
    }

    @Override
    public double getScale() {
      return 1;
    }
  }
}
