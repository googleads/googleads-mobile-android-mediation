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

package com.google.ads.mediation.pangle.renderer;

import static com.google.ads.mediation.pangle.PangleConstants.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.pangle.PangleMediationAdapter.TAG;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAd;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdData;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdInteractionListener;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeAdLoadListener;
import com.bytedance.sdk.openadsdk.api.nativeAd.PAGNativeRequest;
import com.google.ads.mediation.pangle.PangleConstants;
import com.google.ads.mediation.pangle.PangleFactory;
import com.google.ads.mediation.pangle.PangleInitializer;
import com.google.ads.mediation.pangle.PangleInitializer.Listener;
import com.google.ads.mediation.pangle.PanglePrivacyConfig;
import com.google.ads.mediation.pangle.PangleRequestHelper;
import com.google.ads.mediation.pangle.PangleSdkWrapper;
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

public class PangleNativeAd extends UnifiedNativeAdMapper {

  @VisibleForTesting static final double PANGLE_SDK_IMAGE_SCALE = 1.0;

  /** ID of Ad Choices text view asset. */
  @VisibleForTesting static final String ASSET_ID_ADCHOICES_TEXT_VIEW = "3012";

  private final MediationNativeAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
      adLoadCallback;
  private final PangleInitializer pangleInitializer;
  private final PangleSdkWrapper pangleSdkWrapper;
  private final PangleFactory pangleFactory;
  private final PanglePrivacyConfig panglePrivacyConfig;
  private MediationNativeAdCallback callback;
  private PAGNativeAd pagNativeAd;

  public PangleNativeAd(
      @NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull
          MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
              mediationAdLoadCallback,
      @NonNull PangleInitializer pangleInitializer,
      @NonNull PangleSdkWrapper pangleSdkWrapper,
      @NonNull PangleFactory pangleFactory,
      @NonNull PanglePrivacyConfig panglePrivacyConfig) {
    adConfiguration = mediationNativeAdConfiguration;
    adLoadCallback = mediationAdLoadCallback;
    this.pangleInitializer = pangleInitializer;
    this.pangleSdkWrapper = pangleSdkWrapper;
    this.pangleFactory = pangleFactory;
    this.panglePrivacyConfig = panglePrivacyConfig;
  }

  public void render() {
    panglePrivacyConfig.setCoppa(adConfiguration.taggedForChildDirectedTreatment());

    Bundle serverParameters = adConfiguration.getServerParameters();
    String placementId = serverParameters.getString(PangleConstants.PLACEMENT_ID);
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
    Context context = adConfiguration.getContext();
    String appId = serverParameters.getString(PangleConstants.APP_ID);
    pangleInitializer.initialize(
        context,
        appId,
        new Listener() {
          @Override
          public void onInitializeSuccess() {
            PAGNativeRequest request = pangleFactory.createPagNativeRequest();
            request.setAdString(bidResponse);
            PangleRequestHelper.fillWaterCoverParam(request, bidResponse, adConfiguration);
            pangleSdkWrapper.loadNativeAd(
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
                    callback = adLoadCallback.onSuccess(PangleNativeAd.this);
                  }
                });
          }

          @Override
          public void onInitializeError(@NonNull AdError error) {
            Log.w(TAG, error.toString());
            adLoadCallback.onFailure(error);
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
    copyClickableAssetViews.remove(ASSET_ID_ADCHOICES_TEXT_VIEW);

    View creativeBtn = copyClickableAssetViews.get(NativeAdAssetNames.ASSET_CALL_TO_ACTION);
    ArrayList<View> creativeViews = new ArrayList<>();
    if (creativeBtn != null) {
      creativeViews.add(creativeBtn);
    }

    ArrayList<View> assetViews = new ArrayList<>(copyClickableAssetViews.values());
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
