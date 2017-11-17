package com.google.ads.mediation.nend;


import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import net.nend.android.NendAdRewardItem;
import net.nend.android.NendAdRewardedListener;
import net.nend.android.NendAdRewardedVideo;
import net.nend.android.NendAdVideo;

import java.net.HttpURLConnection;

class NendMediationRewardedVideoEventForwarder implements NendAdRewardedListener {

    private NendAdRewardedVideo mNendAdRewardedVideo;
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;
    private NendRewardedAdapter mNendAdapter;
    private boolean mIsInitialized;
    private Activity mActivity;

    // Invalid network
    private static final int NEND_SDK_NETWORK_ERROR_CODE = 603;
    // https://github.com/fan-ADN/nendSDK-Android/wiki/Implementation-for-video-ads#acquire-error-information)

    NendMediationRewardedVideoEventForwarder(
            Activity activity, NendRewardedAdapter nendAdapter, Bundle serverParameters,
            MediationRewardedVideoAdListener listener) {
        this.mMediationRewardedVideoAdListener = listener;
        this.mNendAdapter = nendAdapter;
        this.mActivity = activity;

        String apiKey = serverParameters.getString("apiKey");
        int spotId = Integer.parseInt(serverParameters.getString("spotId"));

        if (!isValidSpotIdAndKey(spotId, apiKey)) {
            Log.e(NendRewardedAdapter.TAG, "Failed to initialize! It may spotId or apiKey is invalid.");
            mMediationRewardedVideoAdListener.onInitializationFailed(nendAdapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
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
            mNendAdRewardedVideo.showAd(mActivity);
        } else {
            Log.w(NendRewardedAdapter.TAG, "Failed to show ad. Loading of video ad is not completed yet.");
        }
    }

    void loadAd(Bundle mediationExtras) {
        if (mediationExtras != null) {
            mNendAdRewardedVideo.setUserId(mediationExtras.getString(NendRewardedAdapter.KEY_USER_ID, ""));
        }

        mNendAdRewardedVideo.loadAd();
    }

    void releaseAd() {
        mNendAdRewardedVideo.releaseAd();
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
                mNendAdapter, getAdMobErrorCode(errorCode));
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

    private int getAdMobErrorCode(int errorCode) {
        switch (errorCode) {
            case HttpURLConnection.HTTP_NO_CONTENT:
                return AdRequest.ERROR_CODE_NO_FILL;

            case HttpURLConnection.HTTP_BAD_REQUEST:
                return AdRequest.ERROR_CODE_INVALID_REQUEST;

            case NEND_SDK_NETWORK_ERROR_CODE:
                return AdRequest.ERROR_CODE_NETWORK_ERROR;

            case HttpURLConnection.HTTP_INTERNAL_ERROR:
            default:
                return AdRequest.ERROR_CODE_INTERNAL_ERROR;
        }
    }
}
