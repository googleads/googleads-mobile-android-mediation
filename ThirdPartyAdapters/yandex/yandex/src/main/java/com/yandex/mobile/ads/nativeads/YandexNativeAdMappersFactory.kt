package com.yandex.mobile.ads.nativeads

import android.content.Context
import android.os.Bundle
import com.google.ads.mediation.yandex.nativeads.YandexNativeAdAsset
import com.google.ads.mediation.yandex.nativeads.YandexNativeAdImageFactory
import com.google.android.gms.ads.formats.NativeAd.Image

internal class YandexNativeAdMappersFactory(
        private val adMapperFactory: YandexNativeAdMapperFactory = YandexNativeAdMapperFactory(),
        private val nativeAdImageFactory: YandexNativeAdImageFactory = YandexNativeAdImageFactory(),
        private val mediaViewFactory: MediaViewFactory = MediaViewFactory()
) {

    fun createAdMapper(
            context: Context,
            nativeAd: NativeAd,
            extras: Bundle?
    ): YandexNativeAdMapper {
        val networkExtras = extras ?: Bundle()
        val mapper = adMapperFactory.create(nativeAd, networkExtras)

        mapper.overrideClickHandling = OVERRIDE_CLICK_TRACKING
        mapper.overrideImpressionRecording = OVERRIDE_IMPRESSION_RECORDING

        val assets = nativeAd.adAssets

        assets.sponsored?.let {
            mapper.advertiser = it
        }

        assets.body?.let {
            mapper.body = it
        }

        assets.callToAction?.let {
            mapper.callToAction = it
        }

        assets.title?.let {
            mapper.headline = it
        }

        assets.price?.let {
            mapper.price = it
        }

        assets.domain?.let {
            mapper.store = it
        }

        nativeAdImageFactory.createYandexNativeAdImage(context, assets.icon)?.let {
            mapper.icon = it
        }

        mapper.images = createNativeAdImages(context, assets.image)

        assets.rating?.let {
            mapper.starRating = it.toDouble()
        }

        val media = assets.media
        val hasMediaContent = media != null

        mapper.setHasVideoContent(hasMediaContent)

        if (hasMediaContent) {
            media?.let {
                mapper.mediaContentAspectRatio = media.aspectRatio
            }
        }

        val mediaView = mediaViewFactory.create(context)
        mapper.setMediaView(mediaView)

        val assetExtras = createAssetExtras(assets)
        mapper.extras = assetExtras

        return mapper
    }

    private fun createAssetExtras(assets: NativeAdAssets): Bundle {
        val assetExtras = Bundle()

        with(assetExtras) {
            putString(YandexNativeAdAsset.AGE, assets.age)
            putString(YandexNativeAdAsset.DOMAIN, assets.domain)
            putString(YandexNativeAdAsset.REVIEW_COUNT, assets.reviewCount)
            putString(YandexNativeAdAsset.SPONSORED, assets.sponsored)
            putString(YandexNativeAdAsset.WARNING, assets.warning)
        }

        return assetExtras
    }

    private fun createNativeAdImages(
            context: Context,
            nativeAdImage: NativeAdImage?
    ): List<Image?> {
        val images: MutableList<Image?> = ArrayList()

        if (nativeAdImage != null) {
            images.add(nativeAdImageFactory.createYandexNativeAdImage(context, nativeAdImage))
        }

        return images
    }

    companion object {
        private const val OVERRIDE_CLICK_TRACKING = true
        private const val OVERRIDE_IMPRESSION_RECORDING = true
    }
}
