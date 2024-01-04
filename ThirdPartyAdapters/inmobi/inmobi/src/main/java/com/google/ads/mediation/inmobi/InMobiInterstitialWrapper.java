package com.google.ads.mediation.inmobi;

import androidx.annotation.VisibleForTesting;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.WatermarkData;
import java.util.Map;

/** Wrapper around InMobiInterstitial class. */
public class InMobiInterstitialWrapper {

  private final InMobiInterstitial inMobiInterstitial;

  InMobiInterstitialWrapper(InMobiInterstitial inMobiInterstitial) {
    this.inMobiInterstitial = inMobiInterstitial;
  }

  @VisibleForTesting
  public InMobiInterstitial getInMobiInterstitial() {
    return inMobiInterstitial;
  }

  public void setExtras(final Map<String, String> extras) {
    inMobiInterstitial.setExtras(extras);
  }

  public void setKeywords(final String keywords) {
    inMobiInterstitial.setKeywords(keywords);
  }

  public void setWatermarkData(final WatermarkData watermarkData) {
    inMobiInterstitial.setWatermarkData(watermarkData);
  }

  public void load() {
    inMobiInterstitial.load();
  }

  public void load(final byte[] bidToken) {
    inMobiInterstitial.load(bidToken);
  }

  public Boolean isReady() {
    return inMobiInterstitial.isReady();
  }

  public void show() {
    inMobiInterstitial.show();
  }
}
