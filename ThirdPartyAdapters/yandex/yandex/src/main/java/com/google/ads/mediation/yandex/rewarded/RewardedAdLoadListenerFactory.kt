package com.google.ads.mediation.yandex.rewarded

import com.google.ads.mediation.yandex.base.AdMobAdErrorCreator
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener

internal class RewardedAdLoadListenerFactory {

    fun create(
            callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>,
            adMobAdErrorCreator: AdMobAdErrorCreator,
            mediationRewardedAdFactory: MediationRewardedAdFactory = MediationRewardedAdFactory()
    ) = object : RewardedAdLoadListener {

        var rewardedAdCallback: MediationRewardedAdCallback? = null

        override fun onAdLoaded(loadedAd: RewardedAd) {
            val mediationRewardedAd = mediationRewardedAdFactory.create(
                    loadedAd, ::onContextIsNotActivityError
            )
            rewardedAdCallback = callback.onSuccess(mediationRewardedAd).also { rewardedAdCallback ->
                val mediationRewardedAdEventListener = MediationRewardedAdEventListener(
                        rewardedAdCallback,
                        adMobAdErrorCreator
                )
                loadedAd.setAdEventListener(mediationRewardedAdEventListener)
            }
        }

        override fun onAdFailedToLoad(error: AdRequestError) {
            val adError = adMobAdErrorCreator.createLoadAdError(error)
            callback.onFailure(adError)
        }

        private fun onContextIsNotActivityError() {
            val error = adMobAdErrorCreator.createRequiresActivityError()
            rewardedAdCallback?.onAdFailedToShow(error)
        }
    }
}
