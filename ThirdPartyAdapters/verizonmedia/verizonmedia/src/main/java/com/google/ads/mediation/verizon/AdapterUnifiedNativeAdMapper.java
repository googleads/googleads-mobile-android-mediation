package com.google.ads.mediation.verizon;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.verizon.ads.nativeplacement.NativeAd;

import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;


public class AdapterUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

    private static final String TAG = AdapterUnifiedNativeAdMapper.class.getSimpleName();

    private final NativeAd verizonAd;
    private final Context context;


    AdapterUnifiedNativeAdMapper(final Context context, @NonNull final NativeAd nativeAd) {

        this.context = context;
        verizonAd = nativeAd;

        // title
        JSONObject titleJSON = nativeAd.getJSON("title");
        if (titleJSON != null) {
            setHeadline(titleJSON.optString("data"));
        }

        // body
        JSONObject bodyJSON = nativeAd.getJSON("body");
        if (bodyJSON != null) {
            setBody(bodyJSON.optString("data"));
        }

        // callToAction
        JSONObject callToActionJSON = nativeAd.getJSON("callToAction");
        if (callToActionJSON != null) {
            setCallToAction(callToActionJSON.optString("data"));
        }

        // disclaimer
        JSONObject disclaimerJSON = nativeAd.getJSON("disclaimer");
        if (disclaimerJSON != null) {
            setAdvertiser(disclaimerJSON.optString("data"));
        }

        // rating
        JSONObject ratingJSON = nativeAd.getJSON("rating");
        if (ratingJSON != null) {
            String ratingString = ratingJSON.optString("data");
            if (ratingString != null) {
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
    }


    @Override
    public void recordImpression() {

        verizonAd.fireImpression();
    }


    @Override
    public void handleClick(final View view) {

        verizonAd.invokeDefaultAction(context);
    }


    void loadResources(final LoadListener loadListener) {

        com.verizon.ads.utils.ThreadUtils.runOffUiThread(new Runnable() {
            @Override
            public void run() {

                try {
                    // iconImage
                    JSONObject iconImageJSON = verizonAd.getJSON("iconImage");
                    if (iconImageJSON != null) {
                        AdapterNativeMappedImage adapterNativeMappedImage = nativeMappedImageFromJSON(iconImageJSON.optString("url"));
                        if (adapterNativeMappedImage != null) {
                            setIcon(adapterNativeMappedImage);
                        }
                    }

                    // mainImage
                    JSONObject mainImageJSON = verizonAd.getJSON("mainImage");
                    if (mainImageJSON != null) {
                        List<com.google.android.gms.ads.formats.NativeAd.Image> imagesList = new ArrayList<>();
                        AdapterNativeMappedImage adapterNativeMappedImage = nativeMappedImageFromJSON(mainImageJSON.optString("url"));
                        if (adapterNativeMappedImage != null) {
                            imagesList.add(adapterNativeMappedImage);

                            // Setting main image as the AdMob's MediaView
                            ImageView imageView = new ImageView(context);
                            imageView.setImageDrawable(adapterNativeMappedImage.getDrawable());
                            ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                            imageView.setLayoutParams(layoutParams);
                            setMediaView(imageView);
                        }
                        setImages(imagesList);
                    }

                    loadListener.onLoadComplete();
                } catch (Exception e) {
                    Log.e(TAG, "Unable to load resources.", e);
                    loadListener.onLoadError();
                }
            }
        });
    }


    private AdapterNativeMappedImage nativeMappedImageFromJSON(final String url) {

        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();
            input = connection.getInputStream();

            Bitmap bitmap = BitmapFactory.decodeStream(input);
            return new AdapterNativeMappedImage(new BitmapDrawable(Resources.getSystem(), bitmap), Uri.parse(url), VerizonMediationAdapter.VAS_IMAGE_SCALE);
        } catch (Exception e) {
            Log.e(TAG, "Unable to create drawable from URL " + url, e);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error closing input stream.", e);
            }

            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }


    interface LoadListener {

        void onLoadComplete();
        void onLoadError();
    }
}
