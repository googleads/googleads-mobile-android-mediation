package com.google.ads.mediation.vungle.rtb;

import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.KEY_APP_ID;
import static com.google.ads.mediation.vungle.VungleMediationAdapter.TAG;

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
import com.google.ads.mediation.vungle.VungleInitializer;
import com.google.ads.mediation.vungle.VungleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.formats.NativeAd.Image;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdAssetNames;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.vungle.ads.AdConfig;
import com.vungle.ads.BaseAd;
import com.vungle.ads.NativeAd;
import com.vungle.ads.NativeAdListener;
import com.vungle.ads.VungleException;
import com.vungle.ads.internal.ui.view.MediaView;
import com.vungle.mediation.PlacementFinder;
import com.vungle.mediation.VungleExtrasBuilder;
import java.util.ArrayList;
import java.util.Map;

public class VungleRtbNativeAd extends UnifiedNativeAdMapper implements NativeAdListener {

  private final MediationNativeAdConfiguration adConfiguration;
  private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback;
  private MediationNativeAdCallback nativeAdCallback;

  private NativeAd nativeAd;
  private MediaView mediaView;

  public VungleRtbNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
      @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> callback) {
    this.adConfiguration = mediationNativeAdConfiguration;
    this.callback = callback;
  }

  public void render() {
    Bundle mediationExtras = adConfiguration.getMediationExtras();
    Bundle serverParameters = adConfiguration.getServerParameters();
    NativeAdOptions nativeAdOptions = adConfiguration.getNativeAdOptions();
    Context context = adConfiguration.getContext();

    String appID = serverParameters.getString(KEY_APP_ID);
    if (TextUtils.isEmpty(appID)) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or invalid app ID.", ERROR_DOMAIN);
      Log.d(TAG, error.toString());
      callback.onFailure(error);
      return;
    }

    String placementId = PlacementFinder.findPlacement(mediationExtras, serverParameters);
    if (placementId == null || placementId.isEmpty()) {
      AdError error = new AdError(ERROR_INVALID_SERVER_PARAMETERS,
          "Failed to load ad from Vungle. Missing or Invalid placement ID.", ERROR_DOMAIN);
      Log.d(TAG, error.toString());
      callback.onFailure(error);
      return;
    }

    String adMarkup = adConfiguration.getBidResponse();

    AdConfig adConfig = VungleExtrasBuilder.adConfigWithNetworkExtras(mediationExtras,
        nativeAdOptions);

    Log.d(TAG, "start to render native ads...");

    VungleInitializer.getInstance()
        .initialize(
            appID,
            context,
            new VungleInitializer.VungleInitializationListener() {
              @Override
              public void onInitializeSuccess() {
                nativeAd = new NativeAd(context, placementId, adConfig);
                nativeAd.setAdListener(VungleRtbNativeAd.this);
                mediaView = new MediaView(context);

                nativeAd.load(adMarkup);
              }

              @Override
              public void onInitializeError(AdError error) {
                Log.d(TAG, error.toString());
                callback.onFailure(error);
              }
            });
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

    if (nativeAd == null || !nativeAd.canPlayAd()) {
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
    setMediaView(mediaView);

    String iconUrl = nativeAd.getAppIcon();
    if (iconUrl.startsWith("file://")) {
      setIcon(new VungleNativeMappedImage(Uri.parse(iconUrl)));
    }

    setOverrideImpressionRecording(true);
    setOverrideClickHandling(true);
  }

  @Override
  public void adClick(@NonNull BaseAd baseAd) {
    if (nativeAdCallback != null) {
      nativeAdCallback.reportAdClicked();
      nativeAdCallback.onAdOpened();
    }
  }

  @Override
  public void adEnd(@NonNull BaseAd baseAd) {
    // no-op
  }

  @Override
  public void adImpression(@NonNull BaseAd baseAd) {
    if (nativeAdCallback != null) {
      nativeAdCallback.reportAdImpression();
    }
  }

  @Override
  public void adLoaded(@NonNull BaseAd baseAd) {
    mapNativeAd();
    nativeAdCallback = callback.onSuccess(VungleRtbNativeAd.this);
  }

  @Override
  public void adStart(@NonNull BaseAd baseAd) {
    // no-op
  }

  @Override
  public void error(@NonNull BaseAd baseAd, @NonNull VungleException e) {
    AdError error = VungleMediationAdapter.getAdError(e);
    callback.onFailure(error);
  }

  @Override
  public void onAdLeftApplication(@NonNull BaseAd baseAd) {
    if (nativeAdCallback != null) {
      nativeAdCallback.onAdLeftApplication();
    }
  }

  private static class VungleNativeMappedImage extends Image {

    private final Uri imageUri;

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
