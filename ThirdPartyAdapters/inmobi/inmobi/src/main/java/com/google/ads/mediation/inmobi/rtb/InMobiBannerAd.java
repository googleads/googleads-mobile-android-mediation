package com.google.ads.mediation.inmobi.rtb;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.listeners.BannerAdEventListener;

import java.util.HashMap;
import java.util.Map;

public class InMobiBannerAd implements MediationBannerAd  {
    private final InMobiBanner mInMobiBanner;
    private SignalCallbacks mSignalsCallback;
    private MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
            mMediationAdLoadCallback;
    private MediationBannerAdCallback mBannerAdCallback;
    private final String TAG = InMobiBannerAd.class.getName();

    public InMobiBannerAd(Context context, long placementId, AdSize adSize) {
        mInMobiBanner = new InMobiBanner(context, placementId);
        mInMobiBanner.setBannerSize(adSize.getWidth(), adSize.getHeight());
        mInMobiBanner.setListener(new BannerAdEventListener() {
            @Override
            public void onAdLoadSucceeded(InMobiBanner inMobiBanner) {
                Log.d(TAG, "onAdLoadSucceeded");
                if (mMediationAdLoadCallback != null) {
                    mBannerAdCallback = mMediationAdLoadCallback.onSuccess(InMobiBannerAd.this);
                    mBannerAdCallback.reportAdImpression();
                }
            }

            @Override
            public void onAdLoadFailed(InMobiBanner inMobiBanner,
                                       InMobiAdRequestStatus inMobiAdRequestStatus) {
                String logMessage = "onAdLoadFailed: " + inMobiAdRequestStatus.getMessage();
                Log.d(TAG, logMessage);
                if (mMediationAdLoadCallback != null) {
                    mMediationAdLoadCallback.onFailure(logMessage);
                }
            }

            @Override
            public void onAdClicked(InMobiBanner inMobiBanner, Map<Object, Object> map) {
                Log.d(TAG, "onAdClicked");
                if (mBannerAdCallback != null) {
                    mBannerAdCallback.reportAdClicked();
                }
            }

            @Override
            public void onAdDisplayed(InMobiBanner inMobiBanner) {
                Log.d(TAG, "onAdDisplayed");
                if (mBannerAdCallback != null) {
                    mBannerAdCallback.onAdOpened();
                }
            }

            @Override
            public void onAdDismissed(InMobiBanner inMobiBanner) {
                Log.d(TAG, "onAdDismissed");
                if (mBannerAdCallback != null) {
                    mBannerAdCallback.onAdClosed();
                }
            }

            @Override
            public void onUserLeftApplication(InMobiBanner inMobiBanner) {
                Log.d(TAG, "onUserLeftApplication");
                if (mBannerAdCallback != null) {
                    mBannerAdCallback.onAdLeftApplication();
                }
            }

            @Override
            public void onRequestPayloadCreated(byte[] bytes) {
                String payload = new String(bytes);
                Log.d(TAG, "onRequestPayloadCreated: " + payload);
                if (mSignalsCallback != null) {
                    mSignalsCallback.onSuccess(payload);
                }
            }

            @Override
            public void onRequestPayloadCreationFailed(InMobiAdRequestStatus status) {
                String logMessage = status.getMessage();
                Log.d(TAG, "onRequestPayloadCreationFailed: "+ logMessage);
                if (mSignalsCallback != null) {
                    mSignalsCallback.onFailure(logMessage);
                }
            }
        });
        Map<String, String> extras = new HashMap<>();
        extras.put("tp","c_admob");
        mInMobiBanner.setExtras(extras);
    }


    public void collectSignals(final SignalCallbacks signalCallbacks) {
        mSignalsCallback = signalCallbacks;
        mInMobiBanner.getSignals();
    }


    public void load(MediationBannerAdConfiguration adConfiguration,
                     MediationAdLoadCallback<MediationBannerAd,
                             MediationBannerAdCallback> callback) {
        mMediationAdLoadCallback = callback;
        mInMobiBanner.load(adConfiguration.getBidResponse().getBytes());
    }

    //MediationBannerAd implementation
    @NonNull
    @Override
    public View getView() {
        return mInMobiBanner;
    }
}
