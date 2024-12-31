package com.google.ads.mediation.yandex.rewarded

import android.content.Context
import com.yandex.mobile.ads.rewarded.RewardedAdLoader

internal class RewardedLoaderFactory {
    fun create(context: Context) = RewardedAdLoader(context)
}
