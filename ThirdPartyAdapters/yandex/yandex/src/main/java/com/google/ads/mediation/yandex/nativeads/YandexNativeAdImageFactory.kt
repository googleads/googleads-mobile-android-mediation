package com.google.ads.mediation.yandex.nativeads

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.yandex.mobile.ads.nativeads.NativeAdImage

internal class YandexNativeAdImageFactory {

    private val uriGenerator: UriGenerator = UriGenerator()

    fun createYandexNativeAdImage(
            context: Context,
            imageData: NativeAdImage?
    ): YandexNativeAdImage? {
        var nativeAdImage: YandexNativeAdImage? = null

        if (imageData != null) {
            val resources = context.resources
            val bitmap = imageData.bitmap
            val imageDrawable: Drawable = BitmapDrawable(resources, bitmap)
            uriGenerator.createImageUri(imageData)?.let {
                nativeAdImage = YandexNativeAdImage(imageDrawable, it)
            }
        }

        return nativeAdImage
    }
}
