package com.google.ads.mediation.fyber;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveMediationDefs;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.InneractiveUnitController;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

/**
 * Class for rendering a Fyber Marketplace banner
 */
public class FyberInterstitialRenderer implements MediationInterstitialAd {
    // Definitions
    private final static String TAG = FyberInterstitialRenderer.class.getSimpleName();;

    // Members
    /** AdMob's Interstitial ad configuration object */
    MediationInterstitialAdConfiguration mAdConfiguration;
    /** AdMob's callback object */
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> mAdLoadCallback;

    // Definitions
    // TODO: Can we somehow separate AdMob from DFP?
    private final static InneractiveMediationName MEDIATOR_NAME = InneractiveMediationName.ADMOB;

    // Members
    /**
     * The Spot object for the banner
     */
    InneractiveAdSpot mInterstitialSpot;
    InneractiveFullscreenUnitController mUnitController;

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
    public FyberInterstitialRenderer(MediationInterstitialAdConfiguration adConfiguration,
                                     MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> adLoadCallback) {
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
                MediationInterstitialAdCallback interstitialAdCallback = mAdLoadCallback.onSuccess(FyberInterstitialRenderer.this);
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

    private void createFyberAdListener(InneractiveFullscreenUnitController controller, final MediationInterstitialAdCallback callback) {
        mAdListener = new InneractiveFullscreenAdEventsListener() {
            @Override
            public void onAdImpression(InneractiveAdSpot inneractiveAdSpot) {
                callback.reportAdImpression();
                callback.onAdOpened();
            }

            @Override
            public void onAdClicked(InneractiveAdSpot inneractiveAdSpot) {
                callback.reportAdClicked();
            }

            @Override
            public void onAdWillOpenExternalApp(InneractiveAdSpot inneractiveAdSpot) {
                callback.onAdLeftApplication();
            }

            @Override
            public void onAdEnteredErrorState(InneractiveAdSpot inneractiveAdSpot, InneractiveUnitController.AdDisplayError adDisplayError) {

            }

            @Override
            public void onAdWillCloseInternalBrowser(InneractiveAdSpot inneractiveAdSpot) {

            }

            @Override
            public void onAdDismissed(InneractiveAdSpot inneractiveAdSpot) {
                callback.onAdClosed();
            }
        };

        controller.setEventsListener(mAdListener);
    }

    @Override
    public void showAd(Context context) {
        // TODO: What about show errors. Is there an interface for that?
        if (mInterstitialSpot != null && mInterstitialSpot.isReady()) {
            mUnitController.show(context);
        }
    }
}
