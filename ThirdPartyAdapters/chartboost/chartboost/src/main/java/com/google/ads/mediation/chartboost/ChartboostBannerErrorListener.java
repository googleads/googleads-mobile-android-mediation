package com.google.ads.mediation.chartboost;

/**
 * The {@link ChartboostBannerErrorListener} custom interface to notify {@link ChartboostAdapter}
 * that banner init caused an error
 */
public interface ChartboostBannerErrorListener {

  void notifyBannerInitializationError(int error);
}