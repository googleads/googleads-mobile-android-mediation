package com.google.ads.mediation.inmobi;

import android.view.ViewGroup;
import android.widget.FrameLayout;

/** Container for holding the InMobi ad view. */
public class InMobiAdViewHolder {

  private final FrameLayout frameLayout;

  InMobiAdViewHolder(final FrameLayout frameLayout) {
    this.frameLayout = frameLayout;
  }

  public FrameLayout getFrameLayout() {
    return frameLayout;
  }

  public void setLayoutParams(final ViewGroup.LayoutParams layoutParams) {
    frameLayout.setLayoutParams(layoutParams);
  }

  public void addView(final InMobiBannerWrapper inMobiBannerWrapper) {
    frameLayout.addView(inMobiBannerWrapper.getInMobiBanner());
  }
}
