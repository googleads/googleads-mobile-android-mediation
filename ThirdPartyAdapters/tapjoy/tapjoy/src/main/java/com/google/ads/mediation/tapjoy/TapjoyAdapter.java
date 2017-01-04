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
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJConnectListener;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJPlacementVideoListener;
import com.tapjoy.Tapjoy;

import java.util.Hashtable;

public class TapjoyAdapter implements MediationInterstitialAdapter, MediationRewardedVideoAdAdapter, TJPlacementVideoListener {

	private static final String TAG = TapjoyAdapter.class.getSimpleName();
	private  static final String SDK_KEY_SERVER_PARAMETER_KEY = "sdkKey";
	private static final String PLACEMENT_NAME_SERVER_PARAMETER_KEY = "placementName";
	private static final String MEDIATION_AGENT = "admob";
	private static final String TAPJOY_INTERNAL_ADAPTER_VERSION = "1.0.0"; // only used internally for Tapjoy SDK

	private final Handler mainHandler = new Handler(Looper.getMainLooper());

	/** Represents a {@link TJPlacement}. */

	private String sdkKey = null;
	private String videoPlacementName = null;
	private String interstitialPlacementName = null;

	private boolean isInitialized;
	private boolean videoPlacementIsRequesting = false;
	private boolean interstitialPlacementIsRequesting = false;

	private TJPlacement videoPlacement;
	private MediationRewardedVideoAdListener mediationRewardedVideoAdListener;

	private TJPlacement interstitialPlacement;
	private MediationInterstitialListener mediationInterstitialListener;

	public enum RequestType {
		INTERSTITIAL, VIDEO
	}

