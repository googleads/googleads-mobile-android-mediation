package com.mopub.mobileads.dfp.adapters;

import com.google.ads.mediation.mopub.MoPubMediationAdapter.AdapterError;
import com.mopub.mobileads.MoPubRewardedVideoListener;

public interface MoPubAdapterRewardedListener extends MoPubRewardedVideoListener {

  /**
   * Called when the adapter fails to load an ad for a reason that is different than a MoPub SDK
   * failure callback.
   *
   * @param errorCode    The error code.
   * @param errorMessage A message describing the error.
   */
  void onAdFailedToLoad(@AdapterError int errorCode, String errorMessage);
}
