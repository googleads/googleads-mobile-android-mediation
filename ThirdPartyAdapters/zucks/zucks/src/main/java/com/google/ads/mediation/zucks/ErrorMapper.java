package com.google.ads.mediation.zucks;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;

import net.zucks.exception.FrameIdNotFoundException;

public class ErrorMapper {

  /** Adapter error domain */
  private static final String ERROR_ADAPTER_DOMAIN = BuildConfig.LIBRARY_PACKAGE_NAME;

  /** Zucks Ad Network SDK error domain */
  private static final String ERROR_SDK_DOMAIN = net.zucks.BuildConfig.LIBRARY_PACKAGE_NAME;

  /**
   * Server parameters, such as Frame ID, are invalid.
   * Adapter's error code(s) is always greater than 100.
   *
   * @see <a
   *     href="https://github.com/googleads/googleads-mobile-android-mediation/pull/337#discussion_r653153767">googleads/googleads-mobile-android-mediation
   *     #337</a>
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a Zucks supported banner size.
   * Adapter's error code(s) is always greater than 100.
   *
   * @see <a
   *     href="https://github.com/googleads/googleads-mobile-android-mediation/pull/337#discussion_r653153767">googleads/googleads-mobile-android-mediation
   *     #337</a>
   */
  public static final int ERROR_BANNER_SIZE_MISMATCH = 102;

  /**
   * Invalid parameter type.
   * Adapter's error code(s) is always greater than 100.
   *
   * @see <a
   *     href="https://github.com/googleads/googleads-mobile-android-mediation/pull/337#discussion_r653153767">googleads/googleads-mobile-android-mediation
   *     #337</a>
   */
  public static final int ERROR_CONTEXT_NOT_ACTIVITY = 103;

  /**
   * Other internal errors.
   * Adapter's error code(s) is always greater than 100.
   *
   * @see <a
   *     href="https://github.com/googleads/googleads-mobile-android-mediation/pull/337#discussion_r653153767">googleads/googleads-mobile-android-mediation
   *     #337</a>
   */
  public static final int ERROR_INTERNAL = 104;

  @IntDef(
      value = {
        ERROR_INVALID_SERVER_PARAMETERS,
        ERROR_BANNER_SIZE_MISMATCH,
        ERROR_CONTEXT_NOT_ACTIVITY,
        ERROR_INTERNAL,
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
      return ERROR_INVALID_SERVER_PARAMETERS;
    }
    return ERROR_INTERNAL;
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
