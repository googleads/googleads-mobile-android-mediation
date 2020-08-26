package com.google.ads.mediation.nend;

import static com.google.ads.mediation.nend.NendMediationAdapter.MEDIATION_NAME_ADMOB;
import static com.google.ads.mediation.nend.NendMediationAdapter.TAG;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.VideoOptions;
import com.google.android.gms.ads.mediation.NativeMediationAdRequest;
import net.nend.android.NendAdNativeVideo;
import net.nend.android.NendAdNativeVideoLoader;

class NativeVideoAdLoader {

  private NendNativeAdForwarder forwarder;

  private NendAdNativeVideoLoader videoAdLoader;
  private NendAdNativeVideoLoader.Callback videoLoaderCallback =
      new NendAdNativeVideoLoader.Callback() {
        @Override
        public void onSuccess(NendAdNativeVideo nendAdNativeVideo) {
          Context context = forwarder.getContextFromWeakReference();
          if (context == null) {
            Log.e(TAG, "Your context may be released...");
            forwarder.failedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
          } else {
            NendUnifiedNativeVideoAdMapper videoAdMapper =
                new NendUnifiedNativeVideoAdMapper(context, forwarder, nendAdNativeVideo);
            forwarder.setUnifiedNativeAdMapper(videoAdMapper);
            forwarder.adLoaded();
          }
        }

        @Override
        public void onFailure(int nendErrorCode) {
          forwarder.setUnifiedNativeAdMapper(null);
          forwarder.failedToLoad(nendErrorCode);
        }
      };

  NativeVideoAdLoader(
      NendNativeAdForwarder forwarder,
      AdUnitMapper mapper,
      NativeMediationAdRequest nativeMediationAdRequest,
      Bundle mediationExtras) {
    Context context = forwarder.getContextFromWeakReference();
    if (context == null) {
      Log.e(TAG, "Your context may be released...");
      forwarder.failedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
      return;
    }
    this.forwarder = forwarder;

    NendAdNativeVideo.VideoClickOption clickOption = NendAdNativeVideo.VideoClickOption.LP;
    VideoOptions nativeVideoOptions =
        nativeMediationAdRequest.getNativeAdOptions().getVideoOptions();
    if (nativeVideoOptions != null && nativeVideoOptions.getClickToExpandRequested()) {
      clickOption = NendAdNativeVideo.VideoClickOption.FullScreen;
    }
    videoAdLoader = new NendAdNativeVideoLoader(context, mapper.spotId, mapper.apiKey, clickOption);
    videoAdLoader.setMediationName(MEDIATION_NAME_ADMOB);
    if (mediationExtras != null) {
      videoAdLoader.setUserId(mediationExtras.getString(NendMediationAdapter.KEY_USER_ID, ""));
    }
  }

  void loadAd() {
    videoAdLoader.loadAd(videoLoaderCallback);
  }

  void releaseLoader() {
    videoAdLoader.releaseLoader();
  }
}
