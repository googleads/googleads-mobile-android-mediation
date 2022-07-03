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

  private static final HashMap<String, MaioAdsManager> mInstances = new HashMap<>();
  private ArrayList<InitializationListener> mInitListeners = new ArrayList<>();

  private MaioAdsInstance mMaioInstance;
  private String mMediaID;

  private enum InitializationStatus {
    UNINITIALIZED,
    INITIALIZING,
    INITIALIZED
  }

  private InitializationStatus mInitState;

  private HashMap<String, WeakReference<MaioAdsManagerListener>> mListeners;

  public static final String KEY_MEDIA_ID = "mediaId";
  public static final String KEY_ZONE_ID = "zoneId";

  public static MaioAdsManager getManager(@NonNull String mediaID) {
    if (!mInstances.containsKey(mediaID)) {
      mInstances.put(mediaID, new MaioAdsManager(mediaID));
    }
    return mInstances.get(mediaID);
  }

  private MaioAdsManager(String mediaID) {
    this.mMediaID = mediaID;
    this.mListeners = new HashMap<>();
    this.mInitState = InitializationStatus.UNINITIALIZED;
  }

  public void initialize(Activity activity, InitializationListener listener) {
    if (mInitState == InitializationStatus.INITIALIZED) {
      listener.onMaioInitialized();
      return;
    }

    mInitListeners.add(listener);
    if (mInitState != InitializationStatus.INITIALIZING) {
      mInitState = InitializationStatus.INITIALIZING;

      this.mMaioInstance =
          MaioAds.initWithNonDefaultMediaId(activity, this.mMediaID, MaioAdsManager.this);
    }
  }

  private boolean hasListener(String zoneID) {
    return !TextUtils.isEmpty(zoneID)
        && this.mListeners.containsKey(zoneID)
        && this.mListeners.get(zoneID).get() != null;
  }

  private boolean canShowAd(String zoneID) {
    return !TextUtils.isEmpty(zoneID)
        && this.mMaioInstance != null
        && this.mMaioInstance.canShow(zoneID);
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
    mListeners.put(zoneID, new WeakReference<>(listener));
    listener.onChangedCanShow(zoneID, true);
  }

  public void showAd(String zoneID, MaioAdsManagerListener listener) {
    if (!canShowAd(zoneID)) {
      this.mListeners.remove(zoneID);
      AdError error = new AdError(ERROR_AD_NOT_AVAILABLE,
          "Failed to show ad: Ad not ready for zone ID: " + zoneID, ERROR_DOMAIN);
      Log.w(TAG, error.getMessage());
      listener.onAdFailedToShow(error);
      return;
    }

    this.mMaioInstance.show(zoneID);
  }

  // region MaioAdsListenerInterface implementation
  @Override
  public void onInitialized() {
    mInitState = InitializationStatus.INITIALIZED;

    for (InitializationListener listener : mInitListeners) {
      listener.onMaioInitialized();
    }
    mInitListeners.clear();
  }

  @Override
  public void onChangedCanShow(String zoneId, boolean isAvailable) {
    if (hasListener(zoneId)) {
      this.mListeners.get(zoneId).get().onChangedCanShow(zoneId, isAvailable);
    }
  }

  @Override
  public void onFailed(FailNotificationReason reason, String zoneId) {
    if (hasListener(zoneId)) {
      this.mListeners.get(zoneId).get().onFailed(reason, zoneId);
    }
    this.mListeners.remove(zoneId);
  }

  @Override
  public void onOpenAd(String zoneId) {
    if (hasListener(zoneId)) {
      this.mListeners.get(zoneId).get().onOpenAd(zoneId);
    }
  }

  @Override
  public void onStartedAd(String zoneId) {
    if (hasListener(zoneId)) {
      this.mListeners.get(zoneId).get().onStartedAd(zoneId);
    }
  }

  @Override
  public void onClickedAd(String zoneId) {
    if (hasListener(zoneId)) {
      this.mListeners.get(zoneId).get().onClickedAd(zoneId);
    }
  }

  @Override
  public void onFinishedAd(int playtime, boolean skipped, int duration, String zoneId) {
    if (hasListener(zoneId)) {
      this.mListeners.get(zoneId).get().onFinishedAd(playtime, skipped, duration, zoneId);
    }
  }

  @Override
  public void onClosedAd(String zoneId) {
    if (hasListener(zoneId)) {
      this.mListeners.get(zoneId).get().onClosedAd(zoneId);
    }
    this.mListeners.remove(zoneId);
  }
  // endregion

  public interface InitializationListener {

    /**
     * Called when Maio SDK successfully initializes.
     */
    void onMaioInitialized();
  }
}
