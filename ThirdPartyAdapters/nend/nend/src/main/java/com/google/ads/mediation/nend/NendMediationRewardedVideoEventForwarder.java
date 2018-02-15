package com.google.ads.mediation.nend;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import net.nend.android.NendAdRewardItem;
import net.nend.android.NendAdRewardedListener;
import net.nend.android.NendAdRewardedVideo;
import net.nend.android.NendAdUserFeature;
import net.nend.android.NendAdVideo;

import java.lang.ref.WeakReference;

/*
 * The {@link NendMediationRewardedVideoEventForwarder} is used to forward reward-based video ad
 * events from Nend SDK to Google Mobile Ads SDK.
 */
class NendMediationRewardedVideoEventForwarder implements NendAdRewardedListener {

    private static final String TAG = NendAdapter.class.getSimpleName();
    private NendAdRewardedVideo mNendAdRewardedVideo;
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;
    private NendRewardedAdapter mNendAdapter;
    private boolean mIsInitialized;
    /*
     * An Android {@link Activity} weak reference used to show ads.
     */
    private WeakReference<Activity> mActivityWeakReference;

    NendMediationRewardedVideoEventForwarder(Activity activity,
                                             NendRewardedAdapter nendAdapter,
                                             Bundle serverParameters,
                                             MediationRewardedVideoAdListener listener) {
        this.mMediationRewardedVideoAdListener = listener;
        this.mNendAdapter = nendAdapter;
        this.mActivityWeakReference = new WeakReference<>(activity);

        String apiKey = serverParameters.getString("apiKey");
        int spotId = Integer.parseInt(serverParameters.getString("spotId"));

        if (!isValidSpotIdAndKey(spotId, apiKey)) {
            Log.e(NendRewardedAdapter.TAG, "Failed to initialize! It may spotId or apiKey is "
                    + "invalid.");
            mMediationRewardedVideoAdListener
                    .onInitializationFailed(nendAdapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);

            return;
        }
        mNendAdRewardedVideo = new NendAdRewardedVideo(activity, spotId, apiKey);
        mNendAdRewardedVideo.setAdListener(this);
        mNendAdRewardedVideo.setMediationName("AdMob");
        mIsInitialized = true;
    }

    private boolean isValidSpotIdAndKey(int spotId, String apiKey) {
        return !(spotId <= 0 || TextUtils.isEmpty(apiKey));
    }

    void onResume() {
    }

    void onPause() {
    }

    void showAd() {
        if (mNendAdRewardedVideo.isLoaded()) {
            Activity activity = mActivityWeakReference.get();
            if (activity == null) {
                // Activity is null, logging a warning and sending ad closed callback.
                Log.w(TAG, "An activity context is required to show Nend Ads, please call "
                        + "RewardedVideoAd#resume(Context) in your Activity's onResume.");
                mMediationRewardedVideoAdListener.onAdClosed(mNendAdapter);
                return;
            }

            mNendAdRewardedVideo.showAd(activity);
        } else {
            Log.w(NendRewardedAdapter.TAG, "Failed to show ad. Loading of video ad is not "
                    + "completed yet.");
        }
    }

    void loadAd(Bundle mediationExtras, MediationAdRequest mediationAdRequest) {
        if (mediationExtras != null) {
            mNendAdRewardedVideo
                    .setUserId(mediationExtras.getString(NendRewardedAdapter.KEY_USER_ID, ""));
        }
        NendAdUserFeature feature = NendAdRequestUtils.createUserFeature(mediationAdRequest);
        if (feature != null) {
            mNendAdRewardedVideo.setUserFeature(feature);
        }
        mNendAdRewardedVideo.loadAd();
    }

    void releaseAd() {
        mNendAdRewardedVideo.releaseAd();

        mMediationRewardedVideoAdListener = null;
    }

    boolean isInitialized() {
        return mIsInitialized;
    }

    @Override
    public void onRewarded(NendAdVideo nendAdVideo, NendAdRewardItem nendAdRewardItem) {
        mMediationRewardedVideoAdListener.onRewarded(
                mNendAdapter, new NendMediationRewardItem(nendAdRewardItem));
    }

    @Override
    public void onLoaded(NendAdVideo nendAdVideo) {
        mMediationRewardedVideoAdListener.onAdLoaded(mNendAdapter);
    }

    @Override
    public void onFailedToLoad(NendAdVideo nendAdVideo, int errorCode) {
        mMediationRewardedVideoAdListener.onAdFailedToLoad(
                mNendAdapter, ErrorUtil.convertErrorCodeFromNendVideoToAdMob(errorCode));
    }

    @Override
    public void onFailedToPlay(NendAdVideo nendAdVideo) {
        Log.e(NendRewardedAdapter.TAG, "Failed to play video ad.");
    }

    @Override
    public void onShown(NendAdVideo nendAdVideo) {
        mMediationRewardedVideoAdListener.onAdOpened(mNendAdapter);
    }

    @Override
    public void onClosed(NendAdVideo nendAdVideo) {
        mMediationRewardedVideoAdListener.onAdClosed(mNendAdapter);
    }

    @Override
    public void onStarted(NendAdVideo nendAdVideo) {
        mMediationRewardedVideoAdListener.onVideoStarted(mNendAdapter);
    }

    @Override
    public void onStopped(NendAdVideo nendAdVideo) {
    }

    @Override
    public void onCompleted(NendAdVideo nendAdVideo) {
    }

    @Override
    public void onAdClicked(NendAdVideo nendAdVideo) {
        mMediationRewardedVideoAdListener.onAdClicked(mNendAdapter);
        mMediationRewardedVideoAdListener.onAdLeftApplication(mNendAdapter);
    }

    @Override
    public void onInformationClicked(NendAdVideo nendAdVideo) {
        mMediationRewardedVideoAdListener.onAdLeftApplication(mNendAdapter);
    }

    public void contextChanged(Context context) {
        if (!(context instanceof Activity)) {
            Log.w(TAG, "Context is not an Activity. Nend Ads requires an Activity context to show"
                    + "ads.");
            return;
        }

        mActivityWeakReference = new WeakReference<>((Activity) context);
    }
}
