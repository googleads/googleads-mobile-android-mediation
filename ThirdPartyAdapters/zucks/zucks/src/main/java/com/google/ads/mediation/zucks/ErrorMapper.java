package com.google.ads.mediation.zucks;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;

import net.zucks.exception.FrameIdNotFoundException;
import net.zucks.exception.NetworkNotFoundException;

public class ErrorMapper {

  private static final String ERROR_ADAPTER_DOMAIN = AdMobUtil.ADAPTER_DOMAIN;
  private static final String ERROR_SDK_DOMAIN = AdMobUtil.SDK_DOMAIN;

  public static final int ERROR_INVALID_REQUEST = AdRequest.ERROR_CODE_INVALID_REQUEST;
  public static final int ERROR_NETWORK_ERROR = AdRequest.ERROR_CODE_NETWORK_ERROR;
  public static final int ERROR_INTERNAL_ERROR = AdRequest.ERROR_CODE_INTERNAL_ERROR;

  @IntDef(
      value = {
        ERROR_INVALID_REQUEST,
        ERROR_NETWORK_ERROR,
        ERROR_INTERNAL_ERROR,
      })
  public @interface AdapterError {}

  /** Convert Zucks Ad Network SDK's exception to AdMob's error instance. */
  @NonNull
  public static AdError convertSdkError(@Nullable Exception e) {
    return new AdError(
        convertSdkErrorCode(e),
        e != null ? e.toString() : "Internal error occurred.",
        ERROR_SDK_DOMAIN);
  }

  /** Convert Zucks Ad Network SDK's exception to integer-based error code. */
  @AdapterError
  public static int convertSdkErrorCode(@Nullable Exception e) {
    if (e instanceof FrameIdNotFoundException) {
      return ERROR_INVALID_REQUEST;
    } else if (e instanceof NetworkNotFoundException) {
      return ERROR_NETWORK_ERROR;
    } else {
      return ERROR_INTERNAL_ERROR;
    }
  }

  /**
   * Create AdMob's error instance from (this) adapter's error instance. Do **NOT** pass the Zucks
   * Ad Network SDK's integer-based error code (e.g. convertSdkErrorCode result).
   */
  @NonNull
  public static AdError createAdapterError(@AdapterError int code, @NonNull String msg) {
    return new AdError(code, msg, ERROR_ADAPTER_DOMAIN);
  }
}
