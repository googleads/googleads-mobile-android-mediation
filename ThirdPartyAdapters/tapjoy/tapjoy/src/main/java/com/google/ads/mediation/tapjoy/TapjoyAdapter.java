package com.google.ads.mediation.tapjoy;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
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

public class TapjoyAdapter implements MediationRewardedVideoAdAdapter, TJPlacementListener, TJConnectListener, TJPlacementVideoListener {

	private static final String TAG = TapjoyAdapter.class.getSimpleName();
	private  static final String SDK_KEY_SERVER_PARAMETER_KEY = "sdkKey";
	private static final String PLACEMENT_NAME_SERVER_PARAMETER_KEY = "placementName";
	private static final String MEDIATION_AGENT = "admob";
	private static final String ADAPTER_VERSION = "1.0.0";

	private final Handler mainHandler = new Handler(Looper.getMainLooper());

	/** Represents a {@link TJPlacement}. */
	private TJPlacement tapjoyPlacement;
	private MediationRewardedVideoAdListener mediationRewardedVideoAdListener;

	private String sdkKey = null;
	private String placementName = null;

	private boolean isInitialized;
	private boolean isRequesting = false;

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

		// Check for server parameters
		if ( serverParameters != null) {
			sdkKey = serverParameters.getString(SDK_KEY_SERVER_PARAMETER_KEY);
			placementName = serverParameters.getString(PLACEMENT_NAME_SERVER_PARAMETER_KEY);
		}

		if (sdkKey == null || placementName == null) {
			Log.i(TAG, "Did not receive valid server parameters from AdMob");
			this.mediationRewardedVideoAdListener.onInitializationFailed(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
			return;
		}

		if (context != null && context instanceof Activity) {
			Tapjoy.setActivity((Activity) context);
		} else {
			Log.d(TAG, "Tapjoy requires an Activity context to initialize");
			this.mediationRewardedVideoAdListener.onInitializationFailed(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
		}

		Log.i(TAG, "Connecting to Tapjoy for Tapjoy-AdMob adapter");
		if (!Tapjoy.isConnected()) {
			Hashtable<String, Object> connectFlags = new Hashtable<String, Object>();
			if (networkExtras != null) {
				if (networkExtras.containsKey(TapjoyExtrasBundleBuilder.DEBUG)) {
					connectFlags.put("TJC_OPTION_ENABLE_LOGGING", networkExtras.getBoolean(TapjoyExtrasBundleBuilder.DEBUG, false));
				}
			}
			Tapjoy.connect(context, sdkKey, connectFlags, this);
		} else {
			mediationRewardedVideoAdListener.onInitializationSucceeded(this);
		}
	}

	@Override
	public void loadAd(MediationAdRequest mediationAdRequest, Bundle serverParameters, Bundle networkExtras) {
		Log.i(TAG, "Loading ad for Tapjoy-AdMob adapter");
		String loadAdPlacementName = "";

		if (placementName == null || placementName.equals("")) {
			Log.i(TAG, "No placement name given for Tapjoy-AdMob adapter");
			mediationRewardedVideoAdListener.onAdFailedToLoad(TapjoyAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
			return;
		}

		if (serverParameters != null && serverParameters.containsKey(PLACEMENT_NAME_SERVER_PARAMETER_KEY)) {
			loadAdPlacementName = serverParameters.getString(PLACEMENT_NAME_SERVER_PARAMETER_KEY);
		}

		if (!loadAdPlacementName.equals("") && !placementName.equals(loadAdPlacementName)) {
			// If incoming placement name is different from the one already preloaded, create new placement
			placementName = loadAdPlacementName;
			createPlacementAndRequestContent();
		} else if (tapjoyPlacement != null && tapjoyPlacement.isContentReady()) {
			// If content is already available from previous request, fire success
			mediationRewardedVideoAdListener.onAdLoaded(TapjoyAdapter.this);
		} else {
			if (!isRequesting) {
				// If we're not already in the middle of a request, send new placement request
				createPlacementAndRequestContent();
			}
		}
	}

	@Override
	public void showVideo() {
		Log.i(TAG, "Show content for Tapjoy-AdMob adapter");
		if (tapjoyPlacement != null && tapjoyPlacement.isContentAvailable()) {
			tapjoyPlacement.showContent();
		}
	}

	@Override
	public boolean isInitialized() {
		return isInitialized;
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
	 * Tapjoy Callbacks
	 */

	// Connect Callbacks
	@Override
	public void onConnectSuccess() {
		mediationRewardedVideoAdListener.onInitializationSucceeded(this);
	}

	@Override
	public void onConnectFailure() {
		Log.d(TAG, "Tapjoy failed to connect");
		this.mediationRewardedVideoAdListener.onInitializationFailed(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
	}

	// Placement Callbacks
	@Override
	public void onRequestSuccess(TJPlacement tjPlacement) {
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (!tapjoyPlacement.isContentAvailable()) {
					isRequesting = false;
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
				isRequesting = false;
				mediationRewardedVideoAdListener.onAdFailedToLoad(TapjoyAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
			}
		});
	}

	@Override
	public void onContentReady(TJPlacement tjPlacement) {
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				isRequesting = false;
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
				requestPlacementContent();
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

	private void createPlacementAndRequestContent() {
		isInitialized = true;
		Log.i(TAG, "Creating placement for AdMob adapter");
		tapjoyPlacement = Tapjoy.getPlacement(placementName, this);
		tapjoyPlacement.setMediationName(MEDIATION_AGENT);
		tapjoyPlacement.setAdapterVersion(ADAPTER_VERSION);
		tapjoyPlacement.setVideoListener(this);
		requestPlacementContent();
	}

	private void requestPlacementContent() {
		isRequesting = true;
		tapjoyPlacement.requestContent();
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
			// // Tapjoy only supports fixed rewards and doesn't provide a reward amount.
			return 0;
		}
	}

}
