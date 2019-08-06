package com.google.ads.mediation.tapjoy;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJPlacementVideoListener;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyAuctionFlags;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Hashtable;

import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.MEDIATION_AGENT;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.PLACEMENT_NAME_SERVER_PARAMETER_KEY;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.SDK_KEY_SERVER_PARAMETER_KEY;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.TAG;
import static com.google.ads.mediation.tapjoy.TapjoyMediationAdapter.TAPJOY_INTERNAL_ADAPTER_VERSION;

public class TapjoyRewardedRenderer implements MediationRewardedAd, TJPlacementVideoListener {

    private static final String TAPJOY_DEBUG_FLAG_KEY = "enable_debug";

    private TJPlacement videoPlacement;

    private static boolean isRtbAd = false;


    private MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
            mAdLoadCallback;
    private MediationRewardedAdCallback mMediationRewardedAdCallback;
    private MediationRewardedAdConfiguration adConfiguration;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static HashMap<String, WeakReference<TapjoyRewardedRenderer>> mPlacementsInUse =
            new HashMap<>();

    public TapjoyRewardedRenderer(
            MediationRewardedAdConfiguration adConfiguration,
            MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> mAdLoadCallback) {
        this.adConfiguration = adConfiguration;
        this.mAdLoadCallback = mAdLoadCallback;
    }

    public void render() {
        if (!adConfiguration.getBidResponse().equals("")) {
            isRtbAd = true;
        }
        Bundle serverParameters = adConfiguration.getServerParameters();
        final String placementName =
                serverParameters.getString(PLACEMENT_NAME_SERVER_PARAMETER_KEY);

        if (TextUtils.isEmpty(placementName)) {
            String logMessage = "No placement name given for Tapjoy-AdMob adapter";
            Log.w(TAG, logMessage);
            mAdLoadCallback.onFailure(logMessage);
            return;
        }

        Log.i(TAG, "Loading ad for Tapjoy-AdMob adapter");
        Bundle networkExtras = adConfiguration.getMediationExtras();

        Context context = adConfiguration.getContext();
        if (!(context instanceof Activity)) {
            String logMessage = "Tapjoy SDK requires an Activity context to request ads";
            Log.e(TAG, logMessage);
            mAdLoadCallback.onFailure(logMessage);
            return;
        }
        Activity activity = (Activity) context;
        String sdkKey = serverParameters.getString(SDK_KEY_SERVER_PARAMETER_KEY);

        if (TextUtils.isEmpty(sdkKey)) {
            String logMessage = "Failed to request ad from Tapjoy: Missing or Invalid SDK Key.";
            Log.w(TAG, logMessage);
            mAdLoadCallback.onFailure(logMessage);
            return;
        }

        Tapjoy.setActivity(activity);

        Hashtable<String, Object> connectFlags = new Hashtable<>();
        if (networkExtras != null && networkExtras.containsKey(TAPJOY_DEBUG_FLAG_KEY)) {
            connectFlags.put("TJC_OPTION_ENABLE_LOGGING",
                    networkExtras.getBoolean(TAPJOY_DEBUG_FLAG_KEY, false));
        }

        TapjoyInitializer.getInstance().initialize(activity, sdkKey, connectFlags,
                new TapjoyInitializer.Listener() {
            @Override
            public void onInitializeSucceeded() {
                if (mPlacementsInUse.containsKey(placementName) &&
                        mPlacementsInUse.get(placementName).get() != null) {
                    String logMessage =
                            "An ad has already been requested for placement: " + placementName;
                    Log.w(TAG, logMessage);
                    mAdLoadCallback.onFailure(logMessage);
                    return;
                }

                mPlacementsInUse.put(placementName,
                        new WeakReference<>(TapjoyRewardedRenderer.this));
                createVideoPlacementAndRequestContent(placementName);
            }

            @Override
            public void onInitializeFailed(String message) {
                String logMessage = "Failed to request ad from Tapjoy: " + message;
                Log.w(TAG, logMessage);
                mAdLoadCallback.onFailure(logMessage);
            }
        });
    }

