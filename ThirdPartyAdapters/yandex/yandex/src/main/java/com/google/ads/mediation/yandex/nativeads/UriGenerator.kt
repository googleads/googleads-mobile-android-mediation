package com.google.ads.mediation.yandex.nativeads

import android.net.Uri

internal class UriGenerator {

    fun createImageUri(any: Any): Uri? {
        return Uri.parse(IMAGE_URI_PREFIX + any.hashCode())
    }

    companion object {
        private const val IMAGE_URI_PREFIX = "admob.image.url."
    }
}
