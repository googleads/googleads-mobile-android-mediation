package com.google.ads.mediation.inmobi;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeAppInstallAdMapper;
import com.inmobi.ads.InMobiNative;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

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
    void mapAppInstallAd(final Context context) {
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
            URL iconURL = new URL(mInMobiNative.getAdIconUrl());
            iconUri = Uri.parse(iconURL.toURI().toString());
            iconScale = 1.0;
            if (!this.mIsOnlyURL)
                map.put(ImageDownloaderAsyncTask.KEY_ICON, iconURL);
            else {
                setIcon(new InMobiNativeMappedImage(null, iconUri, iconScale));
            }

        } catch (MandatoryParamException | MalformedURLException |
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

        //Add primary view as media view
        final RelativeLayout rl = new RelativeLayout(context);
        rl.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT) );
        rl.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                View pv = mInMobiNative.getPrimaryViewOfWidth( null, rl, rl.getWidth());
                rl.addView(pv);
                rl.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        setMediaView( rl );
        setHasVideoContent(true);
        setOverrideClickHandling(false);
        mMediationNativeListener.onAdLoaded(mInMobiAdapter, InMobiAppInstallNativeAdMapper.this);

    }

    @Override
    public void recordImpression() {
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
        mInMobiNative.destroy();
    }

}
