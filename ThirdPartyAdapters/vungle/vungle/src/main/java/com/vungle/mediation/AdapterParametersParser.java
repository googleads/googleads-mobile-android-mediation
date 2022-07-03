package com.vungle.mediation;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The {@link AdapterParametersParser} class helps in creating a Vungle network-specific
 * parameters.
 */
public class AdapterParametersParser {

  private static final String TAG = VungleManager.class.getSimpleName();

  public static class Config {

    private String appId;
    private String requestUniqueId;

    public String getAppId() {
      return appId;
    }

    public String getRequestUniqueId() {
      return requestUniqueId;
    }
  }

  @NonNull
  public static Config parse(@NonNull String appId, @Nullable Bundle networkExtras) {
    String uuid = null;
    if (networkExtras != null && networkExtras.containsKey(VungleExtrasBuilder.UUID_KEY)) {
      uuid = networkExtras.getString(VungleExtrasBuilder.UUID_KEY);
    }

    Config ret = new Config();
    ret.appId = appId;
    ret.requestUniqueId = uuid;
    return ret;
  }
}
