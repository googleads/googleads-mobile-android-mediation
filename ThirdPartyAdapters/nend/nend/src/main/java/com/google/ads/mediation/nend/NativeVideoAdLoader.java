package com.google.ads.mediation.nend;

import static com.google.ads.mediation.nend.NendMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.nend.NendMediationAdapter.ERROR_NULL_CONTEXT;
import static com.google.ads.mediation.nend.NendMediationAdapter.MEDIATION_NAME_ADMOB;
import static com.google.ads.mediation.nend.NendMediationAdapter.NEND_SDK_ERROR_DOMAIN;
import static com.google.ads.mediation.nend.NendMediationAdapter.TAG;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import net.nend.android.NendAdNativeVideo;
import net.nend.android.NendAdNativeVideoLoader;

class NativeVideoAdLoader {

  private NendNativeAdForwarder forwarder;

  private NendAdNativeVideoLoader videoAdLoader;
  private final NendAdNativeVideoLoader.Callback videoLoaderCallback =
      new NendAdNativeVideoLoader.Callback() {
        @Override
        public void onSuccess(NendAdNativeVideo nendAdNativeVideo) {
          Context context = forwarder.getContextFromWeakReference();
          if (context == null) {
            AdError error = new AdError(ERROR_NULL_CONTEXT, "The context object is null.",
                ERROR_DOMAIN);
            Log.e(TAG, error.getMessage());
            forwarder.failedToLoad(error);
            return;
          }

          NendUnifiedNativeVideoAdMapper videoAdMapper =
              new NendUnifiedNativeVideoAdMapper(context, forwarder, nendAdNativeVideo);
          forwarder.setUnifiedNativeAdMapper(videoAdMapper);
          forwarder.adLoaded();
        }

        @Override
        public void onFailure(int nendErrorCode) {
          forwarder.setUnifiedNativeAdMapper(null);

          String errorMessage = String
              .format("Nend SDK returned an ad load failure callback with code: %d", nendErrorCode);
          AdError error = new AdError(nendErrorCode, errorMessage, NEND_SDK_ERROR_DOMAIN);
          Log.e(TAG, error.getMessage());
          forwarder.failedToLoad(error);
        }
      };

  NativeVideoAdLoader(@NonNull NendNativeAdForwarder forwarder, @NonNull int spotID,
      @NonNull String apiKey, @NonNull NativeMediationAdRequest nativeMediationAdRequest,
      @NonNull String userID) {

    Context context = forwarder.getContextFromWeakReference();
    if (context == null) {
      AdError error = new AdError(ERROR_NULL_CONTEXT, "The context object is null.", ERROR_DOMAIN);
      Log.e(TAG, error.getMessage());
      forwarder.failedToLoad(error);
      return;
    }

    this.forwarder = forwarder;

    NendAdNativeVideo.VideoClickOption clickOption = NendAdNativeVideo.VideoClickOption.LP;
    VideoOptions nativeVideoOptions =
        nativeMediationAdRequest.getNativeAdOptions().getVideoOptions();
    if (nativeVideoOptions != null && nativeVideoOptions.getClickToExpandRequested()) {
      clickOption = NendAdNativeVideo.VideoClickOption.FullScreen;
    }
    videoAdLoader = new NendAdNativeVideoLoader(context, spotID, apiKey, clickOption);
    videoAdLoader.setMediationName(MEDIATION_NAME_ADMOB);
    videoAdLoader.setUserId(userID);
  }

  void loadAd() {
    videoAdLoader.loadAd(videoLoaderCallback);
  }

  void releaseLoader() {
    videoAdLoader.releaseLoader();
  }
}
