package com.google.ads.mediation.yandex.nativeads.view

import android.view.View

internal object ViewUtils {

    @JvmStatic
    fun <T : View?> castView(
            view: View?,
            expectedViewClass: Class<T>
    ): T? {
        return if (expectedViewClass.isInstance(view)) {
            expectedViewClass.cast(view)
        } else {
            null
        }
    }
}
