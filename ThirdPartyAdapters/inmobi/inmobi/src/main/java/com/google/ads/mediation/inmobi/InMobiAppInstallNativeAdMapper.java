package com.google.ads.mediation.inmobi;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.formats.NativeAd;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;
import com.inmobi.ads.InMobiNative;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A {@link NativeAppInstallAdMapper} used to map an InMobi Native ad to Google Native App install
 * ad.
 */
class InMobiAppInstallNativeAdMapper extends NativeAppInstallAdMapper {
    private final InMobiNative mInMobiNative;
    private final Boolean mIsOnlyURL;
    private final MediationNativeListener mMediationNativeListener;
    private final InMobiAdapter mInMobiAdapter;
    private final HashMap<String, String> mLandingUrlMap = new HashMap<>();
    private static final String LOG_TAG = InMobiAppInstallNativeAdMapper.class.getSimpleName();

    InMobiAppInstallNativeAdMapper(InMobiAdapter inMobiAdapter,
                                   InMobiNative inMobiNative,
                                   Boolean isOnlyURL,
                                   MediationNativeListener mediationNativeListener) {
        this.mInMobiAdapter = inMobiAdapter;
        this.mInMobiNative = inMobiNative;
        this.mIsOnlyURL = isOnlyURL;
        this.mMediationNativeListener = mediationNativeListener;
    }

    // Map InMobi Native Ad to AdMob App Install Ad.
    void mapAppInstallAd(final Context context) {
        JSONObject payLoad;
        HashMap<String, URL> map;
        final Uri iconUri;
        final Double iconScale;

        try {
            if (mInMobiNative.getCustomAdContent() != null) {
                payLoad = mInMobiNative.getCustomAdContent();
            } else {
                mMediationNativeListener
                        .onAdFailedToLoad(mInMobiAdapter, AdRequest.ERROR_CODE_NO_FILL);
                return;
            }

            setHeadline(InMobiAdapterUtils.mandatoryChecking(
                    mInMobiNative.getAdTitle(), InMobiNetworkValues.TITLE));
            setBody(InMobiAdapterUtils.mandatoryChecking(
                    mInMobiNative.getAdDescription(), InMobiNetworkValues
                            .DESCRIPTION));
            setCallToAction(InMobiAdapterUtils.mandatoryChecking(
                    mInMobiNative.getAdCtaText(), InMobiNetworkValues.CTA));

            String landingURL = InMobiAdapterUtils.mandatoryChecking(
                    mInMobiNative.getAdLandingPageUrl(), InMobiNetworkValues.LANDING_URL);
            Bundle paramMap = new Bundle();
            paramMap.putString(InMobiNetworkValues.LANDING_URL, landingURL);
            setExtras(paramMap);
            mLandingUrlMap.put(InMobiNetworkValues.LANDING_URL, landingURL);
            map = new HashMap<>();

            // App icon.
            URL iconURL = new URL(mInMobiNative.getAdIconUrl());
            iconUri = Uri.parse(iconURL.toURI().toString());
            iconScale = 1.0;
            if (!this.mIsOnlyURL)
                map.put(ImageDownloaderAsyncTask.KEY_ICON, iconURL);
            else {
                setIcon(new InMobiNativeMappedImage(null, iconUri, iconScale));
                List<NativeAd.Image> imagesList = new ArrayList<>();
                imagesList.add(new InMobiNativeMappedImage(
                        new ColorDrawable(Color.TRANSPARENT), null, 1.0));
                setImages(imagesList);
            }

        } catch (MandatoryParamException | MalformedURLException |
                URISyntaxException e) {
            e.printStackTrace();
            mMediationNativeListener.onAdFailedToLoad(mInMobiAdapter, AdRequest.ERROR_CODE_NO_FILL);
            return;
        }

        try {
            if (payLoad.has(InMobiNetworkValues.RATING)) {
                setStarRating(Double.parseDouble(payLoad.getString(InMobiNetworkValues.RATING)));
            }
            if (payLoad.has(InMobiNetworkValues.PACKAGE_NAME)) {
                setStore("Google Play");
            } else {
                setStore("Others");
            }
            if (payLoad.has(InMobiNetworkValues.PRICE)) {
                setPrice(payLoad.getString(InMobiNetworkValues.PRICE));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //Add primary view as media view
        final RelativeLayout placeHolderView = new RelativeLayout(context);
        placeHolderView.setLayoutParams( new RelativeLayout.LayoutParams( RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT) );
        final ViewTreeObserver viewTreeObserver = placeHolderView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        placeHolderView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        placeHolderView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                    final View parent = (View)placeHolderView.getParent();
                    int width = parent.getWidth();
                    Log.d(LOG_TAG, "parent layout width is " + width);
                    final View primaryView = mInMobiNative.getPrimaryViewOfWidth(context, null, placeHolderView, width);
                    if(primaryView != null){
                        placeHolderView.addView(primaryView);
                    }
                }
            });
        }

        setMediaView( placeHolderView );
        boolean hasVideo = (mInMobiNative.isVideo() == null) ? false : mInMobiNative.isVideo();
        setHasVideoContent(hasVideo);
        setOverrideClickHandling(false);

        // Download drawables.
        if (!this.mIsOnlyURL) {
            new ImageDownloaderAsyncTask(new ImageDownloaderAsyncTask.DrawableDownloadListener() {
                @Override
                public void onDownloadSuccess(HashMap<String, Drawable> drawableMap) {
                    Drawable iconDrawable = drawableMap.get(ImageDownloaderAsyncTask.KEY_ICON);
                    setIcon(new InMobiNativeMappedImage(iconDrawable, iconUri,
                            iconScale));

                    List<NativeAd.Image> imagesList = new ArrayList<>();
                    imagesList.add(new InMobiNativeMappedImage(
                            new ColorDrawable(Color.TRANSPARENT), null, 1.0));
                    setImages(imagesList);

                    if ((null != iconDrawable)) {
                        mMediationNativeListener.onAdLoaded(mInMobiAdapter,
                                InMobiAppInstallNativeAdMapper.this);
                    } else {
                        mMediationNativeListener.onAdFailedToLoad(mInMobiAdapter,
                                AdRequest.ERROR_CODE_NETWORK_ERROR);
                    }
                }

                @Override
                public void onDownloadFailure() {
                    mMediationNativeListener.onAdFailedToLoad(mInMobiAdapter, AdRequest
                            .ERROR_CODE_NO_FILL);
                }
            }).execute(map);
        } else {
            mMediationNativeListener
                    .onAdLoaded(mInMobiAdapter, InMobiAppInstallNativeAdMapper.this);
        }
    }

    @Override
    public void recordImpression() {
    }

    @Override
    public void handleClick(View view) {
        // Handle click.
        mInMobiNative.reportAdClickAndOpenLandingPage();
    }

    @Override
    public void trackView(View view) {
    }

    @Override
    public void untrackView(View view) {
        mInMobiNative.destroy();
    }

}
