package com.yandex.mobile.ads.nativeads

import android.os.Bundle

internal class YandexNativeAdMapperFactory {
    fun create(nativeAd: NativeAd, extras: Bundle) = YandexNativeAdMapper(nativeAd, extras)
}
