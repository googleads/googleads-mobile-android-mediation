package com.google.ads.mediation.verizon;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;

import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper;
import com.verizon.ads.nativeplacement.NativeAd;
import com.verizon.ads.nativeplacement.VASDisplayMediaView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class AdapterUnifiedNativeAdMapper extends UnifiedNativeAdMapper {

	private final NativeAd verizonAd;
	private final Context context;


	AdapterUnifiedNativeAdMapper(final Context context, final NativeAd nativeAd) {

		this.context = context;
		this.verizonAd = nativeAd;

		if (nativeAd != null) {
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

			// iconImage
			JSONObject iconImageJSON = nativeAd.getJSON("iconImage");
			if (iconImageJSON != null) {
				setIcon(mappedImageFromJSON(iconImageJSON));
			}

			// mainImage
			JSONObject mainImageJSON = nativeAd.getJSON("mainImage");
			if (mainImageJSON != null) {
				List<com.google.android.gms.ads.formats.NativeAd.Image> imagesList = new ArrayList<>();
				imagesList.add(mappedImageFromJSON(mainImageJSON));
				setImages(imagesList);
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


	private AdapterNativeMappedImage mappedImageFromJSON(final JSONObject jsonObject) {

		Uri url = Uri.parse(jsonObject.optString("url"));
		String assetPath = jsonObject.optString("asset");
		Drawable drawable = null;
		if (assetPath != null) {
			drawable = Drawable.createFromPath(assetPath);
		}
		return new AdapterNativeMappedImage(drawable, url, VerizonMediationAdapter.VAS_IMAGE_SCALE);
	}

}
