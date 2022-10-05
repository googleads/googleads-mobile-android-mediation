package jp.maio.sdk.android.mediation.admob.adapter;

import static com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_AD_NOT_AVAILABLE;
import static com.google.ads.mediation.maio.MaioMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.maio.MaioMediationAdapter.TAG;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.ads.mediation.maio.MaioAdsManagerListener;
import com.google.android.gms.ads.AdError;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import jp.maio.sdk.android.FailNotificationReason;
import jp.maio.sdk.android.MaioAds;
import jp.maio.sdk.android.MaioAdsInstance;
import jp.maio.sdk.android.MaioAdsListenerInterface;

/**
 * Used to handle multiple {@link MaioAdsInstance} objects and forward maio Rewarded and
 * Interstitial events to the Google Mobile Ads SDK.
 */
public class MaioAdsManager implements MaioAdsListenerInterface {

  private static final HashMap<String, MaioAdsManager> instances = new HashMap<>();
  private final ArrayList<InitializationListener> initListeners = new ArrayList<>();

  private MaioAdsInstance maioInstance;
  private final String mediaID;

  private enum InitializationStatus {
    UNINITIALIZED,
    INITIALIZING,
    INITIALIZED
  }

  private InitializationStatus initState;

  private final HashMap<String, WeakReference<MaioAdsManagerListener>> listeners;

  public static final String KEY_MEDIA_ID = "mediaId";
  public static final String KEY_ZONE_ID = "zoneId";

  @NonNull
  public static MaioAdsManager getManager(@NonNull String mediaID) {
    if (!instances.containsKey(mediaID)) {
      instances.put(mediaID, new MaioAdsManager(mediaID));
    }
    return instances.get(mediaID);
  }

  private MaioAdsManager(String mediaID) {
    this.mediaID = mediaID;
    this.listeners = new HashMap<>();
    this.initState = InitializationStatus.UNINITIALIZED;
  }

  public void initialize(Activity activity, InitializationListener listener) {
    if (initState == InitializationStatus.INITIALIZED) {
      listener.onMaioInitialized();
      return;
    }

    initListeners.add(listener);
    if (initState != InitializationStatus.INITIALIZING) {
      initState = InitializationStatus.INITIALIZING;

      this.maioInstance =
          MaioAds.initWithNonDefaultMediaId(activity, this.mediaID, MaioAdsManager.this);
    }
  }

  private boolean hasListener(String zoneID) {
    return !TextUtils.isEmpty(zoneID)
        && this.listeners.containsKey(zoneID)
        && this.listeners.get(zoneID).get() != null;
  }

  private boolean canShowAd(String zoneID) {
    return !TextUtils.isEmpty(zoneID)
        && this.maioInstance != null
        && this.maioInstance.canShow(zoneID);
  }

  public void loadAd(String zoneID, MaioAdsManagerListener listener) {
    if (hasListener(zoneID)) {
      Log.e(TAG, "An ad has already been requested for zone ID: " + zoneID);
      listener.onFailed(FailNotificationReason.AD_STOCK_OUT, zoneID);
      return;
    }

    Log.d(TAG, "Requesting ad from zone ID: " + zoneID);
    // If maio does not have an ad ready to be shown, then we fail the ad request to avoid timeouts.
    if (!canShowAd(zoneID)) {
      AdError error = new AdError(ERROR_AD_NOT_AVAILABLE,
          "No ad available for zone id: " + zoneID, ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToLoad(error);
      return;
    }
    listeners.put(zoneID, new WeakReference<>(listener));
    listener.onChangedCanShow(zoneID, true);
  }

  public void showAd(String zoneID, MaioAdsManagerListener listener) {
    if (!canShowAd(zoneID)) {
      this.listeners.remove(zoneID);
      AdError error = new AdError(ERROR_AD_NOT_AVAILABLE,
          "Failed to show ad: Ad not ready for zone ID: " + zoneID, ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToShow(error);
      return;
    }

    this.maioInstance.show(zoneID);
  }

  // region MaioAdsListenerInterface implementation
  @Override
  public void onInitialized() {
    initState = InitializationStatus.INITIALIZED;

    for (InitializationListener listener : initListeners) {
      listener.onMaioInitialized();
    }
    initListeners.clear();
  }

  @Override
  public void onChangedCanShow(String zoneId, boolean isAvailable) {
    if (hasListener(zoneId)) {
      this.listeners.get(zoneId).get().onChangedCanShow(zoneId, isAvailable);
    }
  }

  @Override
  public void onFailed(FailNotificationReason reason, String zoneId) {
    if (hasListener(zoneId)) {
      this.listeners.get(zoneId).get().onFailed(reason, zoneId);
    }
    this.listeners.remove(zoneId);
  }

  @Override
  public void onOpenAd(String zoneId) {
    if (hasListener(zoneId)) {
      this.listeners.get(zoneId).get().onOpenAd(zoneId);
    }
  }

  @Override
  public void onStartedAd(String zoneId) {
    if (hasListener(zoneId)) {
      this.listeners.get(zoneId).get().onStartedAd(zoneId);
    }
  }

  @Override
  public void onClickedAd(String zoneId) {
    if (hasListener(zoneId)) {
      this.listeners.get(zoneId).get().onClickedAd(zoneId);
    }
  }

  @Override
  public void onFinishedAd(int playtime, boolean skipped, int duration, String zoneId) {
    if (hasListener(zoneId)) {
      this.listeners.get(zoneId).get().onFinishedAd(playtime, skipped, duration, zoneId);
    }
  }

  @Override
  public void onClosedAd(String zoneId) {
    if (hasListener(zoneId)) {
      this.listeners.get(zoneId).get().onClosedAd(zoneId);
    }
    this.listeners.remove(zoneId);
  }
  // endregion

  public interface InitializationListener {

    /**
     * Called when Maio SDK successfully initializes.
     */
    void onMaioInitialized();
  }
}