    private void createVideoPlacementAndRequestContent(final String placementName) {
        Log.i(TAG, "Creating video placement for AdMob adapter");

        videoPlacement = Tapjoy.getPlacement(placementName, new TJPlacementListener() {
            // Placement Callbacks
            @Override
            public void onRequestSuccess(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!videoPlacement.isContentAvailable()) {
                            mPlacementsInUse.remove(placementName);

                            String logMessage =
                                    "Failed to request rewarded ad from Tapjoy: No Fill.";
                            Log.w(TAG, logMessage);
                            if (mAdLoadCallback != null) {
                                mAdLoadCallback.onFailure(logMessage);
                            }
                        }
                    }
                });
            }

            @Override
            public void onRequestFailure(TJPlacement tjPlacement, final TJError tjError) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mPlacementsInUse.remove(placementName);

                        String logMessage =
                                "Failed to request rewarded ad from Tapjoy: " + tjError.message;
                        Log.w(TAG, logMessage);
                        if (mAdLoadCallback != null) {
                            mAdLoadCallback.onFailure(logMessage);
                        }
                    }
                });
            }

            @Override
            public void onContentReady(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Tapjoy Rewarded Ad is available.");
                        if (mAdLoadCallback != null) {
                            mMediationRewardedAdCallback =
                                    mAdLoadCallback.onSuccess(TapjoyRewardedRenderer.this);
                        }
                    }
                });
            }

            @Override
            public void onContentShow(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Tapjoy Rewarded Ad has been opened.");
                        if (mMediationRewardedAdCallback != null) {
                            mMediationRewardedAdCallback.onAdOpened();
                        }
                    }
                });
            }

            @Override
            public void onContentDismiss(TJPlacement tjPlacement) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Tapjoy Rewarded Ad has been closed.");
                        if (mMediationRewardedAdCallback != null) {
                            mMediationRewardedAdCallback.onAdClosed();
                        }
                        mPlacementsInUse.remove(placementName);
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
                        Log.d(TAG, "Tapjoy Rewarded Ad has been clicked.");
                        if (mMediationRewardedAdCallback != null) {
                            mMediationRewardedAdCallback.reportAdClicked();
                        }
                    }
                });
            }
        });

        videoPlacement.setMediationName(MEDIATION_AGENT);
        videoPlacement.setAdapterVersion(TAPJOY_INTERNAL_ADAPTER_VERSION);
        if (isRtbAd) {
            HashMap<String, String> auctionData = new HashMap<>();
            try {
                String bidResponse = adConfiguration.getBidResponse();
                JSONObject bidData = new JSONObject(bidResponse);
                String id = bidData.getString(TapjoyAuctionFlags.AUCTION_ID);
                String extData = bidData.getString(TapjoyAuctionFlags.AUCTION_DATA);
                auctionData.put(TapjoyAuctionFlags.AUCTION_ID, id);
                auctionData.put(TapjoyAuctionFlags.AUCTION_DATA, extData);
            } catch (JSONException e) {
                Log.e(TAG, "Bid Response JSON Error: " + e.getMessage());
            }
            videoPlacement.setAuctionData(auctionData);
        }
        videoPlacement.setVideoListener(this);
        videoPlacement.requestContent();
    }


    @Override
    public void showAd(Context context) {
        Log.i(TAG, "Show video content for Tapjoy-AdMob adapter");
        if (videoPlacement != null && videoPlacement.isContentAvailable()) {
            videoPlacement.showContent();
        } else if (mMediationRewardedAdCallback != null) {
            mMediationRewardedAdCallback.onAdFailedToShow("Tapjoy Rewarded Ad is not ready.");
        }
    }

    /**
     * {@link TJPlacementVideoListener} implementation.
     */

    @Override
    public void onVideoStart(TJPlacement tjPlacement) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Tapjoy Rewarded Ad has started playing.");
                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.onVideoStart();
                    mMediationRewardedAdCallback.reportAdImpression();
                }
            }
        });
    }

    @Override
    public void onVideoError(final TJPlacement tjPlacement, final String errorMessage) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                mPlacementsInUse.remove(tjPlacement.getName());
                Log.w(TAG, "Tapjoy Rewarded Ad has failed to play: " + errorMessage);
                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.onAdFailedToShow(errorMessage);
                }
            }
        });
    }

    @Override
    public void onVideoComplete(TJPlacement tjPlacement) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Tapjoy Rewarded Ad has finished playing.");
                if (mMediationRewardedAdCallback != null) {
                    mMediationRewardedAdCallback.onVideoComplete();
                    mMediationRewardedAdCallback.onUserEarnedReward(new TapjoyReward());
                }
            }
        });
    }

    /**
     * A {@link RewardItem} used to map Tapjoy reward to Google's reward.
     */
    public class TapjoyReward implements RewardItem {
        @Override
        public String getType() {
            // Tapjoy only supports fixed rewards and doesn't provide a reward type.
            return "";
        }

        @Override
        public int getAmount() {
            // Tapjoy only supports fixed rewards and doesn't provide a reward amount.
            return 1;
        }
    }
}
