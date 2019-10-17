package com.google.ads.mediation.fyber;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListenerAdapter;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.VideoContentListenerAdapter;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;

/**
 * Class for rendering a Fyber Marketplace rewarded video
 */
public class FyberRewardedVideoRenderer implements MediationRewardedAd {
    private final static String TAG = FyberRewardedVideoRenderer.class.getSimpleName();
    private final static String RENDER_FAILURE_ERROR_STR = "Cannot render rewarded ad. Please define a valid spot id on the AdMob UI";

    /** AdMob's Interstitial ad configuration object */
    private MediationRewardedAdConfiguration mAdConfiguration;
    /** AdMob's callback object */
    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;

    /**
     * AbMob's rewarded ad callback. as returned from {@link MediationAdLoadCallback#onSuccess}
     */
    private MediationRewardedAdCallback mRewardedAdCallback;

    /**
     * The Spot object for the banner
     */
    private InneractiveAdSpot mRewardedSpot;
    private InneractiveFullscreenUnitController mUnitController;
    private boolean mReceivedRewardItem = false;

    /**
     * Constructor
     * @param adConfiguration AdMob interstitial ad configuration
     * @param adLoadCallback AdMob load callback
     */
    FyberRewardedVideoRenderer(MediationRewardedAdConfiguration adConfiguration,
                                      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback) {
        mAdConfiguration = adConfiguration;
        mAdLoadCallback = adLoadCallback;
    }

    void render() {
        // Check that we got a valid spot id from the server
        String spotId = mAdConfiguration.getServerParameters().getString(FyberMediationAdapter.KEY_SPOT_ID);
        if (TextUtils.isEmpty(spotId)) {
            Log.w(TAG, RENDER_FAILURE_ERROR_STR);
            mAdLoadCallback.onFailure(RENDER_FAILURE_ERROR_STR);
            return;
        }

        mRewardedSpot = InneractiveAdSpotManager.get().createSpot();
        mRewardedSpot.setMediationName(InneractiveMediationName.ADMOB);

        mUnitController = new InneractiveFullscreenUnitController();
        mRewardedSpot.addUnitController(mUnitController);

        InneractiveAdRequest request = new InneractiveAdRequest(spotId);
        InneractiveAdSpot.RequestListener requestListener = createRequestListener();
        mRewardedSpot.setRequestListener(requestListener);

        mRewardedSpot.requestAd(request);
    }

    private InneractiveAdSpot.RequestListener createRequestListener() {
        InneractiveAdSpot.RequestListener requestListener = new InneractiveAdSpot.RequestListener() {
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
                // Report load success to AdMob, and cache the returned callback for a later use
                mRewardedAdCallback = mAdLoadCallback.onSuccess(FyberRewardedVideoRenderer.this);
                registerFyberAdListener(mUnitController, mRewardedAdCallback);
            }

            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
                                                     InneractiveErrorCode errorCode) {
                if (errorCode != InneractiveErrorCode.NO_FILL && errorCode != InneractiveErrorCode.CONNECTION_ERROR) {
                    Log.w(TAG, "Fyber rewarded video request failed. Error: " + errorCode);
                }

                mAdLoadCallback.onFailure("Error code: " + errorCode.toString());
            }
        };

        return requestListener;
    }

    /**
     * Creates a listener for Fyber's fullscreen placement events
     * @param controller the full screen controller
     * @param callback Google's rewarded ad callback
     */
    private void registerFyberAdListener(InneractiveFullscreenUnitController controller, final MediationRewardedAdCallback callback) {
        InneractiveFullscreenAdEventsListenerAdapter adListener = new InneractiveFullscreenAdEventsListenerAdapter() {
            @Override
            public void onAdImpression(InneractiveAdSpot inneractiveAdSpot) {
                callback.onAdOpened();
                callback.onVideoStart();
                callback.reportAdImpression();
            }

            @Override
            public void onAdClicked(InneractiveAdSpot inneractiveAdSpot) {
                callback.reportAdClicked();
            }

            @Override
            public void onAdDismissed(InneractiveAdSpot inneractiveAdSpot) {
                callback.onAdClosed();
                userEarnedReward(callback);
            }
        };

        // Listen to video completion event
        InneractiveFullscreenVideoContentController videoContentController =
                new InneractiveFullscreenVideoContentController();

        videoContentController.setEventsListener(new VideoContentListenerAdapter() {
            /**
             * Called by inneractive when a rewarded video ad was played to the end
             * <br>Note: This event does not indicate that the rewarded video was closed
             */
            @Override
            public void onCompleted() {
                callback.onVideoComplete();
                userEarnedReward(callback);
            }
        });

        controller.addContentController(videoContentController);
        controller.setEventsListener(adListener);
    }

    @Override
    public void showAd(Context context) {
        if (mRewardedSpot != null && mUnitController != null && mRewardedSpot.isReady()) {
            mUnitController.show(context);
        } else if (mRewardedAdCallback != null) {
            mRewardedAdCallback.onAdFailedToShow("showAd called, but Fyber's rewarded spot is not ready");
        }
    }

    /**
     * Small helper method, in order to report user earned reward for both video and mraid video ads
     * @param callback
     */
    private void userEarnedReward(MediationRewardedAdCallback callback) {
        if(!mReceivedRewardItem) {
            mReceivedRewardItem = true;

            callback.onUserEarnedReward(RewardItem.DEFAULT_REWARD);
        }
    }
}
