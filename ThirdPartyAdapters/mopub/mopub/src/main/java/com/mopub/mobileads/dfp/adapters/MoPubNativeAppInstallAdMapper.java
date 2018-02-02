
package com.mopub.mobileads.dfp.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;
import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;
import com.mopub.common.util.Drawables;
import com.mopub.nativeads.NativeImageHelper;
import com.mopub.nativeads.StaticNativeAd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A {@link NativeAppInstallAdMapper} used to map a MoPub static native ad to Google native app
 * install ad.
 */
public class MoPubNativeAppInstallAdMapper extends NativeAppInstallAdMapper {

    private StaticNativeAd mMopubNativeAdData;
    private int privacyIconPlacement;
    private ImageView privacyInformationIconImageView;
    private int mPrivacyIconSize;

    public MoPubNativeAppInstallAdMapper(StaticNativeAd ad, HashMap<String, Drawable>
            drawableMap, int privacyIconPlacementParam, int privacyIconSize) {
        mMopubNativeAdData = ad;
        setHeadline(mMopubNativeAdData.getTitle());
        setBody(mMopubNativeAdData.getText());
        setCallToAction(mMopubNativeAdData.getCallToAction());
        privacyIconPlacement = privacyIconPlacementParam;
        mPrivacyIconSize = privacyIconSize;

        if (drawableMap != null) {
            setIcon(new MoPubNativeMappedImage(
                    drawableMap.get(DownloadDrawablesAsync.KEY_ICON),
                    mMopubNativeAdData.getIconImageUrl(),
                    MoPubAdapter.DEFAULT_MOPUB_IMAGE_SCALE));

            List<NativeAd.Image> imagesList = new ArrayList<NativeAd.Image>();
            imagesList.add(new MoPubNativeMappedImage(
                    drawableMap.get(DownloadDrawablesAsync.KEY_IMAGE),
                    mMopubNativeAdData.getMainImageUrl(),
                    MoPubAdapter.DEFAULT_MOPUB_IMAGE_SCALE));
            setImages(imagesList);
        } else {

            setIcon(new MoPubNativeMappedImage(null,
                    mMopubNativeAdData.getIconImageUrl(), MoPubAdapter.DEFAULT_MOPUB_IMAGE_SCALE));

            List<NativeAd.Image> imagesList = new ArrayList<NativeAd.Image>();
            imagesList.add(new MoPubNativeMappedImage(null,
                    mMopubNativeAdData.getMainImageUrl(), MoPubAdapter.DEFAULT_MOPUB_IMAGE_SCALE));
            setImages(imagesList);
        }
        setOverrideClickHandling(true);
        setOverrideImpressionRecording(true);
    }

    @Override
    public void untrackView(View view) {
        super.untrackView(view);
        mMopubNativeAdData.clear(view);

        if (privacyInformationIconImageView != null && (ViewGroup)
                privacyInformationIconImageView.getParent() != null) {
            ((ViewGroup) privacyInformationIconImageView.getParent())
                    .removeView(privacyInformationIconImageView);
        }
    }

    public void trackView(View view) {

        mMopubNativeAdData.prepare(view);

        if (!(view instanceof ViewGroup)) {
            return;
        }

        ViewGroup adView = (ViewGroup) view;

        View overlayView = adView.getChildAt(adView.getChildCount() - 1);
        if (overlayView instanceof FrameLayout) {

            final Context context = view.getContext();
            if (context == null) {
                return;
            }

            privacyInformationIconImageView = new ImageView(context);
            String privacyInformationImageUrl =
                    mMopubNativeAdData.getPrivacyInformationIconImageUrl();
            final String privacyInformationClickthroughUrl = mMopubNativeAdData
                    .getPrivacyInformationIconClickThroughUrl();

            if (privacyInformationImageUrl == null) {
                privacyInformationIconImageView.setImageDrawable(
                        Drawables.NATIVE_PRIVACY_INFORMATION_ICON.createDrawable(context));
            } else {
                NativeImageHelper.loadImageView(privacyInformationImageUrl,
                        privacyInformationIconImageView);
            }

            privacyInformationIconImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    new UrlHandler.Builder()
                            .withSupportedUrlActions(
                                    UrlAction.IGNORE_ABOUT_SCHEME,
                                    UrlAction.OPEN_NATIVE_BROWSER,
                                    UrlAction.OPEN_IN_APP_BROWSER,
                                    UrlAction.HANDLE_SHARE_TWEET,
                                    UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
                                    UrlAction.FOLLOW_DEEP_LINK)
                            .build().handleUrl(context, privacyInformationClickthroughUrl);
                }
            });
            privacyInformationIconImageView.setVisibility(View.VISIBLE);
            ((ViewGroup) overlayView).addView(privacyInformationIconImageView);

            float scale = context.getResources().getDisplayMetrics().density;
            int icon_size_px = (int) (mPrivacyIconSize * scale + 0.5);
            FrameLayout.LayoutParams params =
                    new FrameLayout.LayoutParams(icon_size_px, icon_size_px);

            switch (privacyIconPlacement) {
                case NativeAdOptions.ADCHOICES_TOP_LEFT:
                    params.gravity = Gravity.TOP | Gravity.LEFT;
                    break;
                case NativeAdOptions.ADCHOICES_BOTTOM_RIGHT:
                    params.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                    break;
                case NativeAdOptions.ADCHOICES_BOTTOM_LEFT:
                    params.gravity = Gravity.BOTTOM | Gravity.LEFT;
                    break;
                case NativeAdOptions.ADCHOICES_TOP_RIGHT:
                    params.gravity = Gravity.TOP | Gravity.RIGHT;
                    break;
                default:
                    params.gravity = Gravity.TOP | Gravity.RIGHT;
            }
            privacyInformationIconImageView.setLayoutParams(params);
            adView.requestLayout();
        } else {
            Log.d(MoPubAdapter.TAG, "Failed to show AdChoices icon.");
        }
    }

    @Override
    public void recordImpression() {
    }

    @Override
    public void handleClick(View view) {
    }
}
