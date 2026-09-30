package com.google.ads.mediation.inmobi;

import androidx.annotation.NonNull;
import java.util.HashMap;

public final class InMobiExtras {

  private final HashMap<String, String> parameterMap;
  private final String keywords;
  private final Boolean isMuted;

  public InMobiExtras(@NonNull HashMap<String, String> parameterMap, @NonNull String keywords, @NonNull Boolean isMuted) {
    this.parameterMap = parameterMap;
    this.keywords = keywords;
    this.isMuted = isMuted;
  }

  public HashMap<String, String> getParameterMap() {
    return this.parameterMap;
  }

  public String getKeywords() {
    return this.keywords;
  }

  public Boolean getIsMuted() { return this.isMuted; }
}
