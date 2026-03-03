package com.google.ads.mediation.yandex

import android.content.Context
import com.google.ads.mediation.yandex.banner.BannerAdViewWrapper
import com.google.ads.mediation.yandex.banner.MediationAdEventListener
import com.google.ads.mediation.yandex.banner.YandexAdSizeProvider
import com.google.ads.mediation.yandex.base.AdMobAdErrorCreator
import com.google.ads.mediation.yandex.base.AdMobServerExtrasParserProvider
import com.google.ads.mediation.yandex.base.AdRequestMapper
import com.google.ads.mediation.yandex.base.MediationAdRequestWrapper
import com.google.ads.mediation.yandex.base.YandexAdRequestCreator
import com.google.ads.mediation.yandex.base.YandexErrorConverter
import com.google.ads.mediation.yandex.base.YandexVersionInfoProvider
import com.google.ads.mediation.yandex.interstitial.InterstitialAdLoadListenerFactory
import com.google.ads.mediation.yandex.interstitial.InterstitialLoaderFactory
import com.google.ads.mediation.yandex.nativeads.YandexNativeAdLoadListener
import com.google.ads.mediation.yandex.rewarded.RewardedAdLoadListenerFactory
import com.google.ads.mediation.yandex.rewarded.RewardedLoaderFactory
import com.google.android.gms.ads.mediation.Adapter
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.common.MobileAds
import com.yandex.mobile.ads.nativeads.NativeAdLoader

