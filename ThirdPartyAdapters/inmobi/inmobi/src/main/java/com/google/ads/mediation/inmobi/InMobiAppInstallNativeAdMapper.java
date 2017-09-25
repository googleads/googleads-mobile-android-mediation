package com.google.ads.mediation.inmobi;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

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
 * A {@link NativeAppInstallAdMapper} used to map map InMobi native ads to Google's
 * {@link com.google.android.gms.ads.formats.NativeAppInstallAd}.
 */
class InMobiAppInstallNativeAdMapper extends NativeAppInstallAdMapper {
    private final InMobiNative mInMobiNative;
    private final Boolean mIsOnlyURL;
    private final MediationNativeListener mMediationNativeListener;
    private final InMobiAdapter mInMobiAdapter;
    private final HashMap<String, String> mLandingUrlMap = new HashMap<>();

    InMobiAppInstallNativeAdMapper(InMobiAdapter inMobiAdapter,
                                   InMobiNative inMobiNative,
                                   Boolean isOnlyURL,
                                   MediationNativeListener mediationNativeListener) {
        this.mInMobiAdapter = inMobiAdapter;
        this.mInMobiNative = inMobiNative;
        this.mIsOnlyURL = isOnlyURL;
        this.mMediationNativeListener = mediationNativeListener;
    }

    //Map InMobi Native Ad to AdMob App Install Ad
    void mapAppInstallAd() {
        Object pubContent = mInMobiNative.getAdContent();

        JSONObject payLoad;
        HashMap<String, URL> map;
        final Uri imageUri;
        final Double imageScale;
        final Uri iconUri;
        final Double iconScale;

        try {
            payLoad = new JSONObject(pubContent.toString());

            setHeadline(InMobiAdapterUtils.mandatoryChecking(
                    payLoad.getString(InMobiNetworkValues.TITLE),
                    InMobiNetworkValues.TITLE));
            setBody(InMobiAdapterUtils.mandatoryChecking(
                    payLoad.getString(InMobiNetworkValues.DESCRIPTION),
                    InMobiNetworkValues.DESCRIPTION));
            setCallToAction(InMobiAdapterUtils.mandatoryChecking(
                    payLoad.getString(InMobiNetworkValues.CTA),
                    InMobiNetworkValues.CTA));

            String landingURL = InMobiAdapterUtils.mandatoryChecking(
                    payLoad.getString(InMobiNetworkValues.LANDING_URL),
                    InMobiNetworkValues.LANDING_URL);
            Bundle paramMap = new Bundle();
            paramMap.putString(InMobiNetworkValues.LANDING_URL, landingURL);
            setExtras(paramMap);
            mLandingUrlMap.put(InMobiNetworkValues.LANDING_URL, landingURL);
            map = new HashMap<>();

            // Get app icon.
            String temp;

            JSONObject iconObject = InMobiAdapterUtils.mandatoryChecking(
                    payLoad.getJSONObject(InMobiNetworkValues.ICON),
                    InMobiNetworkValues.ICON);
            URL iconURL = new URL(iconObject.getString(InMobiNetworkValues.URL));
            iconUri = Uri.parse(iconURL.toURI().toString());
            temp = iconObject.getString(InMobiNetworkValues.ASPECT_RATIO);
            iconScale = Double.parseDouble(temp);
            if (!this.mIsOnlyURL)
                map.put(ImageDownloaderAsyncTask.KEY_ICON, iconURL);
            else {
                setIcon(new InMobiNativeMappedImage(null, iconUri, iconScale));
            }

            // Get screenshots.
            JSONObject imageObject = InMobiAdapterUtils.mandatoryChecking(
                    payLoad.getJSONObject(InMobiNetworkValues.SCREENSHOTS),
                    InMobiNetworkValues.SCREENSHOTS);
            URL imageURL = new URL(imageObject.getString(InMobiNetworkValues.URL));
            imageUri = Uri.parse(imageURL.toURI().toString());
            temp = iconObject.getString(InMobiNetworkValues.ASPECT_RATIO);
            imageScale = Double.parseDouble(temp);
            if (!this.mIsOnlyURL)
                map.put(ImageDownloaderAsyncTask.KEY_IMAGE, imageURL);
            else {
                List<NativeAd.Image> imagesList = new ArrayList<>();
                imagesList.add(new InMobiNativeMappedImage(null, imageUri, imageScale));
                setImages(imagesList);
            }

        } catch (MandatoryParamException
                | JSONException
                | MalformedURLException
                | URISyntaxException exception) {
            exception.printStackTrace();
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


        // Download drawables if needed.
        if (!this.mIsOnlyURL) {
            new ImageDownloaderAsyncTask(new ImageDownloaderAsyncTask.DrawableDownloadListener() {
                @Override
                public void onDownloadSuccess(HashMap<String, Drawable> drawableMap) {
                    Drawable iconDrawable = drawableMap.get(ImageDownloaderAsyncTask.KEY_ICON);
                    setIcon(new InMobiNativeMappedImage(iconDrawable, iconUri,
                            iconScale));

                    Drawable imageDrawable = drawableMap.get(ImageDownloaderAsyncTask.KEY_IMAGE);
                    List<NativeAd.Image> imagesList = new ArrayList<>();
                    imagesList.add(new InMobiNativeMappedImage(imageDrawable, imageUri,
                            imageScale));
                    setImages(imagesList);

                    if ((null != iconDrawable) || (null != imageDrawable)) {
                        mMediationNativeListener.onAdLoaded(mInMobiAdapter,
                                InMobiAppInstallNativeAdMapper.this);
                    } else {
                        mMediationNativeListener.onAdFailedToLoad(mInMobiAdapter,
                                AdRequest.ERROR_CODE_NETWORK_ERROR);
                    }
                }

                @Override
                public void onDownloadFailure() {
                    mMediationNativeListener
                            .onAdFailedToLoad(mInMobiAdapter, AdRequest.ERROR_CODE_NO_FILL);
                }
            }).execute(map);
        } else {
            mMediationNativeListener
                    .onAdLoaded(mInMobiAdapter, InMobiAppInstallNativeAdMapper.this);
        }

        setOverrideClickHandling(false);
        setOverrideImpressionRecording(true);
    }

    @Override
    public void recordImpression() {
        // Do Nothing.
    }

    @Override
    public void handleClick(View view) {
        // Handle click.
        mInMobiNative.reportAdClickAndOpenLandingPage(mLandingUrlMap);
    }

    @Override
    public void trackView(View view) {
        InMobiNative.bind(view, mInMobiNative);
    }

    @Override
    public void untrackView(View view) {
        InMobiNative.unbind(view);
    }
}