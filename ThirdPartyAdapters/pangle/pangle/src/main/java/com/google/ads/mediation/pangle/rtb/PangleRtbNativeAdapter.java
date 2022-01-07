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

public class PangleRtbNativeAdapter extends UnifiedNativeAdMapper {
    private static final String TAG = "PangleRtbNativeAdapter";
    private static final double PANGLE_SDK_IMAGE_SCALE = 1.0;
    private final MediationNativeAdConfiguration mAdConfiguration;
    private final MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mAdLoadCallback;
    private final Context mContext;
    private NativeAdOptions mNativeAdOptions;
    private MediationNativeAdCallback callback;

    public PangleRtbNativeAdapter(MediationNativeAdConfiguration mediationNativeAdConfiguration, MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> mediationAdLoadCallback) {
        mAdConfiguration = mediationNativeAdConfiguration;
        mAdLoadCallback = mediationAdLoadCallback;
        mContext = mediationNativeAdConfiguration.getContext();
    }

    public void render() {
        PangleMediationAdapter.setCoppa(mAdConfiguration);
        String placementID = mAdConfiguration.getServerParameters().getString(PangleConstant.PLACEMENT_ID);

        if (TextUtils.isEmpty(placementID)) {
            AdError error = PangleConstant.createAdapterError(ERROR_INVALID_PLACEMENT,
                    "Failed to request ad. PlacementID is null or empty.");
            Log.e(TAG, error.getMessage());
            mAdLoadCallback.onFailure(error);
            return;
        }

        String bidResponse = mAdConfiguration.getBidResponse();

        mNativeAdOptions = mAdConfiguration.getNativeAdOptions();

        //(notice : make sure the Pangle sdk had been initialized) obtain Pangle ad manager
        TTAdManager mTTAdManager = PangleMediationAdapter.getPangleSdkManager();
        TTAdNative mTTAdNative = mTTAdManager.createAdNative(mAdConfiguration.getContext().getApplicationContext());

        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(placementID)
                .setAdCount(1) //ad count from 1 to 3
                .withBid(bidResponse)
                .build();

        mTTAdNative.loadFeedAd(adSlot, new TTAdNative.FeedAdListener() {
            @Override
            public void onError(int errorCode, String message) {
                Log.e(TAG, "feedAdListener loaded fail .code=" + errorCode + ",message=" + message);
                if (mAdLoadCallback != null) {
                    mAdLoadCallback.onFailure(PangleConstant.createSdkError(errorCode, message));
                }
            }

            @Override
            public void onFeedAdLoad(List<TTFeedAd> ads) {
                if (ads == null || ads.size() == 0) {
                    if (mAdLoadCallback != null) {
                        mAdLoadCallback.onFailure(PangleConstant.createSdkError(AdRequest.ERROR_CODE_NO_FILL,
                                "feedAdListener loaded success. but ad no fill "));
                    }
                    Log.d(TAG, "feedAdListener loaded success. but ad no fill ");
                    return;
                }
                callback = mAdLoadCallback.onSuccess(new PangleNativeAd(ads.get(0)));
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

            Bundle extras = new Bundle();
            this.setExtras(extras);

            /**Pangle does its own show event handling and click event handling*/
            setOverrideImpressionRecording(true);
            setOverrideClickHandling(true);


            /** add Native Feed Main View */
            MediaView mediaView = new MediaView(mContext);
            MediationAdapterUtil.addNativeFeedMainView(mContext, ad.getImageMode(), mediaView, ad.getAdView(), ad.getImageList());
            setMediaView(mediaView);


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
                        if (callback != null) {
                            callback.reportAdClicked();
                        }

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


            // set logo
            ViewGroup adView = (ViewGroup) view;

            View overlayView = adView.getChildAt(adView.getChildCount() - 1);
            if (overlayView instanceof FrameLayout) {
                int privacyIconPlacement = NativeAdOptions.ADCHOICES_TOP_RIGHT;
                if (mNativeAdOptions != null) {
                    privacyIconPlacement = mNativeAdOptions.getAdChoicesPlacement();
                }
                final Context context = view.getContext();
                if (context == null) {
                    return;
                }

                ImageView privacyInformationIconImageView = null;
                if (mPangleAd != null) {
                    privacyInformationIconImageView = (ImageView) mPangleAd.getAdLogoView();
                }

                if (privacyInformationIconImageView != null) {
                    privacyInformationIconImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // jump to privacy
                            mPangleAd.showPrivacyActivity();
                            Log.d(TAG, "privacyInformationIconImageView--》click");
                        }
                    });

                    privacyInformationIconImageView.setVisibility(View.VISIBLE);
                    ((ViewGroup) overlayView).addView(privacyInformationIconImageView);

                    float scale = context.getResources().getDisplayMetrics().density;
                    int iconSizePx = (int) ((10 * scale + 0.5f) * scale + 0.5);

                    FrameLayout.LayoutParams params =
                            new FrameLayout.LayoutParams(iconSizePx, iconSizePx);

                    switch (privacyIconPlacement) {
                        case NativeAdOptions.ADCHOICES_TOP_LEFT:
                            params.gravity = Gravity.TOP | Gravity.START;
                            break;
                        case NativeAdOptions.ADCHOICES_BOTTOM_RIGHT:
                            params.gravity = Gravity.BOTTOM | Gravity.END;
                            break;
                        case NativeAdOptions.ADCHOICES_BOTTOM_LEFT:
                            params.gravity = Gravity.BOTTOM | Gravity.START;
                            break;
                        case NativeAdOptions.ADCHOICES_TOP_RIGHT:
                            params.gravity = Gravity.TOP | Gravity.END;
                            break;
                        default:
                            params.gravity = Gravity.TOP | Gravity.END;
                    }
                    privacyInformationIconImageView.setLayoutParams(params);
                }
                adView.requestLayout();

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