internal class YandexAdapter @JvmOverloads constructor(
        private val yandexAdRequestCreator: YandexAdRequestCreator = YandexAdRequestCreator(),
        private val adRequestMapper: AdRequestMapper = AdRequestMapper(),
        private val interstitialLoaderFactory: InterstitialLoaderFactory = InterstitialLoaderFactory(),
        private val rewardedLoaderFactory: RewardedLoaderFactory = RewardedLoaderFactory(),
        private val interstitialAdLoadListenerFactory: InterstitialAdLoadListenerFactory =
                InterstitialAdLoadListenerFactory(),
        private val rewardedAdLoadListenerFactory: RewardedAdLoadListenerFactory = RewardedAdLoadListenerFactory(),
        private val adMobAdErrorCreator: AdMobAdErrorCreator = AdMobAdErrorCreator(),
        private val yandexErrorConverter: YandexErrorConverter = YandexErrorConverter(),
        private val adMobServerExtrasParserProvider: AdMobServerExtrasParserProvider = AdMobServerExtrasParserProvider(),
        private val yandexVersionInfoProvider: YandexVersionInfoProvider = YandexVersionInfoProvider(),
        private val yandexAdSizeProvider: YandexAdSizeProvider = YandexAdSizeProvider()
) : Adapter() {

    override fun getSDKVersionInfo() = yandexVersionInfoProvider.getSdkVersionInfo()

    override fun getVersionInfo() = yandexVersionInfoProvider.adapterVersionInfo

    override fun initialize(
            context: Context,
            initializationCompleteCallback: InitializationCompleteCallback,
            list: MutableList<MediationConfiguration>
    ) {
        MobileAds.initialize(context) {
            initializationCompleteCallback.onInitializationSucceeded()
        }
    }

    override fun loadBannerAd(
            mediationBannerAdConfiguration: MediationBannerAdConfiguration,
            callback: MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>
    ) {
        try {
            val serverExtrasParser = adMobServerExtrasParserProvider.getServerExtrasParser(
                    mediationBannerAdConfiguration.serverParameters,
                    CUSTOM_EVENT_SERVER_PARAMETER_FIELD
            )
            val adRequestWrapper = MediationAdRequestWrapper(mediationBannerAdConfiguration)
            val adRequest = yandexAdRequestCreator.createAdRequest(adRequestWrapper)
            val context = mediationBannerAdConfiguration.context

            val adUnitId = serverExtrasParser.parseAdUnitId()
            if (adUnitId.isNullOrEmpty() == false) {
                val bannerAdView = BannerAdView(context)
                val adViewWrapper = BannerAdViewWrapper(bannerAdView)

                val adSize = yandexAdSizeProvider.getAdSize(
                        context, serverExtrasParser, mediationBannerAdConfiguration.adSize
                )
                val bannerAdEventListener = MediationAdEventListener(adViewWrapper, callback, adMobAdErrorCreator)
                bannerAdView.apply {
                    adSize?.let { setAdSize(it) }
                    setAdUnitId(adUnitId)
                    setBannerAdEventListener(bannerAdEventListener)

                    loadAd(adRequest)
                }
            } else {
                val adRequestError = yandexErrorConverter.convertToInvalidRequestError(ERROR_MESSAGE_INVALID_REQUEST)
                callback.onFailure(adMobAdErrorCreator.createLoadAdError(adRequestError))
            }
        } catch (t: Throwable) {
            val adRequestError = yandexErrorConverter.convertToInvalidRequestError(t.message)
            callback.onFailure(adMobAdErrorCreator.createLoadAdError(adRequestError))
        }
    }

    override fun loadNativeAd(
            mediationNativeAdConfiguration: MediationNativeAdConfiguration,
            callback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>
    ) {
        try {
            val serverExtrasParser = adMobServerExtrasParserProvider.getServerExtrasParser(
                    mediationNativeAdConfiguration.serverParameters,
                    CUSTOM_EVENT_SERVER_PARAMETER_FIELD
            )
            val adRequestWrapper = MediationAdRequestWrapper(mediationNativeAdConfiguration)
            val context = mediationNativeAdConfiguration.context

            val adUnitId = serverExtrasParser.parseAdUnitId()
            if (adUnitId.isNullOrEmpty() == false) {
                val nativeAdLoader = NativeAdLoader(context)
                val nativeAdLoadListener = YandexNativeAdLoadListener(
                        context,
                        callback,
                        mediationNativeAdConfiguration.mediationExtras
                )

                val configuration =
                        yandexAdRequestCreator.createNativeAdRequestConfiguration(adRequestWrapper, adUnitId)

                nativeAdLoader.apply {
                    setNativeAdLoadListener(nativeAdLoadListener)
                    loadAd(configuration)
                }
            } else {
                val adRequestError = yandexErrorConverter.convertToInvalidRequestError(ERROR_MESSAGE_INVALID_REQUEST)
                callback.onFailure(adMobAdErrorCreator.createLoadAdError(adRequestError))
            }
        } catch (t: Throwable) {
            val adRequestError = yandexErrorConverter.convertToInvalidRequestError(t.message)
            callback.onFailure(adMobAdErrorCreator.createLoadAdError(adRequestError))
        }
    }

    override fun loadInterstitialAd(
            mediationInterstitialAdConfiguration: MediationInterstitialAdConfiguration,
            callback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>
    ) {
        try {
            val serverExtrasParser = adMobServerExtrasParserProvider.getServerExtrasParser(
                    mediationInterstitialAdConfiguration.serverParameters,
                    CUSTOM_EVENT_SERVER_PARAMETER_FIELD
            )
            val configuration = adRequestMapper.toAdRequestConfiguration(serverExtrasParser)

            if (configuration != null) {
                val context = mediationInterstitialAdConfiguration.context
                val interstitialAdLoadListener = interstitialAdLoadListenerFactory.create(
                        callback, adMobAdErrorCreator
                )

                with(interstitialLoaderFactory.create(context)) {
                    setAdLoadListener(interstitialAdLoadListener)
                    loadAd(configuration)
                }
            } else {
                val adRequestError = yandexErrorConverter.convertToInvalidRequestError(ERROR_MESSAGE_INVALID_REQUEST)
                callback.onFailure(adMobAdErrorCreator.createLoadAdError(adRequestError))
            }
        } catch (t: Throwable) {
            val adRequestError = yandexErrorConverter.convertToInvalidRequestError(t.message)
            callback.onFailure(adMobAdErrorCreator.createLoadAdError(adRequestError))
        }
    }

    override fun loadRewardedAd(
            mediationRewardedAdConfiguration: MediationRewardedAdConfiguration,
            callback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>
    ) {
        try {
            val serverExtrasParser = adMobServerExtrasParserProvider.getServerExtrasParser(
                    mediationRewardedAdConfiguration.serverParameters,
                    CUSTOM_EVENT_SERVER_PARAMETER_FIELD
            )
            val configuration = adRequestMapper.toAdRequestConfiguration(serverExtrasParser)
            if (configuration != null) {
                val context = mediationRewardedAdConfiguration.context
                val rewardedAdLoadListener = rewardedAdLoadListenerFactory.create(
                        callback, adMobAdErrorCreator
                )

                with(rewardedLoaderFactory.create(context)) {
                    setAdLoadListener(rewardedAdLoadListener)
                    loadAd(configuration)
                }
            } else {
                val adRequestError = yandexErrorConverter.convertToInvalidRequestError(ERROR_MESSAGE_INVALID_REQUEST)
                callback.onFailure(adMobAdErrorCreator.createLoadAdError(adRequestError))
            }
        } catch (t: Throwable) {
            val adRequestError = yandexErrorConverter.convertToInvalidRequestError(t.message)
            callback.onFailure(adMobAdErrorCreator.createLoadAdError(adRequestError))
        }
    }

    companion object {

        private const val CUSTOM_EVENT_SERVER_PARAMETER_FIELD = "parameter"
        private const val ERROR_MESSAGE_INVALID_REQUEST = "Invalid request"
    }
}
