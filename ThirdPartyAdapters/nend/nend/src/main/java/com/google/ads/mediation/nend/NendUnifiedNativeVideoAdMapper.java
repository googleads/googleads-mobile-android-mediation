package com.google.ads.mediation.nend;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import net.nend.android.NendAdNativeMediaStateListener;
import net.nend.android.NendAdNativeMediaView;
import net.nend.android.NendAdNativeVideo;
import net.nend.android.NendAdNativeVideoListener;

import java.util.ArrayList;
import java.util.Map;

public class NendUnifiedNativeVideoAdMapper extends NendUnifiedNativeAdMapper
        implements NendAdNativeVideoListener, NendAdNativeMediaStateListener {
    private NendAdNativeMediaView mediaView;
    private NendAdNativeVideo nativeVideo;
    private NendNativeAdForwarder forwarder;
    private Handler handler = new Handler(Looper.getMainLooper());

    private static final int VERTICAL = 1;
    private static final float RATIO_9_TO_16 = 9.0f / 16.0f;
    private static final float RATIO_16_TO_9 = 16.0f / 9.0f;

    NendUnifiedNativeVideoAdMapper(NendNativeAdForwarder forwarder, NendAdNativeVideo ad) {
        super(new NendNativeMappedImage(
                forwarder.contextWeakReference.get(), ad.getLogoImageBitmap(), Uri.parse(ad.getLogoImageUrl())));
        this.forwarder = forwarder;
        setOverrideClickHandling(true);

        setAdvertiser(ad.getAdvertiserName());
        setHeadline(ad.getTitleText());
        setBody(ad.getDescriptionText());
        setStarRating((double) ad.getUserRating());
        setCallToAction(ad.getCallToActionText());

        nativeVideo = ad;
        nativeVideo.setListener(this);

        setMediaContentAspectRatio(ad.getVideoOrientation() == VERTICAL
                ? RATIO_9_TO_16
                : RATIO_16_TO_9
        );
        setHasVideoContent(true);
        mediaView = new NendAdNativeMediaView(forwarder.contextWeakReference.get());
        mediaView.setMediaStateListener(this);
        setMediaView(mediaView);
        mediaView.setMedia(ad);
    }

    private void invalidateMediaViewIfNeed(View containerView) {
        int containerViewWidth = containerView.getWidth();
        int containerViewHeight = containerView.getHeight();
        if (containerViewWidth <= 0 || containerViewHeight <= 0) {
            return;
        }

        boolean isContainerViewLandscape = containerViewWidth > containerViewHeight;
        ViewGroup.LayoutParams params = mediaView.getLayoutParams();

        if (mediaView.getWidth() == 0 && params.width == ViewGroup.LayoutParams.MATCH_PARENT
                && mediaView.getHeight() == 0 && params.height == ViewGroup.LayoutParams.MATCH_PARENT) {
            if (containerViewWidth == containerViewHeight) {
                mediaView.setMinimumWidth(containerViewWidth);
                mediaView.setMinimumHeight(containerViewHeight);
            } else {
                int width = (
                        isContainerViewLandscape
                                ? containerViewWidth
                                : (
                                nativeVideo.getVideoOrientation() == VERTICAL
                                        ? (int)(containerViewHeight * RATIO_9_TO_16)
                                        : (int)(containerViewHeight / RATIO_16_TO_9)
                        )
                );
                int height = (
                        !isContainerViewLandscape
                                ? containerViewHeight
                                : (
                                nativeVideo.getVideoOrientation() == VERTICAL
                                        ? (int)(containerViewWidth * RATIO_9_TO_16)
                                        : (int)(containerViewWidth / RATIO_16_TO_9)
                        )
                );
                mediaView.setMinimumWidth(width);
                mediaView.setMinimumHeight(height);
            }

            mediaView.invalidate();
        }
    }

    void deactivate() {
        if (nativeVideo != null) {
            nativeVideo.deactivate();
            nativeVideo = null;
        }
    }

    @Override
    public void trackViews(View containerView,
                           Map<String, View> clickableAssetViews,
                           Map<String, View> nonClickableAssetViews) {
        super.trackViews(containerView, clickableAssetViews, nonClickableAssetViews);
        nativeVideo.registerInteractionViews(new ArrayList<>(clickableAssetViews.values()));
        invalidateMediaViewIfNeed(containerView);
    }

    @Override
    public void untrackView(View view) {
        nativeVideo.unregisterInteractionViews();
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

        //Note : Why can't App listen multiple event without delay seconder event...
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                forwarder.leftApplication();
            }
        }, 1000);
    }

    @Override
    public void onClickInformation(@NonNull NendAdNativeVideo nendAdNativeVideo) {
        forwarder.informationClicked();
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
