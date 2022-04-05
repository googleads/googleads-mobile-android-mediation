package com.google.ads.mediation.nend;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Map;
import net.nend.android.NendAdNativeMediaStateListener;
import net.nend.android.NendAdNativeMediaView;
import net.nend.android.NendAdNativeVideo;
import net.nend.android.NendAdNativeVideoListener;

public class NendUnifiedNativeVideoAdMapper extends NendUnifiedNativeAdMapper
    implements NendAdNativeVideoListener, NendAdNativeMediaStateListener {

  private static final int VERTICAL = 1;
  private static final float RATIO_9_TO_16 = 9.0f / 16.0f;
  private static final float RATIO_16_TO_9 = 16.0f / 9.0f;

  private NendAdNativeMediaView mediaView;
  private NendAdNativeVideo nativeVideo;
  private NendNativeAdForwarder forwarder;

  NendUnifiedNativeVideoAdMapper(Context context, NendNativeAdForwarder forwarder,
      NendAdNativeVideo ad) {
    super(new NendNativeMappedImage(context, ad.getLogoImageBitmap(),
        Uri.parse(ad.getLogoImageUrl())));
    this.forwarder = forwarder;

    // Note: NendAdNativeMediaView handles Click Event for changing action by VideoClickOption.
    // https://github.com/fan-ADN/nendSDK-Android/wiki/Implementation-for-native-video-ads#information-necessary-for-instance-generation
    setOverrideClickHandling(true);

    setAdvertiser(ad.getAdvertiserName());
    setHeadline(ad.getTitleText());
    setBody(ad.getDescriptionText());
    setStarRating((double) ad.getUserRating());
    setCallToAction(ad.getCallToActionText());

    nativeVideo = ad;
    nativeVideo.setListener(this);

    setMediaContentAspectRatio(
        ad.getVideoOrientation() == VERTICAL ? RATIO_9_TO_16 : RATIO_16_TO_9);
    setHasVideoContent(true);
    mediaView = new NendAdNativeMediaView(context);
    mediaView.setMediaStateListener(this);
    setMediaView(mediaView);
    mediaView.setMedia(ad);
  }

  private void layoutMediaView(View containerView) {
    int containerViewWidth = containerView.getWidth();
    int containerViewHeight = containerView.getHeight();
    if (containerViewWidth <= 0 || containerViewHeight <= 0) {
      return;
    }

    boolean isContainerViewLandscape = containerViewWidth > containerViewHeight;
    ViewGroup.LayoutParams params = mediaView.getLayoutParams();

    if (mediaView.getWidth() == 0
        && params.width == ViewGroup.LayoutParams.MATCH_PARENT
        && mediaView.getHeight() == 0
        && params.height == ViewGroup.LayoutParams.MATCH_PARENT) {
      // Note: Below codes are fitting into the "containerView" as NendAdNativeMediaView's
      // aspect ratio. Because NendAdNativeMediaView needs parent`s frame for measuring own
      // texture frame size.
      if (containerViewWidth == containerViewHeight) {
        mediaView.setMinimumWidth(containerViewWidth);
        mediaView.setMinimumHeight(containerViewHeight);
      } else {
        int width =
            getOffsetSide(containerViewHeight, containerViewWidth, isContainerViewLandscape);
        int height =
            getOffsetSide(containerViewWidth, containerViewHeight, !isContainerViewLandscape);
        mediaView.setMinimumWidth(width);
        mediaView.setMinimumHeight(height);
      }

      mediaView.invalidate();
    }
  }

  private int getOffsetSide(int base, int otherSide, boolean isOffsetSide) {
    if (!isOffsetSide) {
      return otherSide;
    }
    return (int) (base / RATIO_16_TO_9);
  }

  void deactivate() {
    if (nativeVideo != null) {
      nativeVideo.unregisterInteractionViews();
      nativeVideo.deactivate();
      nativeVideo = null;
    }
  }

  @Override
  public void trackViews(
      View containerView,
      Map<String, View> clickableAssetViews,
      Map<String, View> nonClickableAssetViews) {
    super.trackViews(containerView, clickableAssetViews, nonClickableAssetViews);
    nativeVideo.registerInteractionViews(new ArrayList<>(clickableAssetViews.values()));
    layoutMediaView(containerView);
  }

  @Override
  public void untrackView(View view) {
    if (nativeVideo != null) {
      nativeVideo.unregisterInteractionViews();
    }
    super.untrackView(view);
  }

  /**
   * {@link NendAdNativeVideoListener} implementation
   */
  @Override
  public void onImpression(@NonNull NendAdNativeVideo nendAdNativeVideo) {
    forwarder.adImpression();
  }

  @Override
  public void onClickAd(@NonNull NendAdNativeVideo nendAdNativeVideo) {
    forwarder.adClicked();
    forwarder.leftApplication();
  }

  @Override
  public void onClickInformation(@NonNull NendAdNativeVideo nendAdNativeVideo) {
    forwarder.leftApplication();
  }

  /**
   * {@link NendAdNativeMediaStateListener} implementation
   */
  @Override
  public void onStartPlay(@NonNull NendAdNativeMediaView nendAdNativeMediaView) {
    // Do nothing here
  }

  @Override
  public void onStopPlay(@NonNull NendAdNativeMediaView nendAdNativeMediaView) {
    // Do nothing here
  }

  @Override
  public void onOpenFullScreen(@NonNull NendAdNativeMediaView nendAdNativeMediaView) {
    forwarder.adOpened();
  }

  @Override
  public void onCloseFullScreen(@NonNull NendAdNativeMediaView nendAdNativeMediaView) {
    forwarder.adClosed();
  }

  @Override
  public void onStartFullScreenPlay(@NonNull NendAdNativeMediaView nendAdNativeMediaView) {
    // Do nothing here
  }

  @Override
  public void onStopFullScreenPlay(@NonNull NendAdNativeMediaView nendAdNativeMediaView) {
    // Do nothing here
  }

  @Override
  public void onCompletePlay(@NonNull NendAdNativeMediaView nendAdNativeMediaView) {
    forwarder.endVideo();
  }

  @Override
  public void onError(int errorCode, @NonNull String errorMessage) {
    forwarder.endVideo();
  }
}
