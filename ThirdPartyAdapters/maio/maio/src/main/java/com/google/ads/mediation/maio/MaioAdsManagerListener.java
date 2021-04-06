package com.google.ads.mediation.maio;

import androidx.annotation.NonNull;

import com.google.ads.mediation.maio.MaioMediationAdapter.AdapterError;

import com.google.android.gms.ads.AdError;
import jp.maio.sdk.android.MaioAdsListenerInterface;

public interface MaioAdsManagerListener extends MaioAdsListenerInterface {

  /**
   * Called when an ad fails to load due to an adapter error.
   */
  void onAdFailedToLoad(@NonNull AdError error);

  /**
   * Called when an ad fails to show due to an adapter error.
   */
  void onAdFailedToShow(@NonNull AdError error);
}
