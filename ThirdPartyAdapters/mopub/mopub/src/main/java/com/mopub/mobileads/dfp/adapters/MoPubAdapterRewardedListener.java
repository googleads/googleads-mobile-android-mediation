package com.mopub.mobileads.dfp.adapters;

import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.mopub.mobileads.MoPubRewardedVideoListener;

public interface MoPubAdapterRewardedListener extends MoPubRewardedVideoListener {

  /**
   * Called when the adapter fails to load an ad for a reason that is different than a MoPub SDK
   * failure callback.
   *
   * @param loadError The {@link AdError} object.
   */
  void onAdFailedToLoad(@NonNull AdError loadError);
}
