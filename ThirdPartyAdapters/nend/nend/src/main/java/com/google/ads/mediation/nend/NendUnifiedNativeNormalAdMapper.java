package com.google.ads.mediation.nend;

import static com.google.ads.mediation.nend.NendMediationAdapter.TAG;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.nend.android.NendAdNative;
import net.nend.android.NendAdNativeListener;
import net.nend.android.internal.connectors.NendNativeAdConnector;
import net.nend.android.internal.connectors.NendNativeAdConnectorFactory;

public class NendUnifiedNativeNormalAdMapper extends NendUnifiedNativeAdMapper
    implements NendAdNativeListener {

  private final NendAdNative nendAd;
  private TextView adChoicesMappingView;
  private NendNativeAdForwarder forwarder;
  private NendNativeAdConnector connector;

  /**
   * Creates a {@link UnifiedNativeAdMapper} for nend's non-video native ads.
   *
   * @param context   the context used for native ads.
   * @param forwarder the forwarder for native ad events.
   * @param ad        nend's native ad object.
   * @param adImage   the native ad image. nend's "text-only" native ad format supports a {@code
   *                  null} ad image.
   * @param logoImage the native ad logo image. nend's "text-only" native ad format supports a
   *                  {@code null} logo image.
   */
  NendUnifiedNativeNormalAdMapper(@NonNull Context context,
      @NonNull NendNativeAdForwarder forwarder, @NonNull NendAdNative ad,
      @Nullable NendNativeMappedImage adImage, @Nullable NendNativeMappedImage logoImage) {
    super(logoImage);
    this.forwarder = forwarder;
    nendAd = ad;
    connector = NendNativeAdConnectorFactory.createNativeAdConnector(ad);

    setAdvertiser(ad.getPromotionName());
    setHeadline(ad.getTitleText());
    setBody(ad.getContentText());
    setCallToAction(ad.getActionText());

    ImageView imageView = new ImageView(context);
    if (adImage == null) {
      Log.w(TAG, "Missing Image of nend's native ad, so MediaView will be unavailable...");
    } else {
      List<NativeAd.Image> imagesList = new ArrayList<>();
      imagesList.add(adImage);
      setImages(imagesList);

      Drawable drawable = adImage.getDrawable();
      if (drawable != null) {
        imageView.setAdjustViewBounds(true);
        imageView.setImageDrawable(drawable);
      }
    }
    setMediaView(imageView);
    adChoicesMappingView = new TextView(context);
    adChoicesMappingView.setText(NendAdNative.AdvertisingExplicitly.PR.getText());
    setAdChoicesContent(adChoicesMappingView);

    nendAd.setNendAdNativeListener(this);
  }

  @Override
  public void trackViews(
      View containerView,
      Map<String, View> clickableAssetViews,
      Map<String, View> nonClickableAssetViews) {
    super.trackViews(containerView, clickableAssetViews, nonClickableAssetViews);
    nendAd.activate(containerView, adChoicesMappingView);
  }

  @Override
  public void handleClick(View view) {
    super.handleClick(view);
    Context context = forwarder.getContextFromWeakReference();
    if (context instanceof Activity) {
      connector.performAdClick((Activity) context);
      forwarder.leftApplication();
    } else {
      Log.w(TAG, "This native ads is not applied on Activity");
    }
  }

  /**
   * {@link NendAdNativeListener} implementation
   */
  @Override
  public void onImpression(@NonNull NendAdNative nendAdNative) {
    forwarder.adImpression();
  }

  @Override
  public void onClickAd(@NonNull NendAdNative nendAdNative) {
    // Note : never listened this event because NendUnifiedNativeNormalAdMapper did not override
    // click handling.
  }

  @Override
  public void onClickInformation(@NonNull NendAdNative nendAdNative) {
    forwarder.leftApplication();
  }
}
