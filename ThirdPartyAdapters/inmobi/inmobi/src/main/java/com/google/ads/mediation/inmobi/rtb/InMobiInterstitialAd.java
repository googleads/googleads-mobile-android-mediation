package com.google.ads.mediation.inmobi.rtb;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.listeners.InterstitialAdEventListener;

import java.util.HashMap;
import java.util.Map;

public class InMobiInterstitialAd implements MediationInterstitialAd {
    private final InMobiInterstitial mInMobiInterstitial;
    private SignalCallbacks mSignalCallbacks;
    private MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
            mMediationAdLoadCallback;
    private MediationInterstitialAdCallback mInterstitialAdCallback;
    private final String TAG = InMobiInterstitialAd.class.getName();

    public InMobiInterstitialAd(Context context, long placementId) {
        mInMobiInterstitial = new InMobiInterstitial(context, placementId,
                new InterstitialAdEventListener() {
                    @Override
                    public void onAdLoadSucceeded(InMobiInterstitial inMobiInterstitial) {
                        Log.d(TAG, "onAdLoadSucceeded");
                        if (mMediationAdLoadCallback != null) {
                            mInterstitialAdCallback =
                                    mMediationAdLoadCallback.onSuccess(InMobiInterstitialAd.this);
                        }
                    }

                    @Override
                    public void onAdLoadFailed(InMobiInterstitial inMobiInterstitial,
                                               InMobiAdRequestStatus inMobiAdRequestStatus) {
                        String logMessage = "onAdLoadFailed: "
                                + inMobiAdRequestStatus.getMessage();
                        Log.d(TAG, logMessage);
                        if (mMediationAdLoadCallback != null) {
                            mMediationAdLoadCallback.onFailure(logMessage);
                        }
                    }

                    @Override
                    public void onAdClicked(InMobiInterstitial inMobiInterstitial, Map<Object,
                            Object> map) {
                        Log.d(TAG, "onAdClicked");
                        if (mInterstitialAdCallback != null) {
                            mInterstitialAdCallback.reportAdClicked();
                        }
                    }

                    @Override
                    public void onAdWillDisplay(InMobiInterstitial inMobiInterstitial) {
                        Log.d(TAG, "onAdWillDisplay");
                    }

                    @Override
                    public void onAdDisplayed(InMobiInterstitial inMobiInterstitial) {
                        Log.d(TAG, "onAdDisplayed");
                        if (mInterstitialAdCallback != null) {
                            mInterstitialAdCallback.onAdOpened();
                            mInterstitialAdCallback.reportAdImpression();
                        }
                    }

                    @Override
                    public void onAdDismissed(InMobiInterstitial inMobiInterstitial) {
                        Log.d(TAG, "onAdDismissed");
                        if (mInterstitialAdCallback != null) {
                            mInterstitialAdCallback.onAdClosed();
                        }
                    }

                    @Override
                    public void onUserLeftApplication(InMobiInterstitial inMobiInterstitial) {
                        Log.d(TAG, "onUserLeftApplication");
                        if (mInterstitialAdCallback != null) {
                            mInterstitialAdCallback.onAdLeftApplication();
                        }
                    }

                    @Override
                    public void onRequestPayloadCreated(byte[] bytes) {
                        String payload = new String(bytes);
                        Log.d(TAG, "onRequestPayloadCreated: " + payload);
                        if (mSignalCallbacks != null) {
                            mSignalCallbacks.onSuccess(payload);
                        }
                    }

                    @Override
                    public void onRequestPayloadCreationFailed(InMobiAdRequestStatus requestStatus) {
                        String logMessage = requestStatus.getMessage();
                        Log.d(TAG, "onRequestPayloadCreationFailed: " + logMessage);
                        if (mSignalCallbacks != null) {
                            mSignalCallbacks.onFailure(logMessage);
                        }
                    }
                });
        Map<String, String> extras = new HashMap<>();
        extras.put("tp", "c_admob");
        mInMobiInterstitial.setExtras(extras);
    }

    public void collectSignals(SignalCallbacks signalCallbacks) {
        mSignalCallbacks = signalCallbacks;
        mInMobiInterstitial.getSignals();
    }

    public void load(MediationInterstitialAdConfiguration adConfiguration,
                     MediationAdLoadCallback<MediationInterstitialAd,
                             MediationInterstitialAdCallback> callback) {
        mMediationAdLoadCallback = callback;
        mInMobiInterstitial.load(adConfiguration.getBidResponse().getBytes());
    }

    //MediationInterstitialAd implementation
    @Override
    public void showAd(Context context) {
        if (mInMobiInterstitial.isReady()) {
            mInMobiInterstitial.show();
        }
    }
}
