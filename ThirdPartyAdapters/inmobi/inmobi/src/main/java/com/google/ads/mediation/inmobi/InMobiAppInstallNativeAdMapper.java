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
 * Created by vineet.srivastava on 5/2/16.
 */
class InMobiAppInstallNativeAdMapper extends NativeAppInstallAdMapper {
    private final InMobiNative mInMobiNative;
    private final Boolean mIsOnlyURL;
    private final MediationNativeListener mMediationNativeListener;
    private final InMobiAdapter mInMobiAdapter;
    private final HashMap<String, String> mLandingUrlMap = new HashMap<>();
    private String[] mImpressionTrackers;

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
        JSONObject payLoad;
        HashMap<String, URL> map;
        final Uri imageUri;
        final Double imageScale;
        final Uri iconUri;
        final Double iconScale;

        try {
            if(mInMobiNative.getCustomAdContent() != null) {
                payLoad = mInMobiNative.getCustomAdContent();
            }
            else{
                mMediationNativeListener.onAdFailedToLoad(mInMobiAdapter, AdRequest.ERROR_CODE_NO_FILL);
                return;
            }

            setHeadline(InMobiAdapterUtils.mandatoryChecking(
                    mInMobiNative.getAdTitle(), InMobiNetworkValues.TITLE));
            setBody(InMobiAdapterUtils.mandatoryChecking(
                    mInMobiNative.getAdDescription(), InMobiNetworkValues
                            .DESCRIPTION));
            setCallToAction(InMobiAdapterUtils.mandatoryChecking(
                    mInMobiNative.getAdCtaText(), InMobiNetworkValues.CTA));

            String landingURL = InMobiAdapterUtils.mandatoryChecking(mInMobiNative.getAdLandingPageUrl(),
                    InMobiNetworkValues.LANDING_URL);
            Bundle paramMap = new Bundle();
            paramMap.putString(InMobiNetworkValues.LANDING_URL, landingURL);
            setExtras(paramMap);
            mLandingUrlMap.put(InMobiNetworkValues.LANDING_URL, landingURL);
            map = new HashMap<>();

            //app icon
            JSONObject iconObject = InMobiAdapterUtils.mandatoryChecking(payLoad.getJSONObject
                    (InMobiNetworkValues.ICON), InMobiNetworkValues.ICON);
            URL iconURL = new URL(iconObject.getString(InMobiNetworkValues.URL));
            iconUri = Uri.parse(iconURL.toURI().toString());
            if(iconObject.has( InMobiNetworkValues.ASPECT_RATIO )){
                iconScale = Double.parseDouble( iconObject.getString(InMobiNetworkValues.ASPECT_RATIO) );
            }
            else {
                Double width = Double.parseDouble(iconObject.getString(InMobiNetworkValues.WIDTH));
                Double height = Double.parseDouble(iconObject.getString(InMobiNetworkValues.HEIGHT));
                if( width > 0  && height > 0 ){
                    iconScale = width / height;
                }
                else {
                    iconScale = 0.0;
                }
            }
            if (!this.mIsOnlyURL)
                map.put(ImageDownloaderAsyncTask.KEY_ICON, iconURL);
            else {
                setIcon(new InMobiNativeMappedImage(null, iconUri, iconScale));
            }

            //screenshots

            JSONObject imageObject = InMobiAdapterUtils.mandatoryChecking(payLoad.getJSONObject
                    (InMobiNetworkValues.SCREENSHOTS), InMobiNetworkValues.SCREENSHOTS);
            URL imageURL = new URL(imageObject.getString(InMobiNetworkValues.URL));
            imageUri = Uri.parse(imageURL.toURI().toString());
            if(imageObject.has( InMobiNetworkValues.ASPECT_RATIO )){
                imageScale = Double.parseDouble(  imageObject.getString(InMobiNetworkValues.ASPECT_RATIO) );
            }
            else {
                Double width = Double.parseDouble(imageObject.getString(InMobiNetworkValues.WIDTH));
                Double height = Double.parseDouble(imageObject.getString(InMobiNetworkValues.HEIGHT));
                if( width > 0  && height > 0 ){
                    imageScale = width / height;
                }
                else {
                    imageScale = 0.0;
                }
            }
            if (!this.mIsOnlyURL)
                map.put(ImageDownloaderAsyncTask.KEY_IMAGE, imageURL);
            else {
                List<NativeAd.Image> imagesList = new ArrayList<>();
                imagesList.add(new InMobiNativeMappedImage(null, imageUri, imageScale));
                setImages(imagesList);
            }
            int length = payLoad.getString(InMobiNetworkValues.IMPRESSION_TRACKERS).length();
            mImpressionTrackers = payLoad.getString(InMobiNetworkValues.IMPRESSION_TRACKERS).substring(2,length-2).split("\",\"");

        } catch (MandatoryParamException | JSONException | MalformedURLException |
                URISyntaxException e) {
            e.printStackTrace();
            mMediationNativeListener.onAdFailedToLoad(mInMobiAdapter, AdRequest.ERROR_CODE_NO_FILL);
            return;
        }

        try {
            if(payLoad.has(InMobiNetworkValues.RATING)){
                setStarRating(Double.parseDouble(payLoad.getString(InMobiNetworkValues.RATING)));
            }
            if (payLoad.has(InMobiNetworkValues.PACKAGE_NAME)) {
                setStore("Google Play");
            } else {
                setStore("Others");
            }
            if(payLoad.has(InMobiNetworkValues.PRICE)) {
                setPrice(payLoad.getString(InMobiNetworkValues.PRICE));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }


        //Download Drawables
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
                    mMediationNativeListener.onAdFailedToLoad(mInMobiAdapter, AdRequest
                            .ERROR_CODE_NO_FILL);
                }
            }).execute(map);
        } else {
            mMediationNativeListener.onAdLoaded(mInMobiAdapter, InMobiAppInstallNativeAdMapper.this);
        }

        setOverrideClickHandling(false);
        setOverrideImpressionRecording(false);
    }

    @Override
    public void recordImpression() {
        new ImpressionBeaconAsyncTask().execute(this.mImpressionTrackers);
    }

    @Override
    public void handleClick(View view) {
        //Handle click
        mInMobiNative.reportAdClickAndOpenLandingPage();
    }

    @Override
    public void trackView(View view) {
    }

    @Override
    public void untrackView(View view) {
    }

}
