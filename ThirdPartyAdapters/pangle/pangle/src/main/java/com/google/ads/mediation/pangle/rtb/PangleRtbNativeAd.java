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
import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.bytedance.sdk.openadsdk.adapter.MediaView;
import com.bytedance.sdk.openadsdk.adapter.MediationAdapterUtil;
import com.google.ads.mediation.pangle.PangleConstants;
import com.google.ads.mediation.pangle.PangleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.formats.NativeAd.Image;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdAssetNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PangleRtbNativeAd extends UnifiedNativeAdMapper {

  private static final double PANGLE_SDK_IMAGE_SCALE = 1.0;
  private final MediationNativeAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> adLoadCallback;
  private MediationNativeAdCallback callback;
  private TTFeedAd ttFeedAd;

  public PangleRtbNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mediationAdLoadCallback) {
    adConfiguration = mediationNativeAdConfiguration;
    adLoadCallback = mediationAdLoadCallback;
  }

  public void render() {
    PangleMediationAdapter.setCoppa(adConfiguration.taggedForChildDirectedTreatment());

    String placementId = adConfiguration.getServerParameters()
        .getString(PangleConstants.PLACEMENT_ID);
    if (TextUtils.isEmpty(placementId)) {
      AdError error =
          PangleConstants.createAdapterError(
              ERROR_INVALID_SERVER_PARAMETERS,
              "Failed to load native ad from Pangle. Missing or invalid Placement ID.");
      Log.w(TAG, error.toString());
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

    TTAdManager mTTAdManager = PangleMediationAdapter.getPangleSdkManager();
    TTAdNative mTTAdNative = mTTAdManager
        .createAdNative(adConfiguration.getContext().getApplicationContext());

    AdSlot adSlot = new AdSlot.Builder()
        .setCodeId(placementId)
        .setAdCount(1)
        .withBid(bidResponse)
        .build();

    mTTAdNative.loadFeedAd(adSlot, new TTAdNative.FeedAdListener() {
      @Override
      public void onError(int errorCode, String message) {
        AdError error = PangleConstants.createSdkError(errorCode, message);
        Log.w(TAG, error.toString());
        adLoadCallback.onFailure(error);
      }

      @Override
      public void onFeedAdLoad(List<TTFeedAd> ads) {
        mapNativeAd(ads.get(0));
        callback = adLoadCallback.onSuccess(PangleRtbNativeAd.this);
      }
    });
  }

  private void mapNativeAd(TTFeedAd ad) {
    this.ttFeedAd = ad;
    // Set data.
    setHeadline(ttFeedAd.getTitle());
    setBody(ttFeedAd.getDescription());
    setCallToAction(ttFeedAd.getButtonText());
    if (ttFeedAd.getIcon() != null && ttFeedAd.getIcon().isValid()) {
      setIcon(new PangleNativeMappedImage(null, Uri.parse(ttFeedAd.getIcon().getImageUrl()),
          PANGLE_SDK_IMAGE_SCALE));
    }
    // Set ad image.
    if (ttFeedAd.getImageList() != null && ttFeedAd.getImageList().size() != 0) {
      List<Image> imagesList = new ArrayList<>();
      for (TTImage ttImage : ttFeedAd.getImageList()) {
        if (ttImage.isValid()) {
          imagesList.add(new PangleNativeMappedImage(null, Uri.parse(ttImage.getImageUrl()),
              PANGLE_SDK_IMAGE_SCALE));
        }
      }
      setImages(imagesList);
    }

    // Pangle does its own show event handling and click event handling.
    setOverrideImpressionRecording(true);
    setOverrideClickHandling(true);

    // Add Native Feed Main View.
    MediaView mediaView = new MediaView(adConfiguration.getContext());
    MediationAdapterUtil
        .addNativeFeedMainView(adConfiguration.getContext(), ttFeedAd.getImageMode(), mediaView,
            ttFeedAd.getAdView(), ttFeedAd.getImageList());
    setMediaView(mediaView);

    // Set logo.
    setAdChoicesContent(ttFeedAd.getAdLogoView());

    if (ttFeedAd.getImageMode() == TTAdConstant.IMAGE_MODE_VIDEO ||
        ttFeedAd.getImageMode() == TTAdConstant.IMAGE_MODE_VIDEO_VERTICAL ||
        ttFeedAd.getImageMode() == TTAdConstant.IMAGE_MODE_VIDEO_SQUARE) {
      setHasVideoContent(true);
      ttFeedAd.setVideoAdListener(new TTFeedAd.VideoAdListener() {
        @Override
        public void onVideoLoad(TTFeedAd ad) {
          // No-op, will be deprecated in the next version.
        }

        @Override
        public void onVideoError(int errorCode, int extraCode) {
          String errorMessage = String.format("Native ad video playback error," +
              " errorCode: %s, extraCode: %s.", errorCode, extraCode);
          Log.d(TAG, errorMessage);
        }

        @Override
        public void onVideoAdStartPlay(TTFeedAd ad) {
          // No-op, will be deprecated in the next version.
        }

        @Override
        public void onVideoAdPaused(TTFeedAd ad) {
          // No-op, will be deprecated in the next version.
        }

        @Override
        public void onVideoAdContinuePlay(TTFeedAd ad) {
          // No-op, will be deprecated in the next version.
        }

        @Override
        public void onProgressUpdate(long current, long duration) {
          // No-op, will be deprecated in the next version.
        }

        @Override
        public void onVideoAdComplete(TTFeedAd ad) {
          // No-op, will be deprecated in the next version.
        }
      });
    }
  }


  @Override
  public void trackViews(@NonNull View containerView,
      @NonNull Map<String, View> clickableAssetViews,
      @NonNull Map<String, View> nonClickableAssetViews) {
    if (ttFeedAd == null) {
      Log.d(TAG, "The native ad object is null, stop the trackViews process");
      return;
    }
    // Set click interaction.
    HashMap<String, View> copyClickableAssetViews = new HashMap<>(clickableAssetViews);
    copyClickableAssetViews.remove(NativeAdAssetNames.ASSET_ADCHOICES_CONTAINER_VIEW);
    // Exclude fragments view containing ad choices to avoid ad choices click listener failure.
    copyClickableAssetViews.remove("3012");
    ArrayList<View> assetViews = new ArrayList<>(copyClickableAssetViews.values());
    View creativeBtn = copyClickableAssetViews.get(NativeAdAssetNames.ASSET_CALL_TO_ACTION);
    ArrayList<View> creativeViews = new ArrayList<>();
    if (creativeBtn != null) {
      creativeViews.add(creativeBtn);
    }
    ttFeedAd.registerViewForInteraction((ViewGroup) containerView, assetViews, creativeViews,
        new TTNativeAd.AdInteractionListener() {
          @Override
          public void onAdClicked(View view, TTNativeAd ad) {
            // No-op.
          }

          @Override
          public void onAdCreativeClick(View view, TTNativeAd ad) {
            if (callback != null) {
              callback.reportAdClicked();
            }
          }

          @Override
          public void onAdShow(TTNativeAd ad) {
            if (callback != null) {
              callback.reportAdImpression();
            }
          }
        });
    // Set ad choices click listener.
    getAdChoicesContent().setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        ttFeedAd.showPrivacyActivity();
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