package com.google.ads.mediation.inmobi;

import android.content.Context;
import android.widget.FrameLayout;
import com.inmobi.ads.InMobiBanner;

/** Class for creating InMobi ad objects. */
public class InMobiAdFactory {
  public InMobiBannerWrapper createInMobiBannerWrapper(
      final Context context, final Long placementId) {
    return new InMobiBannerWrapper(new InMobiBanner(context, placementId));
  }

  public InMobiAdViewHolder createInMobiAdViewHolder(final Context context) {
    return new InMobiAdViewHolder(new FrameLayout(context));
  }

}