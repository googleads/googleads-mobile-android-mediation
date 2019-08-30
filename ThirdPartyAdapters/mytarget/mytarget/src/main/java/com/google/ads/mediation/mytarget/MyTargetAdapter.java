package com.google.ads.mediation.mytarget;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.my.target.ads.InterstitialAd;
import com.my.target.ads.MyTargetView;
import com.my.target.ads.MyTargetView.MyTargetViewListener;
import com.my.target.common.CustomParams;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Mediation adapter for myTarget.
 */
public class MyTargetAdapter extends MyTargetMediationAdapter
    implements MediationBannerAdapter, MediationInterstitialAdapter {

    @NonNull
    private static final String TAG = "MyTargetAdapter";

    @Nullable
    private MyTargetView mMyTargetView;

    @Nullable
    private InterstitialAd mInterstitial;

    @Override
    public void requestBannerAd(Context context,
                                MediationBannerListener mediationBannerListener,
                                Bundle serverParameters,
                                AdSize adSize,
                                MediationAdRequest mediationAdRequest,
                                Bundle mediationExtras) {
        int slotId = MyTargetTools.checkAndGetSlotId(context, serverParameters);
        Log.d(TAG, "Requesting myTarget banner mediation, slotId: " + slotId);
        if (slotId < 0) {
            if (mediationBannerListener != null) {
                mediationBannerListener.onAdFailedToLoad(
                        MyTargetAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }
        adSize = getSupportedAdSize(context, adSize);

        if (adSize == null) {
            Log.w(TAG, "Failed to request ad, AdSize is null.");
            if (mediationBannerListener != null) {
                mediationBannerListener.onAdFailedToLoad(
                        MyTargetAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        MyTargetBannerListener bannerListener = null;
        if (mediationBannerListener != null) {
            bannerListener = new MyTargetBannerListener(mediationBannerListener);
        }

        if (adSize.getWidth() == 300 && adSize.getHeight() == 250) {
            Log.d(TAG, "Loading myTarget banner, size: 300x250");
            loadBanner(bannerListener,
                    mediationAdRequest,
                    slotId,
                    MyTargetView.AdSize.BANNER_300x250,
                    context);
        } else if (adSize.getWidth() == 728 && adSize.getHeight() == 90) {
            Log.d(TAG, "Loading myTarget banner, size: 728x90");
            loadBanner(bannerListener,
                    mediationAdRequest,
                    slotId,
                    MyTargetView.AdSize.BANNER_728x90,
                    context);
        } else if (adSize.getWidth() == 320 && adSize.getHeight() == 50) {
            Log.d(TAG, "Loading myTarget banner, size: 320x50");
            loadBanner(bannerListener,
                    mediationAdRequest,
                    slotId,
                    MyTargetView.AdSize.BANNER_320x50,
                    context);
        } else {
            Log.w(TAG, "AdSize " + adSize.toString() + " is not currently supported");
            if (mediationBannerListener != null) {
                mediationBannerListener
                        .onAdFailedToLoad(MyTargetAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
            }
        }

    }

    AdSize getSupportedAdSize(Context context, AdSize adSize) {
        AdSize original = new AdSize(adSize.getWidth(), adSize.getHeight());

        /*
            Supported Sizes:
            MyTargetView.AdSize.BANNER_300x250;
            MyTargetView.AdSize.BANNER_320x50;
            MyTargetView.AdSize.BANNER_728x90;
        */

        ArrayList<AdSize> potentials = new ArrayList<AdSize>(3);
        potentials.add(AdSize.BANNER);
        potentials.add(AdSize.MEDIUM_RECTANGLE);
        potentials.add(AdSize.LEADERBOARD);

        Log.i(TAG, "Potential ad sizes: " + potentials.toString());
        return MyTargetTools.findClosestSize(context, original, potentials);
    }

    @Override
    public View getBannerView() {
        return mMyTargetView;
    }

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener mediationInterstitialListener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle mediationExtras) {
        int slotId = MyTargetTools.checkAndGetSlotId(context, serverParameters);
        Log.d(TAG, "Requesting myTarget interstitial mediation, slotId: " + slotId);

        if (slotId < 0) {
            if (mediationInterstitialListener != null) {
                mediationInterstitialListener.onAdFailedToLoad(
                        MyTargetAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
            return;
        }

        MyTargetInterstitialListener bannerListener = null;
        if (mediationInterstitialListener != null) {
            bannerListener = new MyTargetInterstitialListener(mediationInterstitialListener);
        }

        if (mInterstitial != null) {
            mInterstitial.destroy();
        }

        mInterstitial = new InterstitialAd(slotId, context);
        CustomParams params = mInterstitial.getCustomParams();
        params.setCustomParam(
                MyTargetTools.PARAM_MEDIATION_KEY, MyTargetTools.PARAM_MEDIATION_VALUE);
        if (mediationAdRequest != null) {
            int gender = mediationAdRequest.getGender();
            Log.d(TAG, "Set gender to " + gender);
            params.setGender(gender);
            Date date = mediationAdRequest.getBirthday();
            if (date != null && date.getTime() != -1) {
                GregorianCalendar calendar = new GregorianCalendar();
                GregorianCalendar calendarNow = new GregorianCalendar();

                calendar.setTimeInMillis(date.getTime());
                int age = calendarNow.get(GregorianCalendar.YEAR)
                        - calendar.get(GregorianCalendar.YEAR);
                if (age >= 0) {
                    Log.d(TAG, "Set age to " + age);
                    params.setAge(age);
                }
            }
        }
        mInterstitial.setListener(bannerListener);
        mInterstitial.load();
    }

    @Override
    public void showInterstitial() {
        if (mInterstitial != null) {
            mInterstitial.show();
        }
    }

    @Override
    public void onDestroy() {
        if (mMyTargetView != null) {
            mMyTargetView.destroy();
        }
        if (mInterstitial != null) {
            mInterstitial.destroy();
        }
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onResume() {
    }

    /**
     * Starts loading banner.
     *
     * @param myTargetBannerListener listener for ad callbacks
     * @param mediationAdRequest     Google mediation request
     * @param slotId                 myTarget slot ID
     * @param adSize                 myTarget banner size
     * @param context                app context
     */
    private void loadBanner(@Nullable MyTargetBannerListener myTargetBannerListener,
                            @Nullable MediationAdRequest mediationAdRequest,
                            int slotId,
                            int adSize,
                            @NonNull Context context) {
        if (mMyTargetView != null) {
            mMyTargetView.destroy();
        }

        mMyTargetView = new MyTargetView(context);

        mMyTargetView.init(slotId, adSize, false);

        CustomParams params = mMyTargetView.getCustomParams();
        if (params != null) {
            if (mediationAdRequest != null) {
                int gender = mediationAdRequest.getGender();
                params.setGender(gender);
                Log.d(TAG, "Set gender to " + gender);

                Date date = mediationAdRequest.getBirthday();
                if (date != null && date.getTime() != -1) {
                    GregorianCalendar calendar = new GregorianCalendar();
                    GregorianCalendar calendarNow = new GregorianCalendar();

                    calendar.setTimeInMillis(date.getTime());
                    int age = calendarNow.get(GregorianCalendar.YEAR) -
                            calendar.get(GregorianCalendar.YEAR);
                    if (age >= 0) {
                        Log.d(TAG, "Set age to " + age);
                        params.setAge(age);
                    }
                }
            }

            params.setCustomParam(
                    MyTargetTools.PARAM_MEDIATION_KEY, MyTargetTools.PARAM_MEDIATION_VALUE);
        }

        mMyTargetView.setListener(myTargetBannerListener);
        mMyTargetView.load();
    }

    /**
     * A {@link MyTargetBannerListener} used to forward myTarget banner events to Google.
     */
    private class MyTargetBannerListener implements MyTargetViewListener {

        @NonNull
        private final MediationBannerListener listener;

        MyTargetBannerListener(final @NonNull MediationBannerListener listener) {
            this.listener = listener;
        }

        @Override
        public void onLoad(@NonNull final MyTargetView view) {
            Log.d(TAG, "Banner mediation Ad loaded");
            listener.onAdLoaded(MyTargetAdapter.this);
        }

        @Override
        public void onNoAd(@NonNull final String reason, @NonNull final MyTargetView view) {
             Log.i(TAG, "Banner mediation Ad failed to load: " + reason);
             listener.onAdFailedToLoad(MyTargetAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
        }

        @Override
        public void onShow(@NonNull MyTargetView view)
        {
            Log.d(TAG, "Banner mediation Ad show");
        }

        @Override
        public void onClick(@NonNull final MyTargetView view) {
            Log.d(TAG, "Banner mediation Ad clicked");
            listener.onAdClicked(MyTargetAdapter.this);
            listener.onAdOpened(MyTargetAdapter.this);
            // click redirects user to Google Play, or web browser, so we can notify
            // about left application.
            listener.onAdLeftApplication(MyTargetAdapter.this);
        }
    }

    /**
     * A {@link MyTargetInterstitialListener} used to forward myTarget interstitial events to
     * Google.
     */
    private class MyTargetInterstitialListener implements InterstitialAd.InterstitialAdListener {

        @NonNull
        private final MediationInterstitialListener listener;

        MyTargetInterstitialListener(final @NonNull MediationInterstitialListener listener) {
            this.listener = listener;
        }

        @Override
        public void onLoad(@NonNull final InterstitialAd ad) {
            Log.d(TAG, "Interstitial mediation Ad loaded");
            listener.onAdLoaded(MyTargetAdapter.this);
        }

        @Override
        public void onNoAd(@NonNull final String reason, @NonNull final InterstitialAd ad) {
                Log.i(TAG, "Interstitial mediation Ad failed to load: " + reason);
                listener.onAdFailedToLoad(MyTargetAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
        }

        @Override
        public void onClick(@NonNull final InterstitialAd ad) {
            Log.d(TAG, "Interstitial mediation Ad clicked");
            listener.onAdClicked(MyTargetAdapter.this);
            // click redirects user to Google Play, or web browser, so we can notify
            // about left application.
            listener.onAdLeftApplication(MyTargetAdapter.this);
        }

        @Override
        public void onDismiss(@NonNull final InterstitialAd ad) {
            Log.d(TAG, "Interstitial mediation Ad dismissed");
            listener.onAdClosed(MyTargetAdapter.this);
        }

        @Override
        public void onVideoCompleted(@NonNull final InterstitialAd ad) {
        }

        @Override
        public void onDisplay(@NonNull final InterstitialAd ad) {
            Log.d(TAG, "Interstitial mediation Ad displayed");
            listener.onAdOpened(MyTargetAdapter.this);
        }
    }
}
