package com.google.ads.mediation.mytarget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAd.Image;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeAdMapper;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;
import com.google.android.gms.ads.mediation.NativeContentAdMapper;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.my.target.ads.CustomParams;
import com.my.target.nativeads.NativeAd;
import com.my.target.nativeads.banners.NativePromoBanner;
import com.my.target.nativeads.banners.NavigationType;
import com.my.target.nativeads.models.ImageData;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

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

    @Override
    public void requestNativeAd(Context context,
                                MediationNativeListener customEventNativeListener,
                                Bundle serverParameter,
                                NativeMediationAdRequest nativeMediationAdRequest,
                                Bundle customEventExtras) {
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
        if (nativeMediationAdRequest != null) {
            options = nativeMediationAdRequest.getNativeAdOptions();
            gender = nativeMediationAdRequest.getGender();
            birthday = nativeMediationAdRequest.getBirthday();
            contentRequested = nativeMediationAdRequest.isContentAdRequested();
            installRequested = nativeMediationAdRequest.isAppInstallAdRequested();
        }

        NativeAd nativeAd = new NativeAd(slotId, context.getApplicationContext());

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
                + ", install requested: " + installRequested);
        if (!contentRequested || !installRequested) {
            if (!contentRequested) {
                params.setCustomParam(PARAM_NATIVE_TYPE_REQUEST, PARAM_INSTALL_ONLY);
            } else {
                params.setCustomParam(PARAM_NATIVE_TYPE_REQUEST, PARAM_CONTENT_ONLY);
            }
        }

        MyTargetNativeAdListener nativeAdListener = null;
        if (customEventNativeListener != null) {
            nativeAdListener = new MyTargetNativeAdListener(
                    nativeAd,
                    customEventNativeListener,
                    nativeMediationAdRequest,
                    context.getResources());
        }

        params.setCustomParam(
                MyTargetTools.PARAM_MEDIATION_KEY, MyTargetTools.PARAM_MEDIATION_VALUE);
        nativeAd.setListener(nativeAdListener);
        nativeAd.load();
    }

    @Override
    public void onDestroy() {
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

        MyTargetNativeContentAdMapper(@NonNull NativeAd nativeAd, @NonNull Resources resources) {
            this.nativeAd = nativeAd;
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
                setLogo(new MyTargetAdmobNativeImage(icon, resources));
            }
            ImageData image = banner.getImage();
            if (image != null && !TextUtils.isEmpty(image.getUrl())) {
                ArrayList<Image> imageArrayList = new ArrayList<>();
                imageArrayList.add(new MyTargetAdmobNativeImage(image, resources));
                setImages(imageArrayList);
            }
            setAdvertiser(banner.getDomain());
            setHasVideoContent(false);

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
        public void trackView(View view) {
            nativeAd.registerView(view);
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

        MyTargetNativeInstallAdMapper(@NonNull NativeAd nativeAd, @NonNull Resources resources) {
            this.nativeAd = nativeAd;
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
                setIcon(new MyTargetAdmobNativeImage(icon, resources));
            }
            ImageData image = banner.getImage();
            if (image != null && !TextUtils.isEmpty(image.getUrl())) {
                ArrayList<Image> imageArrayList = new ArrayList<>();
                imageArrayList.add(new MyTargetAdmobNativeImage(image, resources));
                setImages(imageArrayList);
            }
            setStarRating(banner.getRating());
            setStore(null);
            setPrice(null);
            setHasVideoContent(false);

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
            final String subCategory = banner.getSubcategory();
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
        public void trackView(View view) {
            nativeAd.registerView(view);
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

        @NonNull
        private final MediationNativeListener mediationNativeListener;

        @Nullable
        private final NativeMediationAdRequest nativeMediationAdRequest;

        @Nullable
        private final Resources resources;

        @NonNull
        private final NativeAd nativeAd;

        MyTargetNativeAdListener(final @NonNull NativeAd nativeAd,
                                 final @NonNull MediationNativeListener mediationNativeListener,
                                 final @Nullable NativeMediationAdRequest nativeMediationAdRequest,
                                 final @Nullable Resources resources) {
            this.nativeAd = nativeAd;
            this.mediationNativeListener = mediationNativeListener;
            this.nativeMediationAdRequest = nativeMediationAdRequest;
            this.resources = resources;
        }

        @Override
        public void onLoad(final NativeAd nativeAd) {

            if (nativeAd == null) {
                Log.d(TAG, "No ad: MyTarget responded with null NativeAd");
                mediationNativeListener
                        .onAdFailedToLoad(MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                return;
            }
            NativePromoBanner banner = nativeAd.getBanner();
            if (banner == null) {
                Log.d(TAG, "No ad: MyTarget responded with null banner");
                mediationNativeListener
                        .onAdFailedToLoad(MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                return;
            }

            if (this.nativeAd != nativeAd) {
                Log.d(TAG, "Failed to load: loaded native ad does not match with requested");
                mediationNativeListener.onAdFailedToLoad(
                        MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                return;
            }

            mapAd(nativeAd, banner);
        }

        @Override
        public void onNoAd(final String reason, final NativeAd nativeAd) {
            Log.d(TAG, "No ad: MyTarget callback with reason " + reason);
            mediationNativeListener
                    .onAdFailedToLoad(MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
        }

        @Override
        public void onClick(final NativeAd nativeAd) {
            Log.d(TAG, "Ad clicked");
            mediationNativeListener.onAdClicked(MyTargetNativeAdapter.this);
            mediationNativeListener.onAdOpened(MyTargetNativeAdapter.this);
            mediationNativeListener.onAdLeftApplication(MyTargetNativeAdapter.this);
        }

        @Override
        public void onShow(final NativeAd nativeAd) {
            Log.d(TAG, "Ad show");
            mediationNativeListener.onAdImpression(MyTargetNativeAdapter.this);
        }

        private void mapAd(final @NonNull NativeAd nativeAd,
                           final @NonNull NativePromoBanner banner) {
            if (resources == null || nativeMediationAdRequest == null) {
                Log.d(TAG, "Failed to load: resources or nativeMediationAdRequest null");
                mediationNativeListener.onAdFailedToLoad(
                        MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                return;
            }

            NativeAdMapper nativeAdMapper;
            String navigationType = banner.getNavigationType();

            if (NavigationType.STORE.equals(navigationType)
                    || NavigationType.DEEPLINK.equals(navigationType)) {
                if (!nativeMediationAdRequest.isAppInstallAdRequested()) {
                    Log.d(TAG, "No ad: AdMob request was without install ad flag, " +
                            "but MyTarget responded with " + navigationType + " navigation type");
                    mediationNativeListener.onAdFailedToLoad(
                            MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    return;
                }

                if (banner.getTitle() == null || banner.getDescription() == null
                        || banner.getImage() == null || banner.getIcon() == null
                        || banner.getCtaText() == null) {
                    Log.d(TAG, "No ad: Some of the Always Included assets are not available for the ad");
                    mediationNativeListener.onAdFailedToLoad(
                            MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    return;
                }

                nativeAdMapper = new MyTargetNativeInstallAdMapper(nativeAd, resources);
            } else {
                if (!nativeMediationAdRequest.isContentAdRequested()) {
                    Log.d(TAG, "No ad: AdMob request was without content ad flag, " +
                            "but MyTarget responded with " + navigationType + " navigation type");
                    mediationNativeListener.onAdFailedToLoad(
                            MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    return;
                }
                if (banner.getTitle() == null || banner.getDescription() == null ||
                        banner.getImage() == null || banner.getCtaText() == null ||
                        banner.getDomain() == null) {
                    Log.d(TAG, "No ad: Some of the Always Included assets are not available for the ad");
                    mediationNativeListener.onAdFailedToLoad(
                            MyTargetNativeAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    return;
                }
                nativeAdMapper = new MyTargetNativeContentAdMapper(nativeAd, resources);
            }
            Log.d(TAG, "Ad loaded successfully");
            mediationNativeListener.onAdLoaded(MyTargetNativeAdapter.this, nativeAdMapper);
        }
    }
}
