package com.google.ads.mediation.mytarget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.MediaView;
import com.google.android.gms.ads.formats.NativeAd.Image;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.formats.NativeAppInstallAdView;
import com.google.android.gms.ads.formats.NativeContentAdView;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeAdMapper;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;
import com.google.android.gms.ads.mediation.NativeContentAdMapper;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.my.target.common.CustomParams;
import com.my.target.common.NavigationType;
import com.my.target.common.models.ImageData;
import com.my.target.nativeads.NativeAd;
import com.my.target.nativeads.banners.NativePromoBanner;
import com.my.target.nativeads.views.MediaAdView;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

/**
 * A {@link MediationNativeAdapter} to load and show myTarget native ads.
 */
public class MyTargetNativeAdapter implements MediationNativeAdapter {
    public static final String EXTRA_KEY_AGE_RESTRICTIONS = "ageRestrictions";
    public static final String EXTRA_KEY_ADVERTISING_LABEL = "advertisingLabel";
    public static final String EXTRA_KEY_CATEGORY = "category";
    public static final String EXTRA_KEY_SUBCATEGORY = "subcategory";
    public static final String EXTRA_KEY_VOTES = "votes";

    private static final String PARAM_NATIVE_TYPE_REQUEST = "at";
    private static final String PARAM_INSTALL_ONLY = "1";
    private static final String PARAM_CONTENT_ONLY = "2";
    private static final String TAG = "MyTargetNativeAdapter";

    @Nullable
    private MediationNativeListener customEventNativeListener;

    private static int findMediaAdViewPosition(@NonNull List<View> clickableViews,
                                               @NonNull MediaAdView mediaAdView) {
        for (int i = 0; i < clickableViews.size(); i++) {
            View view = clickableViews.get(i);
            if (view instanceof MediaView) {
                MediaView mediaView = (MediaView) view;
                int childCount = mediaView.getChildCount();
                for (int j = 0; j < childCount; j++) {
                    View innerView = mediaView.getChildAt(j);
                    if (innerView == mediaAdView) {
                        return i;
                    }
                }
                break;
            }
        }
        return -1;
    }

