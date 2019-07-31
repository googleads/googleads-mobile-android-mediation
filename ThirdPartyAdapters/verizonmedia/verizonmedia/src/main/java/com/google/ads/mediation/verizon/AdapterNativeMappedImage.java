package com.google.ads.mediation.verizon;

import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.google.android.gms.ads.formats.NativeAd;


public class AdapterNativeMappedImage extends NativeAd.Image {

	private final Drawable drawable;
	private final Uri imageUri;
	private final double scale;


	AdapterNativeMappedImage(final Drawable drawable, final Uri imageUri, final double scale) {

		this.drawable = drawable;
		this.imageUri = imageUri;
		this.scale = scale;
	}


	@Override
	public Drawable getDrawable() {

		return drawable;
	}


	@Override
	public Uri getUri() {

		return imageUri;
	}


	@Override
	public double getScale() {

		return scale;
	}

}
