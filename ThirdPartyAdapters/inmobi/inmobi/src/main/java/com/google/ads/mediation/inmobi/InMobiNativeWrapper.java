package com.google.ads.mediation.inmobi;

import android.view.View;
import androidx.annotation.Nullable;
import com.inmobi.ads.InMobiNative;
import com.inmobi.ads.listeners.VideoEventListener;
import com.inmobi.media.ads.nativeAd.InMobiNativeImage;
import com.inmobi.media.ads.nativeAd.InMobiNativeViewData;
import com.inmobi.media.ads.nativeAd.MediaView;
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
    return inMobiNative.getCtaText();
  }

  @Nullable
  public String getAdvertiserName() {
    return inMobiNative.getAdvertiserName();
  }

  @Nullable
  public View getAdChoiceIcon() {
    return inMobiNative.getAdChoiceIcon();
  }

  @Nullable
  public String getAdDescription() {
    return inMobiNative.getAdDescription();
  }

  @Nullable
  public String getAdIconUrl() {
    InMobiNativeImage image = inMobiNative.getAdIcon();
    return image == null ? null : image.getUrl();
  }

  @Nullable
  public String getAdTitle() {
    return inMobiNative.getAdTitle();
  }

  @Nullable
  public JSONObject getCustomAdContent() {
    return inMobiNative.getAdContent();
  }

  public float getAdRating() {
    return inMobiNative.getAdRating();
  }

  @Nullable
  public MediaView getMediaView() {
    return inMobiNative.getMediaView();
  }

  public Boolean isVideo() {
    return inMobiNative.isVideo();
  }

  public void registerForTracking(InMobiNativeViewData nativeViewData) {
    inMobiNative.registerViewForTracking(nativeViewData);
  }

  public void unTrackViews() {
    inMobiNative.unTrackViews();
  }

  public void destroy() {
    inMobiNative.destroy();
  }
}
