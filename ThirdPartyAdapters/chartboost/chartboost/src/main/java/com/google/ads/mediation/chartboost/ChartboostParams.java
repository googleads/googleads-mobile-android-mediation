package com.google.ads.mediation.chartboost;

import static com.google.ads.mediation.chartboost.ChartboostAdapterUtils.LOCATION_DEFAULT;

/**
 * The {@link ChartboostParams} class is used to send network parameters and mediation/network
 * extras from {@link ChartboostMediationAdapter}
 */
public class ChartboostParams {

  /**
   * Chartboost App ID.
   */
  private String appId;

  /**
   * Chartboost App Signature.
   */
  private String appSignature;

  /**
   * Charboost location used to load ads.
   */
  private String cbLocation;

  /**
   * Default constructor, sets a default value for {@link #cbLocation}.
   */
  public ChartboostParams() {
    this.cbLocation = LOCATION_DEFAULT;
  }

  /**
   * @return {@link #appId}.
   */
  public String getAppId() {
    return appId;
  }

  /**
   * @param appId set to {@link #appId}.
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * @return {@link #appSignature}.
   */
  public String getAppSignature() {
    return appSignature;
  }

  /**
   * @param appSignature set to {@link #appSignature}
   */
  public void setAppSignature(String appSignature) {
    this.appSignature = appSignature;
  }

  /**
   * @return {@link #cbLocation}.
   */
  public String getLocation() {
    return cbLocation;
  }

  /**
   * @param location set to {@link #cbLocation}.
   */
  public void setLocation(String location) {
    this.cbLocation = location;
  }
}
