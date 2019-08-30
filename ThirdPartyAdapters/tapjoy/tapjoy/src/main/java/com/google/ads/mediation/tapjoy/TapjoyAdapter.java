package com.google.ads.mediation.tapjoy;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.Tapjoy;

import java.util.Hashtable;

/**
 * A {@link com.google.android.gms.ads.mediation.MediationAdapter} used to load Tapjoy interstitial
 * ads and rewarded video ads for Google Mobile Ads SDK mediation.
 */
public class TapjoyAdapter extends TapjoyMediationAdapter
        implements MediationInterstitialAdapter {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Represents a {@link TJPlacement}.
     */
    private String sdkKey = null;
    private String interstitialPlacementName = null;

    private TJPlacement interstitialPlacement;
    private MediationInterstitialListener mediationInterstitialListener;

    @Override
    public void requestInterstitialAd(Context context,
                                      MediationInterstitialListener listener,
                                      Bundle serverParameters,
                                      MediationAdRequest mediationAdRequest,
                                      Bundle networkExtras) {
        mediationInterstitialListener = listener;

        if (!checkParams(context, serverParameters)) {
            this.mediationInterstitialListener
                    .onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            return;
        }

        Hashtable<String, Object> connectFlags = new Hashtable<>();
        if (networkExtras != null && networkExtras.containsKey(TapjoyExtrasBundleBuilder.DEBUG)) {
            connectFlags.put("TJC_OPTION_ENABLE_LOGGING",
                    networkExtras.getBoolean(TapjoyExtrasBundleBuilder.DEBUG, false));
        }

        TapjoyInitializer.getInstance().initialize((Activity) context, sdkKey, connectFlags,
                new TapjoyInitializer.Listener() {
            @Override
            public void onInitializeSucceeded() {
                if (interstitialPlacement != null && interstitialPlacement.isContentAvailable()) {
                    mediationInterstitialListener.onAdLoaded(TapjoyAdapter.this);
                } else {
                    // Make an ad request
                    createInterstitialPlacementAndRequestContent();
                }
            }

            @Override
            public void onInitializeFailed(String message) {
                Log.w(TAG, "Failed to load ad from Tapjoy: " + message);
                mediationInterstitialListener.onAdFailedToLoad(TapjoyAdapter.this,
                        AdRequest.ERROR_CODE_INTERNAL_ERROR);
            }
        });
    }

    private boolean checkParams(Context context, Bundle serverParameters) {
        String placementName = null;

        // Check for server parameters
        if (serverParameters != null) {
            sdkKey = serverParameters.getString(SDK_KEY_SERVER_PARAMETER_KEY);
            placementName = serverParameters.getString(PLACEMENT_NAME_SERVER_PARAMETER_KEY);
        }

        if (sdkKey == null || placementName == null) {
            Log.w(TAG, "Did not receive valid server parameters from AdMob");
            return false;
        }

        interstitialPlacementName = placementName;

        if (context instanceof Activity) {
            Tapjoy.setActivity((Activity) context);
        } else {
            Log.w(TAG, "Tapjoy requires an Activity context to initialize");
            return false;
        }

        return true;
    }

    private void createInterstitialPlacementAndRequestContent() {
        Log.i(TAG, "Creating interstitial placement for AdMob adapter");
        interstitialPlacement =
                Tapjoy.getPlacement(interstitialPlacementName, new TJPlacementListener() {
                    // Placement Callbacks
                    @Override
                    public void onRequestSuccess(TJPlacement tjPlacement) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!interstitialPlacement.isContentAvailable()) {
                                    mediationInterstitialListener.onAdFailedToLoad(
                                            TapjoyAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
                                }
                            }
                        });
                    }

                    @Override
                    public void onRequestFailure(TJPlacement tjPlacement, final TJError tjError) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.w(TAG, "Failed to request ad from Tapjoy: " + tjError.message);
                                mediationInterstitialListener.onAdFailedToLoad(
                                        TapjoyAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                            }
                        });
                    }

                    @Override
                    public void onContentReady(TJPlacement tjPlacement) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mediationInterstitialListener.onAdLoaded(TapjoyAdapter.this);
                            }
                        });
                    }

                    @Override
                    public void onContentShow(TJPlacement tjPlacement) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mediationInterstitialListener.onAdOpened(TapjoyAdapter.this);
                            }
                        });
                    }

                    @Override
                    public void onContentDismiss(TJPlacement tjPlacement) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mediationInterstitialListener.onAdClosed(TapjoyAdapter.this);
                            }
                        });
                    }

                    @Override
                    public void onPurchaseRequest(TJPlacement tjPlacement,
                                                  TJActionRequest tjActionRequest,
                                                  String s) {
                        // no-op
                    }

                    @Override
                    public void onRewardRequest(TJPlacement tjPlacement,
                                                TJActionRequest tjActionRequest,
                                                String s,
                                                int i) {
                        // no-op
                    }

                    @Override
                    public void onClick(TJPlacement tjPlacement) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mediationInterstitialListener.onAdClicked(TapjoyAdapter.this);
                                mediationInterstitialListener.onAdLeftApplication(TapjoyAdapter.this);
                            }
                        });
                    }
                });
        interstitialPlacement.setMediationName(MEDIATION_AGENT);
        interstitialPlacement.setAdapterVersion(TAPJOY_INTERNAL_ADAPTER_VERSION);

        requestInterstitialPlacementContent();
    }

    private void requestInterstitialPlacementContent() {
        interstitialPlacement.requestContent();
    }

    @Override
    public void showInterstitial() {
        Log.i(TAG, "Show interstitial content for Tapjoy-AdMob adapter");
        if (interstitialPlacement != null && interstitialPlacement.isContentAvailable()) {
            interstitialPlacement.showContent();
        }
    }

    @Override
    public void onDestroy() {
        // no-op
    }

    @Override
    public void onPause() {
        // no-op
    }

    @Override
    public void onResume() {
        // no-op
    }

    /**
     * The {@link TapjoyExtrasBundleBuilder} class is used to create a bundle containing
     * network-specific parameters for Tapjoy.
     */
    public static final class TapjoyExtrasBundleBuilder {
        /**
         * Key to enable or disable Tapjoy debugging.
         */
        private static final String DEBUG = "enable_debug";

        private boolean debugEnabled = false;

        public TapjoyExtrasBundleBuilder setDebug(boolean debug) {
            this.debugEnabled = debug;
            return this;
        }

        public Bundle build() {
            Bundle bundle = new Bundle();
            bundle.putBoolean(DEBUG, debugEnabled);
            return bundle;
        }
    }

}
