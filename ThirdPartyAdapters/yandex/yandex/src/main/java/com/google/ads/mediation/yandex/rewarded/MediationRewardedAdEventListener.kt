package com.google.ads.mediation.yandex.rewarded

import com.google.ads.mediation.yandex.base.AdMobAdErrorCreator
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener

internal class MediationRewardedAdEventListener(
        private val rewardedAdCallback: MediationRewardedAdCallback,
        private val errorConverter: AdMobAdErrorCreator
) : RewardedAdEventListener {

    override fun onAdClicked() {
        rewardedAdCallback.reportAdClicked()
    }

    override fun onAdFailedToShow(adError: AdError) {
        rewardedAdCallback.onAdFailedToShow(errorConverter.convertToAdMobError(adError))
    }

    override fun onAdImpression(impressionData: ImpressionData?) {
        rewardedAdCallback.reportAdImpression()
    }

    override fun onAdDismissed() {
        rewardedAdCallback.onAdClosed()
    }

    override fun onAdShown() {
        rewardedAdCallback.apply {
            onAdOpened()
            onVideoStart()
        }
    }

    override fun onRewarded(reward: Reward) {
        val rewardItem = AdMobReward(reward)
        rewardedAdCallback.onUserEarnedReward(rewardItem)
    }
}
