package com.google.ads.mediation.fyber;

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
import com.fyber.inneractive.sdk.external.InneractiveMediationDefs;
import com.fyber.inneractive.sdk.external.InneractiveMediationName;
import com.fyber.inneractive.sdk.external.InneractiveUnitController;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

/**
 * Class for rendering a Fyber Marketplace banner
 */
public class FyberBannerRenderer implements MediationBannerAd {
    // Definitions
    private final static String TAG = FyberBannerRenderer.class.getSimpleName();;

    // Members
    /** AdMob's Banner ad configuration object */
    MediationBannerAdConfiguration mAdConfiguration;
    /** AdMob's callback object */
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> mAdLoadCallback;

    // Definitions
    // TODO: Can we somehow separate AdMob from DFP?
    private final static InneractiveMediationName MEDIATOR_NAME = InneractiveMediationName.ADMOB;

    // Members
    /**
     * The Spot object for the banner
     */
    InneractiveAdSpot mBannerSpot;
    /**
     * The event listener of the Ad
     */
    InneractiveAdViewEventsListener mAdListener;
    /**
     * Flag to indicate external app been opened
     */
    Boolean mOpenedExternalApp = false;

    /** Holds the banner view which is created by Fyber, in order to return when AdMob calls getView */
    private View mView;

    /** Returned after calling {@link MediationAdLoadCallback#onSuccess(Object)} */
    private MediationBannerAdCallback mBannerAdCallback;

    /**
     * Constructor
     * @param adConfiguration AdMob banner ad configuration
     * @param adLoadCallback AdMob load callback
     */
    public FyberBannerRenderer(MediationBannerAdConfiguration adConfiguration,
                               MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> adLoadCallback) {
        mAdConfiguration = adConfiguration;
        mAdLoadCallback = adLoadCallback;
    }

    public void render() {
        Log.d(TAG, "render start");

        // Check that we got a valid spot id from the server
        String spotId = mAdConfiguration.getServerParameters().getString(FyberMediationAdapter.KEY_SPOT_ID);
        if (TextUtils.isEmpty(spotId)) {
            mAdLoadCallback.onFailure("Cannot render banner ad. Please define a valid spot id on the AdMob console");
            return;
        }

        mBannerSpot = InneractiveAdSpotManager.get().createSpot();
        mBannerSpot.setMediationName(MEDIATOR_NAME);

        InneractiveAdViewUnitController controller = new InneractiveAdViewUnitController();
        mBannerSpot.addUnitController(controller);

        InneractiveAdRequest request = new InneractiveAdRequest(spotId);

        // TODO: Parse network extras
        initRequestListener();

        mBannerSpot.requestAd(request);
    }

    private void initRequestListener() {
        // TODO: Fyber request listener can be implemented in renderer class instead of annon here
        mBannerSpot.setRequestListener(new InneractiveAdSpot.RequestListener() {
            @Override
            public void onInneractiveSuccessfulAdRequest(InneractiveAdSpot adSpot) {
                // Create a parent layout for the Banner Ad
                // TODO: Frame layout?
                ViewGroup outerLayout = new RelativeLayout(mAdConfiguration.getContext());

                InneractiveAdViewUnitController controller = (InneractiveAdViewUnitController) mBannerSpot
                        .getSelectedUnitController();

                controller.bindView(outerLayout);

                // Wait for a getView call
                mView = outerLayout;

                // Report load success to AdMob, and cache the returned callback for a later use
                MediationBannerAdCallback bannerAdCallback = mAdLoadCallback.onSuccess(FyberBannerRenderer.this);
                createFyberAdListener(controller, bannerAdCallback);
            }

            @Override
            public void onInneractiveFailedAdRequest(InneractiveAdSpot adSpot,
                                                     InneractiveErrorCode errorCode) {
                Log.d(TAG,
                        "Failed loading Banner! with error: " + errorCode);

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

    private void createFyberAdListener(InneractiveAdViewUnitController controller, final MediationBannerAdCallback callback) {
        // TODO: Move this to renderer class
        mAdListener = new InneractiveAdViewEventsListener() {
            @Override
            public void onAdImpression(InneractiveAdSpot adSpot) {
                callback.reportAdImpression();
                Log.d(TAG, InneractiveMediationDefs.IA_LOG_FOR_ADMOB_BANNER + " - onAdImpression");
            }

            @Override
            public void onAdClicked(InneractiveAdSpot adSpot) {
                callback.reportAdClicked();
            }

            @Override
            public void onAdWillCloseInternalBrowser(InneractiveAdSpot adSpot) {
                Log.d(TAG, InneractiveMediationDefs.IA_LOG_FOR_ADMOB_BANNER
                        + " - inneractiveInternalBrowserDismissed");
                callback.onAdClosed();
                mOpenedExternalApp = false;
            }

            @Override
            public void onAdWillOpenExternalApp(InneractiveAdSpot adSpot) {
                Log.d(TAG, InneractiveMediationDefs.IA_LOG_FOR_ADMOB_BANNER
                        + " - inneractiveAdWillOpenExternalApp");
                callback.onAdLeftApplication();
                mOpenedExternalApp = true;
            }

            @Override
            public void onAdEnteredErrorState(InneractiveAdSpot adSpot, InneractiveUnitController.AdDisplayError error) {
                Log.d(TAG, InneractiveMediationDefs.IA_LOG_FOR_ADMOB_BANNER
                        + " - onAdEnteredErrorState - " + error.getMessage());
            }

            @Override
            public void onAdExpanded(InneractiveAdSpot adSpot) {
                Log.d(TAG, InneractiveMediationDefs.IA_LOG_FOR_ADMOB_BANNER + " - onAdExpanded");
            }

            @Override
            public void onAdResized(InneractiveAdSpot adSpot) {
                Log.d(TAG, InneractiveMediationDefs.IA_LOG_FOR_ADMOB_BANNER + " - onAdResized");
            }

            @Override
            public void onAdCollapsed(InneractiveAdSpot adSpot) {
                Log.d(TAG, InneractiveMediationDefs.IA_LOG_FOR_ADMOB_BANNER + " - onAdCollapsed");
            }
        };

        controller.setEventsListener(mAdListener);
    }

    @NonNull
    @Override
    public View getView() {
        return mView;
    }
}
