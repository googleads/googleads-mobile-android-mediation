package com.google.ads.mediation.inmobi;

import android.content.Context;
import android.widget.FrameLayout;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.InMobiNative;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import com.inmobi.ads.listeners.NativeAdEventListener;

/** Class for creating InMobi ad objects. */
public class InMobiAdFactory {
  public InMobiBannerWrapper createInMobiBannerWrapper(
      final Context context, final Long placementId) {
    return new InMobiBannerWrapper(new InMobiBanner(context, placementId));
  }

  public InMobiAdViewHolder createInMobiAdViewHolder(final Context context) {
    return new InMobiAdViewHolder(new FrameLayout(context));
  }

  public InMobiInterstitialWrapper createInMobiInterstitialWrapper(
      final Context context, final Long placementId, final InterstitialAdEventListener listener) {
    return new InMobiInterstitialWrapper(new InMobiInterstitial(context, placementId, listener));
  }

  public InMobiNativeWrapper createInMobiNativeWrapper(
      final Context context, final Long placementId, final NativeAdEventListener listener) {
    return new InMobiNativeWrapper(new InMobiNative(context, placementId, listener));
  }

  public InMobiNativeWrapper createInMobiNativeWrapper(InMobiNative inMobiNative) {
    return new InMobiNativeWrapper(inMobiNative);
  }

}
