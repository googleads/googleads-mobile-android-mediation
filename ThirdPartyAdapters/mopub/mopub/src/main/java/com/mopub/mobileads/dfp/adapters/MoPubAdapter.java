package com.mopub.mobileads.dfp.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.formats.NativeAdOptions;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubView;
import com.mopub.nativeads.BaseNativeAd;
import com.mopub.nativeads.MoPubNative;
import com.mopub.nativeads.MoPubStaticNativeAdRenderer;
import com.mopub.nativeads.NativeAd;
import com.mopub.nativeads.NativeErrorCode;
import com.mopub.nativeads.RequestParameters;
import com.mopub.nativeads.StaticNativeAd;
import com.mopub.nativeads.ViewBinder;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;
import java.util.HashMap;

public class MoPubAdapter implements MediationNativeAdapter, MediationBannerAdapter, MediationInterstitialAdapter {


    private MoPubView mMoPubView;
    private MoPubInterstitial mMoPubInterstitial;
    private static final String MOPUB_NATIVE_CEVENT_VERSION = "tp:google_mediating_mopub";
    public static final double DEFAULT_MOPUB_IMAGE_SCALE = 1;
    private static final String MOPUB_AD_UNIT_KEY = "adUnitId";
    private int privacyIconPlacement;


    @Override
    public void onDestroy() {

        if(mMoPubInterstitial!=null){
            mMoPubInterstitial.destroy();
            mMoPubInterstitial=null;
        }

        if (mMoPubView!=null) {
            mMoPubView.destroy();
            mMoPubView=null;
        }
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void requestNativeAd(Context context,
                                final MediationNativeListener listener,
                                Bundle serverParameters,
                                NativeMediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {

        String adunit = serverParameters.getString(MOPUB_AD_UNIT_KEY);
        final NativeAdOptions options = mediationAdRequest.getNativeAdOptions();

        if(options!=null)
            privacyIconPlacement = options.getAdChoicesPlacement();
        else
            privacyIconPlacement = NativeAdOptions.ADCHOICES_TOP_RIGHT;

        if(!mediationAdRequest.isAppInstallAdRequested() && mediationAdRequest
                .isContentAdRequested()) {
            Log.d("MoPub", "MoPub will be able to serve only install ads in the mediation " +
                    "currently.");
            listener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        MoPubNative.MoPubNativeNetworkListener moPubNativeNetworkListener = new MoPubNative.MoPubNativeNetworkListener() {

            @Override
            public void onNativeLoad(NativeAd nativeAd) {
                BaseNativeAd adData = nativeAd.getBaseNativeAd();
                if (adData instanceof StaticNativeAd) {
                    final StaticNativeAd staticNativeAd = (StaticNativeAd) adData;

                    if(options!=null && options.shouldReturnUrlsForImageAssets()){
                        final MoPubNativeAppInstallAdMapper moPubNativeAppInstallAdMapper = new
                                MoPubNativeAppInstallAdMapper(staticNativeAd, null, privacyIconPlacement);
                        listener.onAdLoaded(MoPubAdapter.this, moPubNativeAppInstallAdMapper);
                        return;
                    }

                    HashMap <String, URL>  map  =   new HashMap <String, URL>();
                    try {
                        map.put(DownloadDrawablesAsync.KEY_ICON, new URL(staticNativeAd.getIconImageUrl()));
                        map.put(DownloadDrawablesAsync.KEY_IMAGE, new URL(staticNativeAd.getMainImageUrl()));

                    } catch (MalformedURLException e) {
                        //return added with fail callbacks - rupa
                        Log.d("MoPub", "Invalid ad response received from MoPub. Image URLs are invalid");
                        listener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        return;
                    }

                    new DownloadDrawablesAsync(new DrawableDownloadListener() {
                        @Override
                        public void onDownloadSuccess(HashMap<String, Drawable> drawableMap) {

                            final MoPubNativeAppInstallAdMapper moPubNativeAppInstallAdMapper =
                                    new MoPubNativeAppInstallAdMapper(staticNativeAd,
                                            drawableMap, privacyIconPlacement);
                            listener.onAdLoaded(MoPubAdapter.this, moPubNativeAppInstallAdMapper);
                        }

                        @Override
                        public void onDownloadFailure() {
                            // Failed to download images, send failure callback.
                            listener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        }
                    }).execute(map);
                }
            }

            @Override
            public void onNativeFail(NativeErrorCode errorCode) {
                switch (errorCode){
                    case EMPTY_AD_RESPONSE:
                        listener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                        break;
                    case INVALID_REQUEST_URL:
                        listener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                        break;
                    case CONNECTION_ERROR:
                        listener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                        break;
                    case UNSPECIFIED:
                        listener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        break;
                    default:
                        listener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        break;
                }

            }
        };

        if(adunit==null) {
            Log.d("MoPub", "Adunit id is invalid. So failing the request.");
            listener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        MoPubNative moPubNative = new MoPubNative(context, adunit, moPubNativeNetworkListener);

        ViewBinder viewbinder = new ViewBinder.Builder(0).build();
        MoPubStaticNativeAdRenderer moPubStaticNativeAdRenderer = new MoPubStaticNativeAdRenderer(viewbinder);
        moPubNative.registerAdRenderer(moPubStaticNativeAdRenderer);
        EnumSet<RequestParameters.NativeAdAsset> assetsSet = EnumSet.of(RequestParameters.NativeAdAsset.TITLE, RequestParameters.NativeAdAsset.TEXT,
                RequestParameters.NativeAdAsset.CALL_TO_ACTION_TEXT, RequestParameters.NativeAdAsset.MAIN_IMAGE,
                RequestParameters.NativeAdAsset.ICON_IMAGE);

        RequestParameters requestParameters = new RequestParameters.Builder()
                .keywords(MOPUB_NATIVE_CEVENT_VERSION + "gender:" + mediationAdRequest
                .getGender() + ",age:" + mediationAdRequest.getBirthday())
                .location(mediationAdRequest.getLocation())
                .desiredAssets(assetsSet)
                .build();

        moPubNative.makeRequest(requestParameters);

    }

    @Override
    public void requestBannerAd(Context context, MediationBannerListener mediationBannerListener, Bundle bundle, AdSize adSize, MediationAdRequest mediationAdRequest, Bundle bundle1) {

        String adunit = bundle.getString(MOPUB_AD_UNIT_KEY);

        mMoPubView = new MoPubView(context);
        mMoPubView.setBannerAdListener(new MBannerListener(mediationBannerListener));
        mMoPubView.setAdUnitId(adunit);

        //If test mode is enabled
        if (mediationAdRequest.isTesting()){
            mMoPubView.setTesting(true);
        }

        //If location is available
        if (mediationAdRequest.getLocation()!=null){
            mMoPubView.setLocation(mediationAdRequest.getLocation());
        }

        mMoPubView.setKeywords(getKeywords(mediationAdRequest));
        mMoPubView.loadAd();
    }

    @Override
    public View getBannerView() {
        return mMoPubView;
    }

    private String getKeywords(MediationAdRequest mediationAdRequest){
        StringBuilder keywordsBuilder =  new StringBuilder();

        keywordsBuilder = keywordsBuilder.append(MOPUB_NATIVE_CEVENT_VERSION)
                .append(mediationAdRequest.getBirthday() != null ?  ",m_birthday:"+mediationAdRequest.getBirthday() : "")
                .append(mediationAdRequest.getGender() != -1 ?  ",m_gender:"+mediationAdRequest
                        .getGender() : "");

        return keywordsBuilder.toString();

    }

    private class MBannerListener implements MoPubView.BannerAdListener {
        private MediationBannerListener mMediationBannerListener;

        public MBannerListener(MediationBannerListener bannerListener){
            mMediationBannerListener = bannerListener;
        }

        @Override
        public void onBannerClicked(MoPubView moPubView) {
            mMediationBannerListener.onAdClicked(MoPubAdapter.this);
        }

        @Override
        public void onBannerCollapsed(MoPubView moPubView) {
            mMediationBannerListener.onAdClosed(MoPubAdapter.this);

        }

        @Override
        public void onBannerExpanded(MoPubView moPubView) {
            mMediationBannerListener.onAdOpened(MoPubAdapter.this);

        }

        @Override
        public void onBannerFailed(MoPubView moPubView,
                                   MoPubErrorCode moPubErrorCode) {
            switch (moPubErrorCode) {
                case NO_FILL:
                    mMediationBannerListener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_NO_FILL );
                    break;
                case NETWORK_TIMEOUT:
                    mMediationBannerListener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_NETWORK_ERROR);
                    break;
                case SERVER_ERROR:
                    mMediationBannerListener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                    break;
                default:
                    mMediationBannerListener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    break;
            }
        }

        @Override
        public void onBannerLoaded(MoPubView moPubView) {
            mMediationBannerListener.onAdLoaded(MoPubAdapter.this);

        }

    }


    @Override
    public void requestInterstitialAd(Context context, MediationInterstitialListener mediationInterstitialListener, Bundle bundle, MediationAdRequest mediationAdRequest, Bundle bundle1) {

        String adunit = bundle.getString(MOPUB_AD_UNIT_KEY);

        mMoPubInterstitial = new MoPubInterstitial((Activity) context, adunit);
        mMoPubInterstitial
                .setInterstitialAdListener(new mMediationInterstitialListener(mediationInterstitialListener));

        //If test mode is enabled
        if (mediationAdRequest.isTesting()){
            mMoPubInterstitial.setTesting(true);
        }

        mMoPubInterstitial.setKeywords(getKeywords(mediationAdRequest));
        mMoPubInterstitial.load();
    }

    @Override
    public void showInterstitial() {

        if (mMoPubInterstitial.isReady()) {
            mMoPubInterstitial.show();
        } else {
            MoPubLog.i("Interstitial was not ready. Unable to load the interstitial");
        }

    }

    private class mMediationInterstitialListener implements MoPubInterstitial.InterstitialAdListener {
        private MediationInterstitialListener mMediationInterstitialListener;

        public mMediationInterstitialListener(MediationInterstitialListener interstitialListener){
            mMediationInterstitialListener = interstitialListener;
        }

        @Override
        public void onInterstitialClicked(MoPubInterstitial moPubInterstitial) {
            mMediationInterstitialListener.onAdClicked(MoPubAdapter.this);
        }

        @Override
        public void onInterstitialDismissed(MoPubInterstitial moPubInterstitial) {
            mMediationInterstitialListener.onAdClosed(MoPubAdapter.this);

        }

        @Override
        public void onInterstitialFailed(MoPubInterstitial moPubInterstitial,
                                         MoPubErrorCode moPubErrorCode) {
            switch (moPubErrorCode) {
                case NO_FILL:
                    mMediationInterstitialListener.onAdFailedToLoad(MoPubAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    break;
                case NETWORK_TIMEOUT:
                    mMediationInterstitialListener.onAdFailedToLoad(MoPubAdapter.this, AdRequest
                            .ERROR_CODE_NETWORK_ERROR);
                    break;
                case SERVER_ERROR:
                    mMediationInterstitialListener.onAdFailedToLoad(MoPubAdapter.this, AdRequest
                            .ERROR_CODE_INVALID_REQUEST);
                    break;
                default:
                    mMediationInterstitialListener.onAdFailedToLoad(MoPubAdapter.this, AdRequest
                            .ERROR_CODE_INTERNAL_ERROR);
                    break;
            }

        }

        @Override
        public void onInterstitialLoaded(MoPubInterstitial moPubInterstitial) {
            mMediationInterstitialListener.onAdLoaded(MoPubAdapter.this);
        }

        @Override
        public void onInterstitialShown(MoPubInterstitial moPubInterstitial) {
            mMediationInterstitialListener.onAdOpened(MoPubAdapter.this);
        }

    }

}

