package com.google.ads.mediation.mintegral;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class MintegralConstants {

  public static final String APP_ID = "app_id";
  public static final String APP_KEY = "app_key";
  public static final String AD_UNIT_ID = "ad_unit_id";
  public static final String PLACEMENT_ID = "placement_id";
  public static final String ERROR_DOMAIN = "com.google.ads.mediation.mintegral";
  public static final String MINTEGRAL_SDK_ERROR_DOMAIN = "com.mbridge.msdk";

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(
      value = {ERROR_INVALID_SERVER_PARAMETERS,
          ERROR_BANNER_SIZE_UNSUPPORTED,
          ERROR_INVALID_BID_RESPONSE,
          ERROR_MINTEGRAL_SDK,
          ERROR_CODE_NO_FILL,
      })
  public @interface AdapterError {

  }

  /**
   * The Mintegral SDK returned a failure callback.
   */
  public static final int ERROR_MINTEGRAL_SDK = 100;

  /**
   * Invalid server parameters (e.g. Missing App ID or Placement ID).
   */
  public static final int ERROR_INVALID_SERVER_PARAMETERS = 101;

  /**
   * The requested ad size does not match a Mintegral supported banner size.
   */
  public static final int ERROR_BANNER_SIZE_UNSUPPORTED = 102;

  /**
   * Missing or invalid bid response.
   */
  public static final int ERROR_INVALID_BID_RESPONSE = 103;

  /**
   * Mintegral SDK returned a no fill error.
   */
  public static final int ERROR_CODE_NO_FILL = 104;

  /**
   * The returned offer has been installed
   */
  public static final int ERROR_CODE_APP_FILTER = 105;

  /**
   *Mintegral SDK tpl render exception
   */
  public static final int ERROR_CODE_H5_TEMPLATE_ERROR = 106;

  /**
   * Mintegral SDK zip or html load failed
   */
  public static final int ERROR_CODE_ZIP_OR_HTML_FAILED = 107;

  /**
   * Mintegral SDK  load 201 template failed
   */
  public static final int ERROR_CODE_LOAD_201_TIMEOUT = 108;

  /**
   * Mintegral SDK load 101 template failed
   */
  public static final int ERROR_CODE_LOAD_101_TIMEOUT = 109;

  /**
   * Mintegral SDK rv zip url error
   */
  public static final int ERROR_CODE_RV_ZIP_ERROR = 110;

  /**
   * Mintegral SDK download mraid file failed
   */
  public static final int ERROR_CODE_MRAID_ERROR = 111;

  /**
   * Mintegral SDK disconnected network exception
   */
  public static final int ERROR_CODE_DISCONNECTED_EXCEPTION = 112;

  /**
   * Mintegral SDK load json error
   */
  public static final int ERROR_CODE_V3_ERROR= 113;

  /**
   * Mintegral SDK server is not filled
   */
  public static final int ERROR_CODE_EXCEPTION_RETURN_EMPTY = 114;

  /**
   * Mintegral SDK network UnknownHostException
   */
  public static final int ERROR_CODE_UNKNOWN_HOST = 115;

  /**
   * Mintegral SDK video url Unable to resolve host
   */
  public static final int ERROR_CODE_VIDEO_UNABLE_RESOLVE_HOST = 116;

  /**
   *  Mintegral SDK network ConnectException
   */
  public static final int ERROR_CODE_CONNECT_EXCEPTION = 117;

  /**
   * Mintegral SDK no space left on device
   */
  public static final int ERROR_CODE_VIDEO_NO_SPACE= 118;

  /**
   * Mintegral SDK download template failed
   */
  public static final int ERROR_CODE_TEMP_DOWNLOAD_FAILED = 119;

  /**
   * Mintegral SDK load error msg APP ALREADY INSTALLED
   */
  public static final String ERROR_MESSAGE_APP_INSTALLED = "APP ALREADY INSTALLED";

  /**
   * Mintegral SDK load error msg Need show campaign list is NULL!
   */
  public static final String ERROR_MESSAGE_SHOW_LIST_IS_NULL = "Need show campaign list is NULL!";

  /**
   * Mintegral SDK load error msg tpl temp preload failed
   */
  public static final String ERROR_MESSAGE_TPL_PRELOAD_FAILED = "tpl temp preload failed";

  /**
   * Mintegral SDK load error msg resource download failed zip/html
   */
  public static final String ERROR_MESSAGE_ZIP_OR_HTML_FAILED = "resource download failed zip/html";

  /**
   * Mintegral SDK load error msg resource load timeout is tpl: true
   */
  public static final String ERROR_MESSAGE_201_LOAD_FAILED = "resource load timeout is tpl: true";

  /**
   * Mintegral SDK load error msg resource load timeout is tpl: false
   */
  public static final String ERROR_MESSAGE_101_LOAD_FAILED = "resource load timeout is tpl: false";

  /**
   * Mintegral SDK load error msg hybird.rayjump.com/rv-zip
   */
  public static final String ERROR_MESSAGE_RV_ZIP_ERROR = "hybird.rayjump.com/rv-zip";

  /**
   * Mintegral SDK load error msg mraid resource write fail
   */
  public static final String ERROR_MESSAGE_MRAID_ERROR = "mraid resource write fail";

  /**
   * Mintegral SDK load error msg Network error,disconnected network exception
   */
  public static final String ERROR_MESSAGE_DISCONNECTED_EXCEPTION = "Network error,disconnected network exception";

  /**
   * Mintegral SDK load error msg v3 is timeout
   */
  public static final String ERROR_MESSAGE_V3_ERROR = "v3 is timeout";

  /**
   * Mintegral SDK load error msg EXCEPTION_RETURN_EMPTY
   */
  public static final String ERROR_MESSAGE_EXCEPTION_RETURN_EMPTY = "EXCEPTION_RETURN_EMPTY";

  /**
   * Mintegral SDK load error msg Network error,UnknownHostException
   */
  public static final String ERROR_MESSAGE_UNKNOWN_HOST = "Network error,UnknownHostException";

  /**
   * Mintegral SDK load error msg resource download failed video Unable to resolve host
   */
  public static final String ERROR_MESSAGE_VIDEO_UNABLE_RESOLVE_HOST = "resource download failed video Unable to resolve host";

  /**
   * Mintegral SDK load error msg Network error,ConnectException
   */
  public static final String ERROR_MESSAGE_CONNECT_EXCEPTION = "Network error,ConnectException";

  /**
   * Mintegral SDK load error msg No space left on device
   */
  public static final String ERROR_MESSAGE_VIDEO_NO_SPACE = "No space left on device";

  /**
   * Mintegral SDK load error msg resource download failed temp
   */
  public static final String ERROR_MESSAGE_TEMP_DOWNLOAD_FAILED = "resource download failed temp";



  @NonNull
  public static AdError createAdapterError(@AdapterError int errorCode,
      @NonNull String errorMessage) {
    return new AdError(errorCode, errorMessage, ERROR_DOMAIN);
  }

  @NonNull
  public static AdError createSdkError(@NonNull String errorMessage) {
    int code = MintegralUtils.getCodeByMsg(errorMessage);
    return new AdError(code, errorMessage, MINTEGRAL_SDK_ERROR_DOMAIN);
  }
}
