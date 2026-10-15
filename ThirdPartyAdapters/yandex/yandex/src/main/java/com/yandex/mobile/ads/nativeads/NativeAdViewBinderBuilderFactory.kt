package com.yandex.mobile.ads.nativeads

import android.view.View

internal class NativeAdViewBinderBuilderFactory {
    fun create(view: View) = NativeAdViewBinder.Builder(view)
}