    @Override
    public void requestNativeAd(Context context,
            MediationNativeListener customEventNativeListener,
            Bundle serverParameter,
            NativeMediationAdRequest nativeMediationAdRequest,
            Bundle customEventExtras) {
        this.customEventNativeListener = customEventNativeListener;
        int slotId = MyTargetTools.checkAndGetSlotId(context, serverParameter);
        Log.d(TAG, "Requesting myTarget mediation, slotId: " + slotId);

        if (slotId < 0) {
            if (customEventNativeListener != null) {
                customEventNativeListener.onAdFailedToLoad(
                        MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }
        NativeAdOptions options = null;
        int gender = 0;
        Date birthday = null;
        boolean contentRequested = false;
        boolean installRequested = false;
        boolean unifiedRequested = false;
        if (nativeMediationAdRequest != null) {
            options = nativeMediationAdRequest.getNativeAdOptions();
            gender = nativeMediationAdRequest.getGender();
            birthday = nativeMediationAdRequest.getBirthday();
            contentRequested = nativeMediationAdRequest.isContentAdRequested();
            installRequested = nativeMediationAdRequest.isAppInstallAdRequested();
            unifiedRequested = nativeMediationAdRequest.isUnifiedNativeAdRequested();
        }

        NativeAd nativeAd = new NativeAd(slotId, context);

        boolean autoLoadImages = true;
        if (options != null) {
            autoLoadImages = !options.shouldReturnUrlsForImageAssets();
            Log.d(TAG, "Set autoload images to " + autoLoadImages);
        }
        nativeAd.setAutoLoadImages(autoLoadImages);

        CustomParams params = nativeAd.getCustomParams();
        Log.d(TAG, "Set gender to " + gender);
        params.setGender(gender);

        if (birthday != null && birthday.getTime() != -1) {
            GregorianCalendar calendar = new GregorianCalendar();
            GregorianCalendar calendarNow = new GregorianCalendar();

            calendar.setTimeInMillis(birthday.getTime());
            int age = calendarNow.get(GregorianCalendar.YEAR)
                    - calendar.get(GregorianCalendar.YEAR);
            if (age >= 0) {
                params.setAge(age);
            }
        }
        Log.d(TAG, "Content requested: " + contentRequested
                + ", install requested: " + installRequested
                + ", unified requested: " + unifiedRequested);
        if (!unifiedRequested && (!contentRequested || !installRequested)) {
            if (!contentRequested) {
                params.setCustomParam(PARAM_NATIVE_TYPE_REQUEST, PARAM_INSTALL_ONLY);
            } else {
                params.setCustomParam(PARAM_NATIVE_TYPE_REQUEST, PARAM_CONTENT_ONLY);
            }
        }

        MyTargetNativeAdListener nativeAdListener = new MyTargetNativeAdListener(
                nativeAd,
                nativeMediationAdRequest,
                context);

        params.setCustomParam(
                MyTargetTools.PARAM_MEDIATION_KEY, MyTargetTools.PARAM_MEDIATION_VALUE);
        nativeAd.setListener(nativeAdListener);
        nativeAd.load();
    }

    @Override
    public void onDestroy() {
        customEventNativeListener = null;
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    /**
     * A {@link Image} used to map a myTarget native ad image to a Google native ad image.
     */
    private static class MyTargetAdmobNativeImage extends Image {
        @NonNull
        private final Uri uri;
        @Nullable
        private Drawable drawable;

        MyTargetAdmobNativeImage(@NonNull ImageData imageData, @NonNull Resources resources) {
            Bitmap bitmap = imageData.getBitmap();
            if (bitmap != null) {
                drawable = new BitmapDrawable(resources, bitmap);
            }
            uri = Uri.parse(imageData.getUrl());
        }

        @Nullable
        @Override
        public Drawable getDrawable() {
            return drawable;
        }

        @NonNull
        @Override
        public Uri getUri() {
            return uri;
        }

        @Override
        public double getScale() {
            return 1;
        }
    }

    /**
     * A {@link NativeContentAdMapper} to map myTarget native ad to Google Mobile Ads SDK native
     * content ad.
     */
    private static class MyTargetNativeContentAdMapper extends NativeContentAdMapper {
        @NonNull
        private final NativeAd nativeAd;
        @NonNull
        private final MediaAdView mediaAdView;

        MyTargetNativeContentAdMapper(@NonNull NativeAd nativeAd,
                @NonNull Context context) {
            this.nativeAd = nativeAd;
            this.mediaAdView = new MediaAdView(context);
            setOverrideClickHandling(true);
            setOverrideImpressionRecording(true);
            NativePromoBanner banner = nativeAd.getBanner();
            if (banner == null) {
                return;
            }
            setBody(banner.getDescription());
            setCallToAction(banner.getCtaText());
            setHeadline(banner.getTitle());
            ImageData icon = banner.getIcon();
            if (icon != null && !TextUtils.isEmpty(icon.getUrl())) {
                setLogo(new MyTargetAdmobNativeImage(icon, context.getResources()));
            }
            setHasVideoContent(true);
            setMediaView(mediaAdView);
            ImageData image = banner.getImage();
            if (image != null && !TextUtils.isEmpty(image.getUrl())) {
                ArrayList<Image> imageArrayList = new ArrayList<>();
                imageArrayList.add(new MyTargetAdmobNativeImage(image, context.getResources()));
                setImages(imageArrayList);
            }
            setAdvertiser(banner.getDomain());

            Bundle extras = new Bundle();
            final String ageRestrictions = banner.getAgeRestrictions();
            if (!TextUtils.isEmpty(ageRestrictions)) {
                extras.putString(EXTRA_KEY_AGE_RESTRICTIONS, ageRestrictions);
            }
            final String advertisingLabel = banner.getAdvertisingLabel();
            if (!TextUtils.isEmpty(advertisingLabel)) {
                extras.putString(EXTRA_KEY_ADVERTISING_LABEL, advertisingLabel);
            }
            setExtras(extras);
        }

        @Override
        public void trackViews(final View containerView, final Map<String, View> clickables,
                Map<String, View> nonclickables) {
            final ArrayList<View> clickableViews = new ArrayList<>(clickables.values());
            containerView.post(new Runnable() {
                @Override
                public void run() {
                    int mediaPosition = findMediaAdViewPosition(clickableViews, mediaAdView);
                    if (mediaPosition >= 0) {
                        clickableViews.remove(mediaPosition);
                        clickableViews.add(mediaAdView);
                    }
                    nativeAd.registerView(containerView, clickableViews);
                }
            });
        }

        @Override
        public void trackView(final View view) {

            view.post(new Runnable() {
                @Override
                public void run() {
                    if (view instanceof NativeContentAdView) {
                        NativeContentAdView nativeContentAdView = (NativeContentAdView) view;
                        final ArrayList<View> assetViews = new ArrayList<>();

                        if (nativeContentAdView.getHeadlineView() != null) {
                            assetViews.add(nativeContentAdView.getHeadlineView());
                        }

                        if (nativeContentAdView.getBodyView() != null) {
                            assetViews.add(nativeContentAdView.getBodyView());
                        }

                        if (nativeContentAdView.getCallToActionView() != null) {
                            assetViews.add(nativeContentAdView.getCallToActionView());
                        }

                        if (nativeContentAdView.getAdvertiserView() != null) {
                            assetViews.add(nativeContentAdView.getAdvertiserView());
                        }

                        if (nativeContentAdView.getImageView() != null) {
                            assetViews.add(nativeContentAdView.getImageView());
                        }

                        if (nativeContentAdView.getLogoView() != null) {
                            assetViews.add(nativeContentAdView.getLogoView());
                        }

                        if (nativeContentAdView.getMediaView() != null) {
                            assetViews.add(mediaAdView);
                        }

                        nativeAd.registerView(view, assetViews);
                    } else {
                        Log.w(TAG, "Failed to register view for interaction.");
                    }

                }
            });
        }

        @Override
        public void untrackView(View view) {
            nativeAd.unregisterView();
        }
    }

    /**
     * A {@link MyTargetNativeUnifiedAdMapper} used to map myTarget native ad to Google Mobile
     * Ads SDK
     * native app unified ad.
     */
    private static class MyTargetNativeUnifiedAdMapper extends UnifiedNativeAdMapper {
        @NonNull
        private final NativeAd nativeAd;
        @NonNull
        private final MediaAdView mediaAdView;

        MyTargetNativeUnifiedAdMapper(@NonNull NativeAd nativeAd,
                @NonNull Context context) {
            this.nativeAd = nativeAd;
            this.mediaAdView = new MediaAdView(context);
            setOverrideClickHandling(true);
            setOverrideImpressionRecording(true);
            NativePromoBanner banner = nativeAd.getBanner();
            if (banner == null) {
                return;
            }
            setBody(banner.getDescription());
            setCallToAction(banner.getCtaText());
            setHeadline(banner.getTitle());
            ImageData icon = banner.getIcon();
            if (icon != null && !TextUtils.isEmpty(icon.getUrl())) {
                setIcon(new MyTargetAdmobNativeImage(icon, context.getResources()));
            }
            ImageData image = banner.getImage();
            setHasVideoContent(true);
            setMediaView(mediaAdView);
            if (image != null && !TextUtils.isEmpty(image.getUrl())) {
                ArrayList<Image> imageArrayList = new ArrayList<>();
                imageArrayList.add(new MyTargetAdmobNativeImage(image, context.getResources()));
                setImages(imageArrayList);
            }
            setAdvertiser(banner.getDomain());
            setStarRating((double) banner.getRating());
            setStore(null);
            setPrice(null);

            Bundle extras = new Bundle();
            final String ageRestrictions = banner.getAgeRestrictions();
            if (!TextUtils.isEmpty(ageRestrictions)) {
                extras.putString(EXTRA_KEY_AGE_RESTRICTIONS, ageRestrictions);
            }
            final String advertisingLabel = banner.getAdvertisingLabel();
            if (!TextUtils.isEmpty(advertisingLabel)) {
                extras.putString(EXTRA_KEY_ADVERTISING_LABEL, advertisingLabel);
            }
            final String category = banner.getCategory();
            if (!TextUtils.isEmpty(category)) {
                extras.putString(EXTRA_KEY_CATEGORY, category);
            }
            final String subCategory = banner.getSubCategory();
            if (!TextUtils.isEmpty(subCategory)) {
                extras.putString(EXTRA_KEY_SUBCATEGORY, subCategory);
            }
            final int votes = banner.getVotes();
            if (votes > 0) {
                extras.putInt(EXTRA_KEY_VOTES, votes);
            }
            setExtras(extras);
        }

        @Override
        public void trackViews(final View containerView, final Map<String, View> clickables,
                Map<String, View> nonclickables) {
            final ArrayList<View> clickableViews = new ArrayList<>(clickables.values());
            containerView.post(new Runnable() {
                @Override
                public void run() {
                    int mediaPosition = findMediaAdViewPosition(clickableViews, mediaAdView);
                    if (mediaPosition >=0 ) {
                        clickableViews.remove(mediaPosition);
                        clickableViews.add(mediaAdView);
                    }
                    nativeAd.registerView(containerView, clickableViews);
                }
            });
        }

        @Override
        public void untrackView(View view) {
            nativeAd.unregisterView();
        }
    }

    /**
     * A {@link NativeAppInstallAdMapper} used to map myTarget native ad to Google Mobile Ads SDK
     * native app install ad.
     */
    private static class MyTargetNativeInstallAdMapper extends NativeAppInstallAdMapper {
        @NonNull
        private final NativeAd nativeAd;
        @NonNull
        private final MediaAdView mediaAdView;

        MyTargetNativeInstallAdMapper(@NonNull NativeAd nativeAd,
                @NonNull Context context) {
            this.nativeAd = nativeAd;
            this.mediaAdView = new MediaAdView(context);
            setOverrideClickHandling(true);
            setOverrideImpressionRecording(true);
            NativePromoBanner banner = nativeAd.getBanner();
            if (banner == null) {
                return;
            }
            setBody(banner.getDescription());
            setCallToAction(banner.getCtaText());
            setHeadline(banner.getTitle());
            ImageData icon = banner.getIcon();
            if (icon != null && !TextUtils.isEmpty(icon.getUrl())) {
                setIcon(new MyTargetAdmobNativeImage(icon, context.getResources()));
            }
            ImageData image = banner.getImage();
            setHasVideoContent(true);
            setMediaView(mediaAdView);
            if (image != null && !TextUtils.isEmpty(image.getUrl())) {
                ArrayList<Image> imageArrayList = new ArrayList<>();
                imageArrayList.add(new MyTargetAdmobNativeImage(image, context.getResources()));
                setImages(imageArrayList);
            }
            setStarRating(banner.getRating());
            setStore(null);
            setPrice(null);

            Bundle extras = new Bundle();
            final String ageRestrictions = banner.getAgeRestrictions();
            if (!TextUtils.isEmpty(ageRestrictions)) {
                extras.putString(EXTRA_KEY_AGE_RESTRICTIONS, ageRestrictions);
            }
            final String advertisingLabel = banner.getAdvertisingLabel();
            if (!TextUtils.isEmpty(advertisingLabel)) {
                extras.putString(EXTRA_KEY_ADVERTISING_LABEL, advertisingLabel);
            }
            final String category = banner.getCategory();
            if (!TextUtils.isEmpty(category)) {
                extras.putString(EXTRA_KEY_CATEGORY, category);
            }
            final String subCategory = banner.getSubCategory();
            if (!TextUtils.isEmpty(subCategory)) {
                extras.putString(EXTRA_KEY_SUBCATEGORY, subCategory);
            }
            final int votes = banner.getVotes();
            if (votes > 0) {
                extras.putInt(EXTRA_KEY_VOTES, votes);
            }
            setExtras(extras);
        }

        @Override
        public void trackViews(final View containerView, final Map<String, View> clickables,
                Map<String, View> nonclickables) {
            final ArrayList<View> clickableViews = new ArrayList<>(clickables.values());
            containerView.post(new Runnable() {
                @Override
                public void run() {
                    int mediaPosition = findMediaAdViewPosition(clickableViews, mediaAdView);
                    if (mediaPosition >=0 ) {
                        clickableViews.remove(mediaPosition);
                        clickableViews.add(mediaAdView);
                    }
                    nativeAd.registerView(containerView, clickableViews);
                }
            });
        }

        @Override
        public void trackView(final View view) {

            view.post(new Runnable() {
                @Override
                public void run() {
                    if (view instanceof NativeAppInstallAdView) {
                        NativeAppInstallAdView appInstallAdView = (NativeAppInstallAdView) view;
                        final ArrayList<View> assetViews = new ArrayList<>();

                        if (appInstallAdView.getHeadlineView() != null) {
                            assetViews.add(appInstallAdView.getHeadlineView());
                        }

                        if (appInstallAdView.getBodyView() != null) {
                            assetViews.add(appInstallAdView.getBodyView());
                        }

                        if (appInstallAdView.getCallToActionView() != null) {
                            assetViews.add(appInstallAdView.getCallToActionView());
                        }

                        if (appInstallAdView.getIconView() != null) {
                            assetViews.add(appInstallAdView.getIconView());
                        }

                        if (appInstallAdView.getImageView() != null) {
                            assetViews.add(appInstallAdView.getImageView());
                        }

                        if (appInstallAdView.getPriceView() != null) {
                            assetViews.add(appInstallAdView.getPriceView());
                        }

                        if (appInstallAdView.getStarRatingView() != null) {
                            assetViews.add(appInstallAdView.getStarRatingView());
                        }

                        if (appInstallAdView.getStoreView() != null) {
                            assetViews.add(appInstallAdView.getStoreView());
                        }

                        if (appInstallAdView.getMediaView() != null) {
                            assetViews.add(mediaAdView);
                        }

                        nativeAd.registerView(view, assetViews);
                    } else {
                        Log.w(TAG, "Failed to register view for interaction.");
                    }
                }
            });
        }

        @Override
        public void untrackView(View view) {
            nativeAd.unregisterView();
        }
    }

    /**
     * A {@link MyTargetAdapter.MyTargetInterstitialListener} used to forward myTarget interstitial
     * events to Google.
     */
    private class MyTargetNativeAdListener implements NativeAd.NativeAdListener {

        @Nullable
        private final NativeMediationAdRequest nativeMediationAdRequest;

        @NonNull
        private final NativeAd nativeAd;

        @NonNull
        private final Context context;

        MyTargetNativeAdListener(final @NonNull NativeAd nativeAd,
                final @Nullable NativeMediationAdRequest nativeMediationAdRequest,
                final @NonNull Context context) {
            this.nativeAd = nativeAd;
            this.nativeMediationAdRequest = nativeMediationAdRequest;
            this.context = context;
        }

        @Override
        public void onLoad(@NonNull NativePromoBanner banner, @NonNull NativeAd nativeAd) {

			if (this.nativeAd != nativeAd) {
                Log.d(TAG, "Failed to load: loaded native ad does not match with requested");
                if (customEventNativeListener != null) {
                    customEventNativeListener.onAdFailedToLoad(
                            MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
                return;
            }

            mapAd(nativeAd, banner);
        }

        @Override
        public void onNoAd(@NonNull final String reason, @NonNull final NativeAd nativeAd) {
            Log.i(TAG, "No ad: MyTarget callback with reason " + reason);
            if (customEventNativeListener != null) {
                customEventNativeListener
                        .onAdFailedToLoad(MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
            }
        }

        @Override
        public void onClick(@NonNull final NativeAd nativeAd) {
            Log.d(TAG, "Ad clicked");
            if (customEventNativeListener != null) {
                customEventNativeListener.onAdClicked(MyTargetNativeAdapter.this);
                customEventNativeListener.onAdOpened(MyTargetNativeAdapter.this);
                customEventNativeListener.onAdLeftApplication(MyTargetNativeAdapter.this);
            }
        }

        @Override
        public void onShow(@NonNull final NativeAd nativeAd) {
            Log.d(TAG, "Ad show");
            if (customEventNativeListener != null) {
                customEventNativeListener.onAdImpression(MyTargetNativeAdapter.this);
            }
        }

        @Override
        public void onVideoPlay(@NonNull NativeAd nativeAd) {
            Log.d(TAG, "Play ad video");
        }

        @Override
        public void onVideoPause(@NonNull NativeAd nativeAd) {
            Log.d(TAG, "Pause ad video");
        }

        @Override
        public void onVideoComplete(@NonNull NativeAd nativeAd) {
            Log.d(TAG, "Complete ad video");
            if (customEventNativeListener != null) {
                customEventNativeListener.onVideoEnd(MyTargetNativeAdapter.this);
            }
        }

        private void mapAd(final @NonNull NativeAd nativeAd,
                final @NonNull NativePromoBanner banner) {
            if (nativeMediationAdRequest == null) {
                Log.d(TAG, "Failed to load: resources or nativeMediationAdRequest null");
                if (customEventNativeListener != null) {
                    customEventNativeListener.onAdFailedToLoad(
                            MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
                return;
            }

            if (nativeMediationAdRequest.isUnifiedNativeAdRequested()) {
                MyTargetNativeUnifiedAdMapper unifiedMapper = new MyTargetNativeUnifiedAdMapper(
                        nativeAd, context);
                if (banner.getImage() == null || banner.getIcon() == null) {
                    Log.d(TAG,
                            "No ad: Some of the Always Included assets are not available for the "
                                    + "ad");

                    if (customEventNativeListener != null) {
                        customEventNativeListener.onAdFailedToLoad(
                                MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    }
                    return;
                }
                Log.d(TAG, "Ad loaded successfully");
                if (customEventNativeListener != null) {
                    customEventNativeListener.onAdLoaded(MyTargetNativeAdapter.this,
                            unifiedMapper);
                }
                return;
            }

            String navigationType = banner.getNavigationType();

            NativeAdMapper nativeAdMapper;
            if (NavigationType.STORE.equals(navigationType)) {

                if (nativeMediationAdRequest.isAppInstallAdRequested()) {
                    nativeAdMapper = new MyTargetNativeInstallAdMapper(nativeAd, context);
                } else {
                    Log.d(TAG, "No ad: AdMob request was without install ad flag, " +
                            "but MyTarget responded with " + navigationType + " navigation type");
                    if (customEventNativeListener != null) {
                        customEventNativeListener.onAdFailedToLoad(
                                MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    }
                    return;
                }

                if (banner.getImage() == null || banner.getIcon() == null) {
                    Log.d(TAG,
                            "No ad: Some of the Always Included assets are not available for the "
                                    + "ad");

                    if (customEventNativeListener != null) {
                        customEventNativeListener.onAdFailedToLoad(
                                MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    }
                    return;
                }
            } else {
                if (nativeMediationAdRequest.isContentAdRequested()) {
                    nativeAdMapper = new MyTargetNativeContentAdMapper(nativeAd, context);
            } else {
                    Log.d(TAG, "No ad: AdMob request was without content ad flag, " +
                            "but MyTarget responded with " + navigationType + " navigation type");
                    if (customEventNativeListener != null) {
                        customEventNativeListener.onAdFailedToLoad(
                                MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    }
                    return;
                }
                if (banner.getImage() == null) {
                    Log.d(TAG,
                            "No ad: Some of the Always Included assets are not available for the "
                                    + "ad");
                    if (customEventNativeListener != null) {
                        customEventNativeListener.onAdFailedToLoad(
                                MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    }
                    return;
                }
            }
            Log.d(TAG, "Ad loaded successfully");
            if (customEventNativeListener != null) {
                customEventNativeListener.onAdLoaded(MyTargetNativeAdapter.this, nativeAdMapper);
            }
        }
    }

}
