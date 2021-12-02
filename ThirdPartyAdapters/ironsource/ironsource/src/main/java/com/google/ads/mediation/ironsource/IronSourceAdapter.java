package com.google.ads.mediation.ironsource;

import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.DEFAULT_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_APP_KEY;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.KEY_INSTANCE_ID;
import static com.google.ads.mediation.ironsource.IronSourceAdapterUtils.TAG;
import static com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.ads.mediation.ironsource.IronSourceManager.InitializationCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.ironsource.mediationsdk.logger.IronSourceError;

/**
 * A {@link MediationInterstitialAdapter} to load and show IronSource interstitial ads using Google
 * Mobile Ads SDK mediation.
 */
public class IronSourceAdapter implements MediationInterstitialAdapter, IronSourceAdapterListener {

  /**
   * Mediation interstitial ad listener used to forward interstitial events from IronSource SDK to
   * Google Mobile Ads SDK.
   */
  private MediationInterstitialListener mInterstitialListener;

  /**
   * This is the id of the instance to be shown.
   */
  private String mInstanceID;

  // region MediationInterstitialAdapter implementation.
  @Override
  public void requestInterstitialAd(@NonNull Context context,
      @NonNull final MediationInterstitialListener listener,
      @NonNull final Bundle serverParameters, @NonNull MediationAdRequest mediationAdRequest,
      @Nullable Bundle mediationExtras) {

    String appKey = serverParameters.getString(KEY_APP_KEY);
    IronSourceManager.getInstance().initIronSourceSDK(context, appKey,
        new InitializationCallback() {
          @Override
          public void onInitializeSuccess() {
            mInstanceID = serverParameters.getString(KEY_INSTANCE_ID, DEFAULT_INSTANCE_ID);
            mInterstitialListener = listener;
            Log.d(TAG,
                String.format("Loading IronSource interstitial ad with instance ID: %s",
                    mInstanceID));
            IronSourceManager.getInstance().loadInterstitial(mInstanceID, IronSourceAdapter.this);
          }

          @Override
          public void onInitializeError(@NonNull AdError initializationError) {
            Log.e(TAG, initializationError.getMessage());
            listener.onAdFailedToLoad(IronSourceAdapter.this, initializationError);
          }
        });
  }

  @Override
  public void showInterstitial() {
    Log.d(
        TAG,
        String.format("Showing IronSource interstitial ad for instance ID: %s", this.mInstanceID));
    IronSourceManager.getInstance().showInterstitial(mInstanceID);
  }
  // endregion

  @Override
  public void onDestroy() {
  }

  @Override
  public void onPause() {
  }

  @Override
  public void onResume() {
  }

  // region ISDemandOnlyInterstitialListener implementation.
  public void onInterstitialAdReady(String instanceId) {
    Log.d(TAG, String.format("IronSource Interstitial ad loaded for instance ID: %s", instanceId));

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mInterstitialListener != null) {
              mInterstitialListener.onAdLoaded(IronSourceAdapter.this);
            }
          }
        });
  }

  public void onInterstitialAdLoadFailed(String instanceId, final IronSourceError ironSourceError) {
    AdError loadError = new AdError(ironSourceError.getErrorCode(),
        ironSourceError.getErrorMessage(), IRONSOURCE_SDK_ERROR_DOMAIN);
    String errorMessage = String
        .format("IronSource failed to load interstitial ad for instance ID: %s. Error: %s",
            instanceId, loadError.getMessage());
    Log.e(TAG, errorMessage);

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mInterstitialListener != null) {
              mInterstitialListener.onAdFailedToLoad(IronSourceAdapter.this, loadError);
            }
          }
        });
  }

  public void onInterstitialAdOpened(String instanceId) {
    Log.d(TAG, String.format("IronSource Interstitial ad opened for instance ID: %s", instanceId));

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mInterstitialListener != null) {
              mInterstitialListener.onAdOpened(IronSourceAdapter.this);
            }
          }
        });
  }

  public void onInterstitialAdClosed(String instanceId) {
    Log.d(TAG, String.format("IronSource Interstitial ad closed for instance ID: %s", instanceId));

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mInterstitialListener != null) {
              mInterstitialListener.onAdClosed(IronSourceAdapter.this);
            }
          }
        });
  }

  public void onInterstitialAdShowFailed(String instanceId, IronSourceError ironSourceError) {
    AdError showError = new AdError(ironSourceError.getErrorCode(),
        ironSourceError.getErrorMessage(), IRONSOURCE_SDK_ERROR_DOMAIN);
    String errorMessage = String
        .format("IronSource failed to show interstitial ad for instance ID: %s. Error: %s",
            instanceId, showError.getMessage());
    Log.e(TAG, errorMessage);

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mInterstitialListener != null) {
              mInterstitialListener.onAdOpened(IronSourceAdapter.this);
              mInterstitialListener.onAdClosed(IronSourceAdapter.this);
            }
          }
        });
  }

  public void onInterstitialAdClicked(String instanceId) {
    Log.d(TAG, String.format("IronSource Interstitial ad clicked for instance ID: %s", instanceId));

    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mInterstitialListener != null) {
              mInterstitialListener.onAdClicked(IronSourceAdapter.this);
              mInterstitialListener.onAdLeftApplication(IronSourceAdapter.this);
            }
          }
        });
  }
  // endregion

  // region IronSourceAdapterListener implementation.
  @Override
  public void onAdFailedToLoad(@NonNull AdError loadError) {
    Log.e(TAG, loadError.getMessage());
    IronSourceAdapterUtils.sendEventOnUIThread(
        new Runnable() {
          @Override
          public void run() {
            if (mInterstitialListener != null) {
              mInterstitialListener.onAdFailedToLoad(IronSourceAdapter.this, loadError);
            }
          }
        });
  }

  @Override
  public void onAdFailedToShow(@NonNull AdError showError) {
    Log.e(TAG, showError.getMessage());
  }
  // endregion
}
