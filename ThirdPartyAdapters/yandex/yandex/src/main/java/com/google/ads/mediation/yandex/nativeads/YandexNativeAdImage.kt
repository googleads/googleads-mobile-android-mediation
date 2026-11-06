package com.google.ads.mediation.yandex.nativeads

import android.graphics.drawable.Drawable
import android.net.Uri
import com.google.android.gms.ads.formats.NativeAd

internal class YandexNativeAdImage(
        private val drawable: Drawable,
        private val uri: Uri
) : NativeAd.Image() {

    override fun getDrawable(): Drawable = drawable

    override fun getUri(): Uri = uri

    override fun getScale(): Double = 1.0
}
