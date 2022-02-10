package com.google.ads.mediation.pangle.rtb;


import static com.google.ads.mediation.pangle.PangleConstant.ERROR_INVALID_PLACEMENT;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

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
import com.google.ads.mediation.pangle.PangleConstant;
import com.google.ads.mediation.pangle.PangleMediationAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdCallback;
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.google.android.gms.ads.nativead.NativeAdAssetNames;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PangleRtbNativeAd extends UnifiedNativeAdMapper {

    private static final String TAG = PangleRtbNativeAd.class.getSimpleName();
    private static final double PANGLE_SDK_IMAGE_SCALE = 1.0;
    private final MediationNativeAdConfiguration adConfiguration;
    private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> adLoadCallback;
    private final Context context;
    private NativeAdOptions nativeAdOptions;
    private MediationNativeAdCallback callback;

    public PangleRtbNativeAd(@NonNull MediationNativeAdConfiguration mediationNativeAdConfiguration,
                             @NonNull MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mediationAdLoadCallback) {
        adConfiguration = mediationNativeAdConfiguration;
        adLoadCallback = mediationAdLoadCallback;
        context = mediationNativeAdConfiguration.getContext();
    }

    public void render() {
        PangleMediationAdapter.setCoppa(adConfiguration);
        String placementID = adConfiguration.getServerParameters().getString(PangleConstant.PLACEMENT_ID);

        if (TextUtils.isEmpty(placementID)) {
            AdError error = PangleConstant.createAdapterError(ERROR_INVALID_PLACEMENT,
                    "Failed to request ad. PlacementID is null or empty.");
            Log.e(TAG, error.getMessage());
            adLoadCallback.onFailure(error);
            return;
        }

        String bidResponse = adConfiguration.getBidResponse();

        nativeAdOptions = adConfiguration.getNativeAdOptions();

        //(notice : make sure the Pangle sdk had been initialized) obtain Pangle ad manager
        TTAdManager mTTAdManager = PangleMediationAdapter.getPangleSdkManager();
        TTAdNative mTTAdNative = mTTAdManager.createAdNative(adConfiguration.getContext().getApplicationContext());

        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(placementID)
                .setAdCount(1) //ad count from 1 to 3
                .withBid(bidResponse)
                .build();

        mTTAdNative.loadFeedAd(adSlot, new TTAdNative.FeedAdListener() {
            @Override
            public void onError(int errorCode, String message) {
                Log.e(TAG, "feedAdListener loaded fail .code=" + errorCode + ",message=" + message);
                if (adLoadCallback != null) {
                    adLoadCallback.onFailure(PangleConstant.createSdkError(errorCode, message));
                }
            }

            @Override
            public void onFeedAdLoad(List<TTFeedAd> ads) {
                if (ads == null || ads.size() == 0) {
                    if (adLoadCallback != null) {
                        adLoadCallback.onFailure(PangleConstant.createSdkError(PangleConstant.ERROR_AD_NOT_FILL,
                                "feedAdListener loaded success. but ad no fill "));
                    }
                    Log.d(TAG, "feedAdListener loaded success. but ad no fill ");
                    return;
                }
                callback = adLoadCallback.onSuccess(new PangleNativeAd(ads.get(0)));
            }
        });

    }

    class PangleNativeAd extends UnifiedNativeAdMapper {
        private TTFeedAd mPangleAd;

        private PangleNativeAd(TTFeedAd ad) {
            this.mPangleAd = ad;
            //set data
            setHeadline(mPangleAd.getTitle());
            setBody(mPangleAd.getDescription());
            setCallToAction(mPangleAd.getButtonText());
            if (mPangleAd.getIcon() != null && mPangleAd.getIcon().isValid()) {
                setIcon(new PangleNativeMappedImage(null, Uri.parse(mPangleAd.getIcon().getImageUrl()), PANGLE_SDK_IMAGE_SCALE));
            }
            //set ad image
            if (mPangleAd.getImageList() != null && mPangleAd.getImageList().size() != 0) {
                List<NativeAd.Image> imagesList = new ArrayList<>();
                for (TTImage ttImage : mPangleAd.getImageList()) {
                    if (ttImage.isValid()) {
                        imagesList.add(new PangleNativeMappedImage(null, Uri.parse(ttImage.getImageUrl()),
                                PANGLE_SDK_IMAGE_SCALE));
                    }
                }
                setImages(imagesList);
            }

            /**Pangle does its own show event handling and click event handling*/
            setOverrideImpressionRecording(true);
            setOverrideClickHandling(true);

            /** add Native Feed Main View */
            MediaView mediaView = new MediaView(context);
            MediationAdapterUtil.addNativeFeedMainView(context, ad.getImageMode(), mediaView, ad.getAdView(), ad.getImageList());
            setMediaView(mediaView);

            // set logo
            setAdChoicesContent(mPangleAd.getAdLogoView());

            if (mPangleAd.getImageMode() == TTAdConstant.IMAGE_MODE_VIDEO ||
                    mPangleAd.getImageMode() == TTAdConstant.IMAGE_MODE_VIDEO_VERTICAL ||
                    mPangleAd.getImageMode() == TTAdConstant.IMAGE_MODE_VIDEO_SQUARE) {
                setHasVideoContent(true);
                mPangleAd.setVideoAdListener(new TTFeedAd.VideoAdListener() {
                    @Override
                    public void onVideoLoad(TTFeedAd ad) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onVideoError(int errorCode, int extraCode) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                        String errorMessage = String.format("Native ad video playback error," +
                                " errorCode: %s, extraCode: %s.", errorCode, extraCode);
                        Log.w(TAG, errorMessage);
                    }

                    @Override
                    public void onVideoAdStartPlay(TTFeedAd ad) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onVideoAdPaused(TTFeedAd ad) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onVideoAdContinuePlay(TTFeedAd ad) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onProgressUpdate(long current, long duration) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }

                    @Override
                    public void onVideoAdComplete(TTFeedAd ad) {
                        // Google Mobile Ads SDK doesn't have a matching event. Do nothing.
                    }
                });
            }
        }

        @Override
        public void trackViews(View view,
                               Map<String, View> clickableAssetViews,
                               Map<String, View> nonClickableAssetViews) {

            //set click interaction
            ArrayList<View> assetViews = new ArrayList<>(clickableAssetViews.values());
            View creativeBtn = clickableAssetViews.get(NativeAdAssetNames.ASSET_CALL_TO_ACTION);
            ArrayList<View> creativeViews = new ArrayList<>();
            if (creativeBtn != null) {
                creativeViews.add(creativeBtn);
            }
            if (mPangleAd != null) {
                mPangleAd.registerViewForInteraction((ViewGroup) view, assetViews, creativeViews, new TTNativeAd.AdInteractionListener() {

                    @Override
                    public void onAdClicked(View view, TTNativeAd ad) {

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
            }
        }
    }

    public class PangleNativeMappedImage extends NativeAd.Image {

        private final Drawable drawable;
        private final Uri imageUri;
        private final double scale;

        private PangleNativeMappedImage(Drawable drawable, Uri imageUri, double scale) {
            this.drawable = drawable;
            this.imageUri = imageUri;
            this.scale = scale;
        }

        @Override
        public Drawable getDrawable() {
            return drawable;
        }

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
