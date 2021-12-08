package com.google.ads.mediation.yahoo;

import static com.google.ads.mediation.yahoo.YahooAdapter.TAG;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.yahoo.ads.nativeplacement.NativeAd;
import com.yahoo.ads.utils.ThreadUtils;
import com.yahoo.ads.yahoonativecontroller.NativeComponent;
import com.yahoo.ads.yahoonativecontroller.NativeImageComponent;
import com.yahoo.ads.yahoonativecontroller.NativeTextComponent;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class AdapterUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

  /**
   * Yahoo native ad.
   */
  private final NativeAd yahooAd;

  /**
   * The Context.
   */
  private final Context context;

  public AdapterUnifiedNativeAdMapper(final Context context, @NonNull final NativeAd nativeAd) {

    this.context = context;
    yahooAd = nativeAd;

    // title
    setHeadline(getTextFromNativeTextComponent("title", nativeAd));

    // body
    setBody(getTextFromNativeTextComponent("body", nativeAd));

    // callToAction
    setCallToAction(getTextFromNativeTextComponent("callToAction", nativeAd));

    // disclaimer
    setAdvertiser(getTextFromNativeTextComponent("disclaimer", nativeAd));

    // rating
    String ratingString = getTextFromNativeTextComponent("rating", nativeAd);
    if (!TextUtils.isEmpty(ratingString)) {
      String[] ratingArray = ratingString.trim().split("\\s+");
      if (ratingArray.length >= 1) {
        try {
          Double rating = Double.parseDouble(ratingArray[0]);
          setStarRating(rating);
        } catch (NumberFormatException e) {
          // do nothing
        }
      }
    }
  }

  private Drawable drawableFromUrl(final String url) {

    HttpURLConnection connection = null;
    InputStream input = null;
    try {
      connection = (HttpURLConnection) new URL(url).openConnection();
      connection.connect();
      input = connection.getInputStream();

      Bitmap bitmap = BitmapFactory.decodeStream(input);
      return new BitmapDrawable(Resources.getSystem(), bitmap);
    } catch (Exception e) {
      Log.e(TAG, "Unable to create drawable from URL " + url, e);
    } finally {
      try {
        if (input != null) {
          input.close();
        }
      } catch (Exception e) {
        Log.w(TAG, "Caught an error closing InputStream.", e);
      }

      if (connection != null) {
        connection.disconnect();
      }
    }

    return null;
  }

  @Override
  public void recordImpression() {
    yahooAd.fireImpression();
  }

  @Override
  public void handleClick(@NonNull final View view) {
    yahooAd.invokeDefaultAction();
  }

  private AdapterNativeMappedImage getImageFromNativeImageComponent(final NativeComponent nativeImageComponent) {

    if (nativeImageComponent instanceof NativeImageComponent) {
      try {
        Uri url = ((NativeImageComponent) nativeImageComponent).getUri();
        if (url != null) {
          Drawable drawable = drawableFromUrl(url.toString());
          return new AdapterNativeMappedImage(drawable, url,
                  YahooAdapter.VAS_IMAGE_SCALE);
        }
      } catch (Exception e) {
        Log.e(TAG, "Unable to parse data object.", e);
      }
    }
    return null;
  }

  private String getTextFromNativeTextComponent(final String key, final NativeAd nativeAd) {

    NativeComponent nativeComponent = nativeAd.getComponent(key);
    if (nativeComponent instanceof NativeTextComponent) {
      return ((NativeTextComponent) nativeComponent).getText();
    }

    return "";
  }

  void loadResources(final LoadListener loadListener) {

    final NativeComponent nativeIconImageComponent = yahooAd.getComponent("iconImage");
    final NativeComponent nativeMainImageComponent = yahooAd.getComponent("mainImage");

    ThreadUtils.runOffUiThread(new Runnable() {
      @Override
      public void run() {

        try {
          boolean iconSet = false;
          boolean mediaViewSet = false;

          // iconImage
          AdapterNativeMappedImage adapterNativeMappedIconImage =
                  getImageFromNativeImageComponent(nativeIconImageComponent);
          if (adapterNativeMappedIconImage != null) {
            setIcon(adapterNativeMappedIconImage);
            iconSet = true;
          }

          // mainImage
          List<com.google.android.gms.ads.formats.NativeAd.Image> imagesList =
                  new ArrayList<>();
          AdapterNativeMappedImage adapterNativeMappedImage =
                  getImageFromNativeImageComponent(nativeMainImageComponent);
          if (adapterNativeMappedImage != null) {
            imagesList.add(adapterNativeMappedImage);

            // Setting main image as the AdMob's MediaView
            ImageView imageView = new ImageView(context);
            imageView.setImageDrawable(adapterNativeMappedImage.getDrawable());
            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            imageView.setLayoutParams(layoutParams);
            setMediaView(imageView);
            mediaViewSet = true;
          }
          setImages(imagesList);

          if (mediaViewSet && iconSet) {
            loadListener.onLoadComplete();
          } else {
            Log.e(TAG, "Failed to set icon and/or media view");
            loadListener.onLoadError();
          }
        } catch (Exception e) {
          Log.e(TAG, "Unable to load resources.", e);
          loadListener.onLoadError();
        }
      }
    });
  }

  interface LoadListener {

    void onLoadComplete();

    void onLoadError();
  }
}
