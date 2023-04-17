package com.google.ads.mediation.inmobi;

import androidx.annotation.NonNull;
import java.util.HashMap;

public final class InMobiExtras {

  private final HashMap<String, String> parameterMap;
  private final String keywords;

  public InMobiExtras(@NonNull HashMap<String, String> parameterMap, @NonNull String keywords) {
    this.parameterMap = parameterMap;
    this.keywords = keywords;
  }

  public HashMap<String, String> getParameterMap() {
    return this.parameterMap;
  }

  public String getKeywords() {
    return this.keywords;
  }
}
