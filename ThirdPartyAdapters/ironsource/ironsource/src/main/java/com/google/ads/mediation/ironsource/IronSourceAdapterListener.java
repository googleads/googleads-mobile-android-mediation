package com.google.ads.mediation.ironsource;

import androidx.annotation.NonNull;
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.AdapterError;
import com.google.android.gms.ads.AdError;

public interface IronSourceAdapterListener {

  /**
   * Called when the adapter fails to load an ad.
   *
   * @param loadError the {@link AdError} object.
   */
  void onAdFailedToLoad(@NonNull AdError loadError);

  /**
   * Called when the adapter fails to show an ad.
   *
   * @param showError the {@link AdError} object.
   */
  void onAdFailedToShow(@NonNull AdError showError);
}
