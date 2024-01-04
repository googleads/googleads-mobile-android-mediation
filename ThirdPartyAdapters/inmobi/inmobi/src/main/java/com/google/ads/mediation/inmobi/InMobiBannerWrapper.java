package com.google.ads.mediation.inmobi;

import android.view.ViewGroup;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.WatermarkData;
import com.inmobi.ads.listeners.BannerAdEventListener;
import java.util.Map;

/** Wrapper around InMobiBanner class. */
public class InMobiBannerWrapper {
  private final InMobiBanner inMobiBanner;

  InMobiBannerWrapper(InMobiBanner inMobiBanner) {
    this.inMobiBanner = inMobiBanner;
  }

  public InMobiBanner getInMobiBanner() {
    return inMobiBanner;
  }

  public void setEnableAutoRefresh(final Boolean shouldRefresh) {
    inMobiBanner.setEnableAutoRefresh(shouldRefresh);
  }

  public void setAnimationType(final InMobiBanner.AnimationType type) {
    inMobiBanner.setAnimationType(type);
  }

  public void setListener(final BannerAdEventListener listener) {
    inMobiBanner.setListener(listener);
  }

  public void setWatermarkData(final WatermarkData watermarkData) {
    inMobiBanner.setWatermarkData(watermarkData);
  }

  public void setLayoutParams(final ViewGroup.LayoutParams layoutParams) {
    inMobiBanner.setLayoutParams(layoutParams);
  }

  public void setExtras(final Map<String, String> extras) {
    inMobiBanner.setExtras(extras);
  }

  public void setKeywords(final String keywords) {
    inMobiBanner.setKeywords(keywords);
  }

  public void load() {
    inMobiBanner.load();
  }

  public void load(final byte[] bidToken) {
    inMobiBanner.load(bidToken);
  }
}
