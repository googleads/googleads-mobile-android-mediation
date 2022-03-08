package com.google.ads.mediation.zucks;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;

import net.zucks.exception.FrameIdNotFoundException;
import net.zucks.exception.NetworkNotFoundException;

public class ErrorMapper {

  /** Get (this) adapter's package name. */
  private static final String ERROR_ADAPTER_DOMAIN = BuildConfig.LIBRARY_PACKAGE_NAME;

  /** Get Zucks Ad Network SDK's package name (from its module). */
  private static final String ERROR_SDK_DOMAIN = net.zucks.BuildConfig.LIBRARY_PACKAGE_NAME;

  /**
   * Adapter's error code(s) is always greater than 100.
   *
   * @see <a
   *     href="https://github.com/googleads/googleads-mobile-android-mediation/pull/337#discussion_r653153767">googleads/googleads-mobile-android-mediation
   *     #337</a>
   */
  private static final int ADAPTER_ERROR_BASE = 100;

  public static final int ADAPTER_ERROR_INVALID_REQUEST = ADAPTER_ERROR_BASE + 1;
  public static final int ADAPTER_ERROR_ILLEGAL_STATE = ADAPTER_ERROR_BASE + 2;

  @IntDef(
      value = {
        ADAPTER_ERROR_INVALID_REQUEST,
        ADAPTER_ERROR_ILLEGAL_STATE,
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
      return ADAPTER_ERROR_INVALID_REQUEST;
    } else {
      return ADAPTER_ERROR_ILLEGAL_STATE;
    }
  }

  /**
   * Create AdMob's error instance from (this) adapter's error instance. Do **NOT** pass the Zucks
   * Ad Network SDK's integer-based error code (e.g. convertSdkErrorCode result).
   */
  @NonNull
  public static AdError createAdapterError(@AdapterError int errorCode, @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, ERROR_ADAPTER_DOMAIN);
  }
}
