package com.google.ads.mediation.fyber;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveContentController;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController;
import com.fyber.inneractive.sdk.external.InneractiveMediationDefs;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.InneractiveUnitController;
import com.fyber.inneractive.sdk.external.VideoContentListener;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
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
                Log.d(TAG,
                        "Failed loading Interstitial! with error: " + errorCode);

                mAdLoadCallback.onFailure("Error code: " + errorCode.toString());

                /** No specific error codes anymore? */

                /*
                if (errorCode == InneractiveErrorCode.CONNECTION_ERROR
                        || errorCode == InneractiveErrorCode.CONNECTION_TIMEOUT) {
                    mAdLoadCallback.onFailure(AdRequest.ERROR_CODE_NETWORK_ERROR);
                } else if (errorCode == InneractiveErrorCode.NO_FILL) {
                    customEventListener.onAdFailedToLoad(AdRequest.ERROR_CODE_NO_FILL);
                } else {
                    customEventListener.onAdFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }*/
            }
        });
    }

    private void createFyberAdListener(InneractiveFullscreenUnitController controller, final MediationRewardedAdCallback callback) {
        mAdListener = new InneractiveFullscreenAdEventsListener() {
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
            public void onAdWillOpenExternalApp(InneractiveAdSpot inneractiveAdSpot) {
                // No relevant callback for rewarded videos
            }

            @Override
            public void onAdEnteredErrorState(InneractiveAdSpot inneractiveAdSpot, InneractiveUnitController.AdDisplayError adDisplayError) {
                //Do not call callback.onAdFailedToShow(adDisplayError.getMessage());
                // This callback is only used for MRaid videos, which were already displayed
            }

            @Override
            public void onAdWillCloseInternalBrowser(InneractiveAdSpot inneractiveAdSpot) {
                // No relevant callback for rewarded videos
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

        videoContentController.setEventsListener(new VideoContentListener() {
            @Override
            public void onProgress(int totalDurationInMsec, int positionInMsec) {
                Log.d(InneractiveMediationDefs.IA_LOG_FOR_ADMOB_INTERSTITIAL, "Interstitial: Got video content progress: total time = " + totalDurationInMsec + " position = " + positionInMsec);
            }

            /**
             * Called by inneractive when an Intersititial video ad was played to the end
             * <br>Can be used for incentive flow
             * <br>Note: This event does not indicate that the interstitial was closed
             */
            @Override
            public void onCompleted() {
                callback.onVideoComplete();

                // TODO: Show we send the reward back when the video is completed, or only when the ad is dismissed?
                // Isn't this redundant?
                userEarnedReward(callback);

                Log.d(InneractiveMediationDefs.IA_LOG_FOR_ADMOB_INTERSTITIAL, "Interstitial: Got video content completed event");
            }

            // Deprecated
            @Override
            public void onPlayerError() {
                // No relevant callback for player error
                // We automatically move to the end card, so we still report the reward back in onAdDismissed
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

            // For now, the reward item always equals 1
            // Where was this taken from?
            /*callback.onUserEarnedReward(new RewardItem() {
                @Override
                public String getType() {
                    return "";
                }

                @Override
                public int getAmount() {
                    return 1;
                }
            });*/
        }
    }
}
