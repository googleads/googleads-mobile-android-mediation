package com.google.ads.mediation.verizon;

import static com.google.ads.mediation.verizon.VerizonMediationAdapter.TAG;

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
import com.verizon.ads.nativeplacement.NativeAd;
import com.verizon.ads.utils.ThreadUtils;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

class AdapterUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

  /**
   * Verizon Media native ad.
   */
  private final NativeAd verizonAd;

  /**
   * The Context.
   */
  private final Context context;

  public AdapterUnifiedNativeAdMapper(final Context context, @NonNull final NativeAd nativeAd) {

    this.context = context;
    verizonAd = nativeAd;

    // title
    setHeadline(parseTextComponent("title", nativeAd));

    // body
    setBody(parseTextComponent("body", nativeAd));

    // callToAction
    setCallToAction(parseTextComponent("callToAction", nativeAd));

    // disclaimer
    setAdvertiser(parseTextComponent("disclaimer", nativeAd));

    // rating
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
    verizonAd.fireImpression(context);
  }

  @Override
  public void handleClick(final View view) {
    verizonAd.invokeDefaultAction(context);
  }

  private AdapterNativeMappedImage parseImageComponent(final JSONObject jsonObject) {

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
            VerizonMediationAdapter.VAS_IMAGE_SCALE);
      } catch (Exception e) {
        Log.e(TAG, "Unable to parse data object.", e);
      }
    }
    return null;
  }

  private String parseTextComponent(final String key, final NativeAd nativeAd) {

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

  void loadResources(final LoadListener loadListener) {

    ThreadUtils.runOffUiThread(new Runnable() {
      @Override
      public void run() {

        try {
          boolean iconSet = false;
          boolean mediaViewSet = false;

          // iconImage
          JSONObject iconImageJSON = verizonAd.getJSON("iconImage");
          if (iconImageJSON != null) {
            AdapterNativeMappedImage adapterNativeMappedImage =
                parseImageComponent(iconImageJSON);
            if (adapterNativeMappedImage != null) {
              setIcon(adapterNativeMappedImage);
              iconSet = true;
            }
          }

          // mainImage
          JSONObject mainImageJSON = verizonAd.getJSON("mainImage");
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