	@Override
	public void initialize(Context context,
												 MediationAdRequest mediationAdRequest,
												 String unused,
												 MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
												 Bundle serverParameters,
												 Bundle networkExtras) {
		this.mediationRewardedVideoAdListener = mediationRewardedVideoAdListener;

		if (mediationRewardedVideoAdListener == null) {
			Log.i(TAG, "Did not receive MediationRewardedVideoAdListener from AdMob");
			return;
		}

		if (!checkParams(context, serverParameters, RequestType.VIDEO)) {
			this.mediationRewardedVideoAdListener.onInitializationFailed(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
			return;
		}

		if (!Tapjoy.isConnected()) {

			Hashtable<String, Object> connectFlags = new Hashtable<String, Object>();
			if (networkExtras != null) {
				if (networkExtras.containsKey(TapjoyExtrasBundleBuilder.DEBUG)) {
					connectFlags.put("TJC_OPTION_ENABLE_LOGGING", networkExtras.getBoolean(TapjoyExtrasBundleBuilder.DEBUG, false));
				}
			}

			Log.i(TAG, "Connecting to Tapjoy for Tapjoy-AdMob adapter");
			Tapjoy.connect(context, sdkKey, connectFlags, new TJConnectListener() {

				@Override
				public void onConnectSuccess() {
					TapjoyAdapter.this.mediationRewardedVideoAdListener.onInitializationSucceeded(TapjoyAdapter.this);
				}

				@Override
				public void onConnectFailure() {
					Log.d(TAG, "Tapjoy failed to connect");
					TapjoyAdapter.this.mediationRewardedVideoAdListener.onInitializationFailed(TapjoyAdapter.this, AdRequest.ERROR_CODE_INVALID_REQUEST);
				}
			});
		} else {
			mediationRewardedVideoAdListener.onInitializationSucceeded(this);
		}
	}

	@Override
	public void requestInterstitialAd(
			Context context,
			MediationInterstitialListener listener,
			Bundle serverParameters,
			MediationAdRequest mediationAdRequest,
			Bundle networkExtras) {

		mediationInterstitialListener = listener;

		if (!checkParams(context, serverParameters, RequestType.INTERSTITIAL)) {
			this.mediationInterstitialListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
			return;
		}

		if (!Tapjoy.isConnected()) {
			Hashtable<String, Object> connectFlags = new Hashtable<String, Object>();
			if (networkExtras != null) {
				if (networkExtras.containsKey(TapjoyExtrasBundleBuilder.DEBUG)) {
					connectFlags.put("TJC_OPTION_ENABLE_LOGGING", networkExtras.getBoolean(TapjoyExtrasBundleBuilder.DEBUG, false));
				}
			}

			Log.i(TAG, "Connecting to Tapjoy for Tapjoy-AdMob adapter");
			Tapjoy.connect(context, sdkKey, connectFlags, new TJConnectListener() {

				@Override
				public void onConnectSuccess() {
					// Make an ad request.
					createInterstitialPlacementAndRequestContent();
				}

				@Override
				public void onConnectFailure() {
					Log.d(TAG, "Tapjoy failed to connect");
					mediationInterstitialListener.onAdFailedToLoad(TapjoyAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
				}
			});
		} else {
			if (interstitialPlacement != null && interstitialPlacement.isContentAvailable()) {
				mediationInterstitialListener.onAdLoaded(TapjoyAdapter.this);
			} else {
				// Make an ad request
				createInterstitialPlacementAndRequestContent();
			}
		}

	}

	private boolean checkParams(Context context, Bundle serverParameters, RequestType type) {
		String placementName = null;

		// Check for server parameters
		if ( serverParameters != null) {
			sdkKey = serverParameters.getString(SDK_KEY_SERVER_PARAMETER_KEY);
			placementName = serverParameters.getString(PLACEMENT_NAME_SERVER_PARAMETER_KEY);
		}

		if (sdkKey == null || placementName == null) {
			Log.i(TAG, "Did not receive valid server parameters from AdMob");
			return false;
		}

		if (type.equals(RequestType.VIDEO)) {
			videoPlacementName = placementName;
		} else {
			interstitialPlacementName = placementName;
		}

		if (context != null && context instanceof Activity) {
			Tapjoy.setActivity((Activity) context);
		} else {
			Log.d(TAG, "Tapjoy requires an Activity context to initialize");
			return false;
		}

		return true;
	}

	@Override
	public void loadAd(MediationAdRequest mediationAdRequest, Bundle serverParameters, Bundle networkExtras) {
		Log.i(TAG, "Loading ad for Tapjoy-AdMob adapter");
		String loadAdPlacementName = "";

		if (videoPlacementName == null || videoPlacementName.equals("")) {
			Log.i(TAG, "No placement name given for Tapjoy-AdMob adapter");
			mediationRewardedVideoAdListener.onAdFailedToLoad(TapjoyAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
			return;
		}

		if (serverParameters != null && serverParameters.containsKey(PLACEMENT_NAME_SERVER_PARAMETER_KEY)) {
			loadAdPlacementName = serverParameters.getString(PLACEMENT_NAME_SERVER_PARAMETER_KEY);
		}

		if (!loadAdPlacementName.equals("") && !videoPlacementName.equals(loadAdPlacementName)) {
			// If incoming placement name is different from the one already preloaded, create new placement
			videoPlacementName = loadAdPlacementName;
			createVideoPlacementAndRequestContent();
		} else if (videoPlacement != null && videoPlacement.isContentReady()) {
			// If content is already available from previous request, fire success
			mediationRewardedVideoAdListener.onAdLoaded(TapjoyAdapter.this);
		} else {
			if (!videoPlacementIsRequesting) {
				// If we're not already in the middle of a request, send new placement request
				createVideoPlacementAndRequestContent();
			}
		}
	}

	@Override
	public boolean isInitialized() {
		return isInitialized;
	}

	/**
	 * Tapjoy Callbacks
	 */
	private void createVideoPlacementAndRequestContent() {
		isInitialized = true;

		Log.i(TAG, "Creating video placement for AdMob adapter");
		videoPlacement = Tapjoy.getPlacement(videoPlacementName, new TJPlacementListener() {
			// Placement Callbacks
			@Override
			public void onRequestSuccess(TJPlacement tjPlacement) {
				mainHandler.post(new Runnable() {
					@Override
					public void run() {
						if (!videoPlacement.isContentAvailable()) {
							videoPlacementIsRequesting = false;
							mediationRewardedVideoAdListener.onAdFailedToLoad(TapjoyAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
						}
					}
				});
			}

			@Override
			public void onRequestFailure(TJPlacement tjPlacement, TJError tjError) {
				mainHandler.post(new Runnable() {
					@Override
					public void run() {
						videoPlacementIsRequesting = false;
						mediationRewardedVideoAdListener.onAdFailedToLoad(TapjoyAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
					}
				});
			}

			@Override
			public void onContentReady(TJPlacement tjPlacement) {
				mainHandler.post(new Runnable() {
					@Override
					public void run() {
						videoPlacementIsRequesting = false;
						mediationRewardedVideoAdListener.onAdLoaded(TapjoyAdapter.this);
					}
				});
			}

			@Override
			public void onContentShow(TJPlacement tjPlacement) {
				mainHandler.post(new Runnable() {
					@Override
					public void run() {
						mediationRewardedVideoAdListener.onAdOpened(TapjoyAdapter.this);

					}
				});
			}

			@Override
			public void onContentDismiss(TJPlacement tjPlacement) {
				mainHandler.post(new Runnable() {
					@Override
					public void run() {
						mediationRewardedVideoAdListener.onAdClosed(TapjoyAdapter.this);
						// Load up next video
						requestVideoPlacementContent();
					}
				});
			}

			@Override
			public void onPurchaseRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s) {
				// no-op
			}

			@Override
			public void onRewardRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s, int i) {
				// no-op
			}
		});
		videoPlacement.setMediationName(MEDIATION_AGENT);
		videoPlacement.setAdapterVersion(TAPJOY_INTERNAL_ADAPTER_VERSION);
		videoPlacement.setVideoListener(this);

		requestVideoPlacementContent();
	}

	private void createInterstitialPlacementAndRequestContent() {
		Log.i(TAG, "Creating interstitial placement for AdMob adapter");
		interstitialPlacement = Tapjoy.getPlacement(interstitialPlacementName, new TJPlacementListener() {
			// Placement Callbacks
			@Override
			public void onRequestSuccess(TJPlacement tjPlacement) {
				mainHandler.post(new Runnable() {
					@Override
					public void run() {
						if (!interstitialPlacement.isContentAvailable()) {
							interstitialPlacementIsRequesting = false;
							mediationInterstitialListener.onAdFailedToLoad(TapjoyAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
						}
					}
				});
			}

			@Override
			public void onRequestFailure(TJPlacement tjPlacement, TJError tjError) {
				mainHandler.post(new Runnable() {
					@Override
					public void run() {
						interstitialPlacementIsRequesting = false;
						mediationInterstitialListener.onAdFailedToLoad(TapjoyAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
					}
				});
			}

			@Override
			public void onContentReady(TJPlacement tjPlacement) {
				mainHandler.post(new Runnable() {
					@Override
					public void run() {
						interstitialPlacementIsRequesting = false;
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
			public void onPurchaseRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s) {
				// no-op
			}

			@Override
			public void onRewardRequest(TJPlacement tjPlacement, TJActionRequest tjActionRequest, String s, int i) {
				// no-op
			}
		});
		interstitialPlacement.setMediationName(MEDIATION_AGENT);
		interstitialPlacement.setAdapterVersion(TAPJOY_INTERNAL_ADAPTER_VERSION);

		requestInterstitialPlacementContent();
	}

	private void requestVideoPlacementContent() {
		if (!videoPlacementIsRequesting) {
			videoPlacementIsRequesting = true;
			videoPlacement.requestContent();
		}
	}

	private void requestInterstitialPlacementContent() {
		if (!interstitialPlacementIsRequesting) {
			interstitialPlacementIsRequesting = true;
			interstitialPlacement.requestContent();
		}
	}

	@Override
	public void showVideo() {
		Log.i(TAG, "Show video content for Tapjoy-AdMob adapter");
		if (videoPlacement != null && videoPlacement.isContentAvailable()) {
			videoPlacement.showContent();
		}
	}

	@Override
	public void showInterstitial() {
		Log.i(TAG, "Show interstitial content for Tapjoy-AdMob adapter");
		if (interstitialPlacement != null && interstitialPlacement.isContentAvailable()) {
			interstitialPlacement.showContent();
		}
	}

	@Override
	public void onVideoStart(TJPlacement tjPlacement) {
		mediationRewardedVideoAdListener.onVideoStarted(TapjoyAdapter.this);
	}

	@Override
	public void onVideoError(TJPlacement tjPlacement, String s) {
		mediationRewardedVideoAdListener.onAdFailedToLoad(TapjoyAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
	}

	@Override
	public void onVideoComplete(TJPlacement tjPlacement) {
		mediationRewardedVideoAdListener.onRewarded(this, new TapjoyReward());
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

	public class TapjoyReward implements RewardItem  {
		@Override
		public String getType() {
			// Tapjoy only supports fixed rewards and doesn't provide a reward type.
			return "";
		}

		@Override
		public int getAmount() {
			// Tapjoy only supports fixed rewards and doesn't provide a reward amount.
			return 0;
		}
	}

}