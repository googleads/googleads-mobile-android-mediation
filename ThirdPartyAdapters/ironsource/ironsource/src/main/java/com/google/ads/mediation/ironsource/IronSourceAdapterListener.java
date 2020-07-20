package com.google.ads.mediation.ironsource;

import androidx.annotation.NonNull;
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.AdapterError;

public interface IronSourceAdapterListener {

  /**
   * Called when the adapter fails to load an ad.
   *
   * @param errorCode The error code.
   * @param errorMessage A message describing the error.
   */
  void onAdFailedToLoad(@AdapterError int errorCode, @NonNull String errorMessage);
}
