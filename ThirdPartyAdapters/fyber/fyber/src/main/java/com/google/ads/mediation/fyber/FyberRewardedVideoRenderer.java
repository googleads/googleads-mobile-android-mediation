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
import com.fyber.inneractive.sdk.external.InneractiveMediationDefs;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.InneractiveUnitController;
import com.fyber.inneractive.sdk.external.VideoContentListener;
import com.fyber.inneractive.sdk.external.VideoContentListenerAdapter;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;

/**
 * Class for rendering a Fyber Marketplace banner
 */
public class FyberRewardedVideoRenderer implements MediationRewardedAd {
    // Definitions
    private final static String TAG = FyberRewardedVideoRenderer.class.getSimpleName();;

    // Members
    /** AdMob's Interstitial ad configuration object */
    MediationRewardedAdConfiguration mAdConfiguration;
    /** AdMob's callback object */
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback;

    // Definitions
    // TODO: Can we somehow separate AdMob from DFP?
    private final static InneractiveMediationName MEDIATOR_NAME = InneractiveMediationName.ADMOB;

    // Members
    /**
     * The Spot object for the banner
     */
    InneractiveAdSpot mInterstitialSpot;
    InneractiveFullscreenUnitController mUnitController;
    private boolean mReceivedRewardItem = false;

    /**
     * The event listener of the Ad
     */
    InneractiveFullscreenAdEventsListener mAdListener;

    /** Returned after calling {@link MediationAdLoadCallback#onSuccess(Object)} */
    private MediationInterstitialAdCallback mIntersititialAdCallback;

    /**
     * Constructor
     * @param adConfiguration AdMob interstitial ad configuration
     * @param adLoadCallback AdMob load callback
     */
    public FyberRewardedVideoRenderer(MediationRewardedAdConfiguration adConfiguration,
                                      MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> adLoadCallback) {
        mAdConfiguration = adConfiguration;
        mAdLoadCallback = adLoadCallback;
    }

    public void render() {
        Log.d(TAG, "render start");

        // Check that we got a valid spot id from the server
        String spotId = mAdConfiguration.getServerParameters().getString(FyberMediationAdapter.KEY_SPOT_ID);
        if (TextUtils.isEmpty(spotId)) {
            mAdLoadCallback.onFailure("Cannot render interstitial ad. Please define a valid spot id on the AdMob console");
            return;
        }

        mInterstitialSpot = InneractiveAdSpotManager.get().createSpot();
        mInterstitialSpot.setMediationName(MEDIATOR_NAME);

        mUnitController = new InneractiveFullscreenUnitController();
        mInterstitialSpot.addUnitController(mUnitController);

        InneractiveAdRequest request = new InneractiveAdRequest(spotId);

        // TODO: Parse network extras
        initRequestListener();

        mInterstitialSpot.requestAd(request);
    }

    private void initRequestListener() {
        mInterstitialSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
                if (adSpot != mInterstitialSpot) {
                    Log.d(TAG, "Wrong Interstitial Spot: Received - " + adSpot + ", Actual - " + mInterstitialSpot);
                    return;
                }

                // Report load success to AdMob, and cache the returned callback for a later use
                MediationRewardedAdCallback interstitialAdCallback = mAdLoadCallback.onSuccess(FyberRewardedVideoRenderer.this);
                createFyberAdListener(mUnitController, interstitialAdCallback);
            }

            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
                                                     InneractiveErrorCode errorCode) {
                mAdLoadCallback.onFailure("Error code: " + errorCode.toString());
            }
        });
    }

    private void createFyberAdListener(InneractiveFullscreenUnitController controller, final MediationRewardedAdCallback callback) {
        mAdListener = new InneractiveFullscreenAdEventsListenerAdapter() {
            @Override
            public void onAdImpression(InneractiveAdSpot inneractiveAdSpot) {
                callback.reportAdImpression();
                callback.onAdOpened();
                callback.onVideoStart();
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
             * Called by inneractive when an Intersititial video ad was played to the end
             * <br>Can be used for incentive flow
             * <br>Note: This event does not indicate that the interstitial was closed
             */
            @Override
            public void onCompleted() {
                callback.onVideoComplete();
                userEarnedReward(callback);

                Log.d(InneractiveMediationDefs.IA_LOG_FOR_ADMOB_INTERSTITIAL, "Interstitial: Got video content completed event");
            }
        });

        controller.addContentController(videoContentController);
        controller.setEventsListener(mAdListener);
    }

    @Override
    public void showAd(Context context) {
        // TODO: What about show errors. Is there an interface for that?
        if (mInterstitialSpot != null && mUnitController != null && mInterstitialSpot.isReady()) {
            mUnitController.show(context);
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
