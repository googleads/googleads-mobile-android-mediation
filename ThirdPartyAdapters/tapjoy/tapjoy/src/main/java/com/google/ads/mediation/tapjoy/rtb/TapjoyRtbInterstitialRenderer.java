package com.google.ads.mediation.tapjoy.rtb;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.ads.mediation.MediationAdConfiguration;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyAuctionFlags;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class TapjoyRtbInterstitialRenderer implements MediationInterstitialAd {
    private final String TAG = "TapjoyRTB Interstitial";
    /**
     * Data used to render an RTB interstitial ad.
     */
    private MediationAdConfiguration adConfiguration;

    /**
     * Callback object to notify the Google Mobile Ads SDK if ad rendering succeeded or failed.
     */
    private final MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback;

    private String interstitialPlacementName = null;

    private static final String PLACEMENT_NAME_SERVER_PARAMETER_KEY = "placementName";
    private static final String MEDIATION_AGENT = "admob";
    private static final String TAPJOY_INTERNAL_ADAPTER_VERSION =
            "2.0.0";

    private static HashMap<String, WeakReference<TapjoyRtbInterstitialRenderer>> placementsInUse =
            new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     *  Tapjoy Interstitial Placement
     *
     */
    private TJPlacement interstitialPlacement;

    /**
     * Listener object to notify the Google Mobile Ads SDK of interstitial
     */
    private MediationInterstitialAdCallback listener;

    public TapjoyRtbInterstitialRenderer(
            MediationAdConfiguration adConfiguration,
            MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> callback) {
        this.adConfiguration = adConfiguration;
        this.callback = callback;
    }

    public void render() {
        Log.i(TAG,"Rendering interstitial placement for AdMob adapter");

        if (checkParams()) {
            if (placementsInUse.containsKey(interstitialPlacementName) &&
                    placementsInUse.get(interstitialPlacementName).get() != null) {
                String logMessage =
                        "An ad has already been requested for placement: " + interstitialPlacementName;
                Log.w(TAG, logMessage);
                callback.onFailure(logMessage);
                return;
            }
            placementsInUse.put(interstitialPlacementName,
                    new WeakReference<>(TapjoyRtbInterstitialRenderer.this));
            createInterstitialPlacementAndRequestContent();
        } else {
            callback.onFailure("Invalid server parameters specified in the UI");
        }
    }

    @Override
    public void showAd(Context context) {
        Log.i(TAG, "Show interstitial content for Tapjoy-AdMob adapter");
        if (interstitialPlacement != null && interstitialPlacement.isContentAvailable()) {
            interstitialPlacement.showContent();
        }
    }

    private boolean checkParams() {
        //Check for server parameters
        if (adConfiguration.getServerParameters().getString(PLACEMENT_NAME_SERVER_PARAMETER_KEY)
                != null) {
            interstitialPlacementName = adConfiguration.getServerParameters()
                    .getString(PLACEMENT_NAME_SERVER_PARAMETER_KEY);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Tapjoy Callbacks
     */
    private void createInterstitialPlacementAndRequestContent() {
        interstitialPlacement = Tapjoy.getPlacement(interstitialPlacementName, new TJPlacementListener() {
            @Override
            public void onRequestSuccess(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!interstitialPlacement.isContentAvailable()) {
                            placementsInUse.remove(interstitialPlacementName);
                            callback.onFailure("NO_FILL");
                            Log.d(TAG,"Interstitial Content isn't available");
                        }
                    }
                });
            }

            @Override
            public void onRequestFailure(TJPlacement tjPlacement, final TJError tjError) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        placementsInUse.remove(interstitialPlacementName);
                        callback.onFailure(tjError.message);
                    }
                });
            }

            @Override
            public void onContentReady(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener = callback.onSuccess(TapjoyRtbInterstitialRenderer.this);
                        Log.d(TAG,"Interstitial onContentReady");
                    }
                });
            }

            @Override
            public void onContentShow(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null){
                            listener.onAdOpened();
                            listener.reportAdImpression();
                        }
                    }
                });
            }

            @Override
            public void onContentDismiss(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(listener != null){
                            listener.onAdClosed();
                        }
                        placementsInUse.remove(interstitialPlacementName);
                    }
                });
            }

            @Override
            public void onPurchaseRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s) {

            }

            @Override
            public void onRewardRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s, int i) {

            }

            @Override
            public void onClick(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null) {
                            listener.reportAdClicked();
                            listener.onAdLeftApplication();
                        }
                    }
                });
            }
        });

        interstitialPlacement.setMediationName(MEDIATION_AGENT);
        interstitialPlacement.setAdapterVersion(TAPJOY_INTERNAL_ADAPTER_VERSION);
        HashMap<String, String> auctionData = new HashMap<>();

        try {
            JSONObject bidData = new JSONObject(adConfiguration.getBidResponse());

            String id = bidData.getString(TapjoyAuctionFlags.AUCTION_ID);
            String extData = bidData.getString(TapjoyAuctionFlags.AUCTION_DATA);

            auctionData.put(TapjoyAuctionFlags.AUCTION_ID, id);
            auctionData.put(TapjoyAuctionFlags.AUCTION_DATA, extData);

        } catch (JSONException e) {
            Log.e(TAG, "Bid Response JSON Error: " + e.getMessage());
        }
        interstitialPlacement.setAuctionData(auctionData);
        interstitialPlacement.requestContent();
    }
}
