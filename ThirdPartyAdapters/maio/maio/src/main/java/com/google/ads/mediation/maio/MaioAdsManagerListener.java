package com.google.ads.mediation.maio;

import androidx.annotation.NonNull;
import com.google.ads.mediation.maio.MaioMediationAdapter.AdapterError;
import jp.maio.sdk.android.MaioAdsListenerInterface;

public interface MaioAdsManagerListener extends MaioAdsListenerInterface {

  /**
   * Called when an ad fails to load due to an adapter error.
   * @param code the error code.
   * @param errorMessage the error message.
   */
  void onAdFailedToLoad(@AdapterError int code, @NonNull String errorMessage);
}
