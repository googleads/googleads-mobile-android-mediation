package com.google.ads.mediation.yandex.rewarded

import android.app.Activity
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAd

internal class MediationRewardedAdFactory {

    fun create(
            loadedAd: RewardedAd,
            onContextIsNotActivity: () -> Unit
    ) = MediationRewardedAd { context ->
        if (context is Activity) {
            loadedAd.show(context)
        } else onContextIsNotActivity()
    }
}
