package com.google.ads.mediation.inmobi;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.inmobi.ads.InMobiNative;
import com.inmobi.ads.listeners.VideoEventListener;
import java.util.Map;
import org.json.JSONObject;

/**
 * Wrapper around InMobiNative class since InMobiNative is a final class so cannot be mocked and the
 * wrapper would enable us to test the methods on the InMobiNative object created in the loadAd()
 * method.
 */
public class InMobiNativeWrapper {

  private final InMobiNative inMobiNative;

  InMobiNativeWrapper(InMobiNative inMobiNative) {
    this.inMobiNative = inMobiNative;
  }

  public InMobiNative getInMobiNative() {
    return inMobiNative;
  }

  public void setVideoEventListener(VideoEventListener listener) {
    inMobiNative.setVideoEventListener(listener);
  }

  public void setExtras(final Map<String, String> extras) {
    inMobiNative.setExtras(extras);
  }

  public void setKeywords(final String keywords) {
    inMobiNative.setKeywords(keywords);
  }

  public void load() {
    inMobiNative.load();
  }

  public void load(final byte[] bidToken) {
    inMobiNative.load(bidToken);
  }

  @Nullable
  public String getAdCtaText() {
    return inMobiNative.getAdCtaText();
  }

  @Nullable
  public String getAdDescription() {
    return inMobiNative.getAdDescription();
  }

  @Nullable
  public String getAdIconUrl() {
    return inMobiNative.getAdIconUrl();
  }

  @Nullable
  public String getAdLandingPageUrl() {
    return inMobiNative.getAdLandingPageUrl();
  }

  @Nullable
  public String getAdTitle() {
    return inMobiNative.getAdTitle();
  }

  @Nullable
  public JSONObject getCustomAdContent() {
    return inMobiNative.getCustomAdContent();
  }

  @Nullable
  public View getPrimaryViewOfWidth(
      Context context, View contentView, ViewGroup parent, Integer viewWidthInPixels) {
    return inMobiNative.getPrimaryViewOfWidth(context, contentView, parent, viewWidthInPixels);
  }

  @Nullable
  public Boolean isVideo() {
    return inMobiNative.isVideo();
  }

  public void reportAdClickAndOpenLandingPage() {
    inMobiNative.reportAdClickAndOpenLandingPage();
  }

  public void resume() {
    inMobiNative.resume();
  }

  public void destroy() {
    inMobiNative.destroy();
  }
}
