package com.google.ads.mediation.yahoo;

import static com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_DOMAIN;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.ERROR_FAILED_TO_LOAD_NATIVE_ASSETS;
import static com.google.ads.mediation.yahoo.YahooMediationAdapter.TAG;

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
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.yahoo.ads.nativeplacement.NativeAd;
import com.yahoo.ads.utils.ThreadUtils;
import org.json.JSONObject;
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

  public AdapterUnifiedNativeAdMapper(@NonNull final Context context,
      @NonNull final NativeAd nativeAd) {
    this.context = context;
    yahooAd = nativeAd;

    // Title
    setHeadline(parseTextComponent("title", nativeAd));

    // Body
    setBody(parseTextComponent("body", nativeAd));

    // Call to Action
    setCallToAction(parseTextComponent("callToAction", nativeAd));

    // Disclaimer
    setAdvertiser(parseTextComponent("disclaimer", nativeAd));

    // Rating
    String ratingString = parseTextComponent("rating", nativeAd);
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

  @Nullable
  private Drawable drawableFromUrl(@NonNull final String url) {
    HttpURLConnection connection = null;
    InputStream input = null;
    try {
      connection = (HttpURLConnection) new URL(url).openConnection();
      connection.connect();
      input = connection.getInputStream();

      Bitmap bitmap = BitmapFactory.decodeStream(input);
      return new BitmapDrawable(Resources.getSystem(), bitmap);
    } catch (Exception e) {
      Log.e(TAG, "Unable to create drawable from URL: " + url, e);
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

  @Nullable
  private AdapterNativeMappedImage parseImageComponent(@Nullable final JSONObject jsonObject) {
    if (jsonObject != null) {
      try {
        JSONObject dataObject = jsonObject.getJSONObject("data");
        Uri url = Uri.parse(dataObject.optString("url"));
        String assetPath = dataObject.optString("asset");
        Drawable drawable;
        if (TextUtils.isEmpty(assetPath)) {
          drawable = drawableFromUrl(url.toString());
        } else {
          drawable = Drawable.createFromPath(assetPath);
        }
        return new AdapterNativeMappedImage(drawable, url,
            YahooMediationAdapter.YAS_IMAGE_SCALE);
      } catch (Exception e) {
        Log.e(TAG, "Unable to parse data object.", e);
      }
    }
    return null;
  }

  @NonNull
  private String parseTextComponent(@NonNull final String key, @NonNull final NativeAd nativeAd) {
    String value = "";
    JSONObject jsonObject = nativeAd.getJSON(key);
    if (jsonObject != null) {
      try {
        JSONObject dataObject = jsonObject.getJSONObject("data");
        value = dataObject.optString("value");
      } catch (Exception e) {
        Log.e(TAG, "Unable to parse " + key, e);
      }
    }
    return value;
  }

  void loadResources(@NonNull final LoadListener loadListener) {
    ThreadUtils.runOffUiThread(new Runnable() {
      @Override
      public void run() {

        try {
          boolean iconSet = false;
          boolean mediaViewSet = false;

          // iconImage
          JSONObject iconImageJSON = yahooAd.getJSON("iconImage");
          if (iconImageJSON != null) {
            AdapterNativeMappedImage adapterNativeMappedImage =
                parseImageComponent(iconImageJSON);
            if (adapterNativeMappedImage != null) {
              setIcon(adapterNativeMappedImage);
              iconSet = true;
            }
          }

          // mainImage
          JSONObject mainImageJSON = yahooAd.getJSON("mainImage");
          if (mainImageJSON != null) {
            List<com.google.android.gms.ads.formats.NativeAd.Image> imagesList =
                new ArrayList<>();
            AdapterNativeMappedImage adapterNativeMappedImage =
                parseImageComponent(mainImageJSON);
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
          }

          if (mediaViewSet && iconSet) {
            loadListener.onLoadComplete();
          } else {
            AdError mediaError = new AdError(ERROR_FAILED_TO_LOAD_NATIVE_ASSETS,
                "Failed to set icon and/or media views.", ERROR_DOMAIN);
            loadListener.onLoadError(mediaError);
          }
        } catch (Exception exception) {
          AdError mediaError = new AdError(ERROR_FAILED_TO_LOAD_NATIVE_ASSETS,
              "Exception thrown when loading native ad resources.", ERROR_DOMAIN);
          Log.e(TAG, "Exception thrown when loading native ad resources.", exception);
          loadListener.onLoadError(mediaError);
        }
      }
    });
  }

  interface LoadListener {

    void onLoadComplete();

    void onLoadError(@NonNull AdError loadError);
  }
}
