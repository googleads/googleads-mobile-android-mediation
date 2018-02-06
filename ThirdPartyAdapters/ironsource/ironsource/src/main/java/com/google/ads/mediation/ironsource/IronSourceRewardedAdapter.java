package com.google.ads.mediation.ironsource;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import android.app.Activity;

import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;
import com.ironsource.mediationsdk.model.Placement;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;

import com.google.android.gms.ads.AdRequest;
import com.ironsource.mediationsdk.IronSource;
import com.ironsource.mediationsdk.logger.IronSourceError;

import com.ironsource.mediationsdk.sdk.ISDemandOnlyRewardedVideoListener;

/**
 * A {@link MediationRewardedVideoAdAdapter} to load and show myTarget rewarded video ads.
 */
public class IronSourceRewardedAdapter extends IronSourceBaseAdapter implements MediationRewardedVideoAdAdapter,
ISDemandOnlyRewardedVideoListener {

    /**
     * Mediation rewarded video ad listener used to forward reward-based video ad events from
     * IronSource SDK to Google Mobile Ads SDK.
     */
    private MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;
    
    /**
     * MediationRewardedVideoAdAdapter implementation
     */
    
    @Override
    public void initialize(Context context,
                           MediationAdRequest mediationAdRequest,
                           String userId,
                           MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
                           Bundle serverParameters,
                           Bundle networkExtras) {
        onLog("initialize");
        mMediationRewardedVideoAdListener = mediationRewardedVideoAdListener;
        
        if (!(context instanceof Activity)) {
            // Context not an Activity context, log the reason for failure and fail the
            // initialization.
            onLog("IronSource SDK requires an Activity context to initialize");
            mMediationRewardedVideoAdListener.onInitializationFailed(IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }
        
        try {

            // Parse enabling testing mode key for log
            this.mIsLogEnabled = mediationAdRequest.isTesting();

            String appKey = serverParameters.getString(KEY_APP_KEY);
            if (TextUtils.isEmpty(appKey)) {
                onLog("onInitializationFailed, make sure that 'appKey' server parameter is added");
                mMediationRewardedVideoAdListener.onInitializationFailed(IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                
                return;
            }

            this.mInstanceID = serverParameters.getString(KEY_INTANCE_ID, "0");
            
            // Everything is ok, continue with IronSource initialization
            onLog("Server params for RV | appKey: " + appKey + " | isTestEnabled: " + this.mIsLogEnabled + " | InstanceID: " +this.mInstanceID );
            
            IronSource.setISDemandOnlyRewardedVideoListener(this);
            initIronSourceSDK(context, appKey, IronSource.AD_UNIT.REWARDED_VIDEO);
            
            // in case of Rewarded Video ad unit we report init success
            onRewardedVideoInitSuccess();
            
        } catch (Exception e) {
            mMediationRewardedVideoAdListener.onInitializationFailed(IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
        }
    }
    
    @Override
    public void loadAd(MediationAdRequest mediationAdRequest, Bundle serverParameters, Bundle networkExtras) {

        this.mInstanceID = serverParameters.getString(KEY_INTANCE_ID, "0");

        onLog("loadAd for instance: " + this.mInstanceID);
        if (IronSource.isISDemandOnlyRewardedVideoAvailable(this.mInstanceID)) {
            mMediationRewardedVideoAdListener.onAdLoaded(IronSourceRewardedAdapter.this);
        }
    }
    
    @Override
    public void showVideo() {
        onLog("showVideo for instance: " + this.mInstanceID);

        if (IronSource.isISDemandOnlyRewardedVideoAvailable(this.mInstanceID)) {
            IronSource.showISDemandOnlyRewardedVideo(this.mInstanceID);
        } else {
            // Show ad will only be called if the adapter sends back an ad
            // loaded callback in response to a loadAd request. If for any
            // reason the adapter is not ready to show an ad after sending
            // an ad loaded callback, log a warning.
            onLog("No ads to show.");
        }
    }
    
    @Override
    public boolean isInitialized() {
        onLog("isInitialized: " + this.mInitSucceeded);
        return this.mInitSucceeded;
    }
    
    @Override
    public void onDestroy() {
        onLog("onDestroy");
    }
    
    @Override
    public void onPause() {
        onLog("onPause");
    }
    
    @Override
    public void onResume() {
        onLog("onResume");
    }
    
    /**
     * Private IronSource methods
     */
    
    private void onRewardedVideoInitSuccess() {
        onLog("onRewardedVideoInitSuccess");
        
        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    onLog("onInitializationSucceeded");
                    mMediationRewardedVideoAdListener.onInitializationSucceeded(IronSourceRewardedAdapter.this);
                }
            });
        }
    }
    
    /**
     * IronSource RewardedVideoListener implementation
     */
    
    @Override
    public void onRewardedVideoAvailabilityChanged(String instanceId, final boolean available) {
        onLog("onRewardedVideoAvailabilityChanged " + available + " for instance: " + instanceId);
        
        //We handle callbacks only for registered instances
        if (!this.mInstanceID.equals(instanceId))
            return;
        
        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    if (available) {
                        mMediationRewardedVideoAdListener.onAdLoaded(IronSourceRewardedAdapter.this);
                    } else {
                        mMediationRewardedVideoAdListener.onAdFailedToLoad(IronSourceRewardedAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                    }
                }
            });
        }
    }
    
    @Override
    public void onRewardedVideoAdOpened(final String instanceId) {
        onLog("onRewardedVideoAdOpened for instance: " + instanceId);

        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedVideoAdListener.onAdOpened(IronSourceRewardedAdapter.this);
                    mMediationRewardedVideoAdListener.onVideoStarted(IronSourceRewardedAdapter.this);
                }
            });
        }
    }
    
    @Override
    public void onRewardedVideoAdClosed(String instanceId) {
        onLog("onRewardedVideoAdClosed for instance: " + instanceId);
        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedVideoAdListener.onAdClosed(IronSourceRewardedAdapter.this);
                }
            });
        }
    }
    
    @Override
    public void onRewardedVideoAdRewarded(String instanceId, final Placement placement) {
        if (placement == null) {
            onLog("IronSource Placement Error");
            return;
        }
        
        final IronSourceReward reward = new IronSourceReward(placement);
        onLog("onRewardedVideoAdRewarded for instance: " + instanceId + "rewarded: " + reward.getType() + " " + reward.getAmount());

        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedVideoAdListener.onRewarded(IronSourceRewardedAdapter.this, new IronSourceReward(placement));
                }
            });
        }
    }
    
    @Override
    public void onRewardedVideoAdShowFailed(String instanceId, IronSourceError ironsourceError) {
        // No relevant delegate in AdMob interface
        onLog("onRewardedVideoAdShowFailed: " + ironsourceError.getErrorMessage() + " for instance: " + instanceId);
    }
    
    @Override
    public void onRewardedVideoAdClicked(String instanceId, Placement placement) {
        onLog("onRewardedVideoAdClicked, placement: " + placement.getPlacementName() + " for instance: " + instanceId);
        
        if (mMediationRewardedVideoAdListener != null) {
            sendEventOnUIThread(new Runnable() {
                public void run() {
                    mMediationRewardedVideoAdListener.onAdClicked(IronSourceRewardedAdapter.this);
                    mMediationRewardedVideoAdListener.onAdLeftApplication(IronSourceRewardedAdapter.this);
                }
            });
        }
    }
    
    /**
     * A {@link RewardItem} used to map IronSource reward to Google's reward.
     */

    class IronSourceReward implements RewardItem {
        
        private final Placement mPlacement;
        
        IronSourceReward(Placement placement) {
            this.mPlacement = placement;
        }
        
        @Override
        public String getType() {
            return mPlacement.getRewardName();
        }
        
        @Override
        public int getAmount() {
            return mPlacement.getRewardAmount();
        }
    }
}
