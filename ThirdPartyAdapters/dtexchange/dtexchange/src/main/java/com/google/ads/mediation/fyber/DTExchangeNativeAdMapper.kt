package com.google.ads.mediation.fyber

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.fyber.inneractive.sdk.external.InneractiveAdSpot
import com.fyber.inneractive.sdk.external.NativeAdContent
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager
import com.fyber.inneractive.sdk.external.NativeAdUnitController
import com.fyber.inneractive.sdk.external.NativeAdVideoContentController
import com.fyber.inneractive.sdk.external.VideoContentListener
import com.fyber.inneractive.sdk.external.NativeAdEventsListener
import com.fyber.inneractive.sdk.external.InneractiveErrorCode
import com.fyber.inneractive.sdk.external.MediaView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdAssetNames

class DTExchangeNativeAdMapper(
    private val adLoadCallback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>
) : NativeAdMapper() {

    private var mediationNativeAdCallback: MediationNativeAdCallback? = null
    private var nativeAdSpot: InneractiveAdSpot? = null
    private var adContent: NativeAdContent? = null

    fun loadAd(mediationNativeAdConfiguration: MediationNativeAdConfiguration) {
        InneractiveAdManager.setMediationName(
            FyberMediationAdapter.MEDIATOR_NAME
        )
        InneractiveAdManager.setMediationVersion(
            MobileAds.getVersion()
                .toString()
        )

        val bidResponse = mediationNativeAdConfiguration.bidResponse

        nativeAdSpot = InneractiveAdSpotManager.get()
            .createSpot()
            .apply {
                val controller = NativeAdUnitController()

                val nativeAdVideoContentController = NativeAdVideoContentController()
                nativeAdVideoContentController.eventsListener = object : VideoContentListener {
                    override fun onProgress(
                        totalDurationInMsec: Int,
                        positionInMsec: Int
                    ) {
                    }

                    override fun onCompleted() {
                        mediationNativeAdCallback?.onVideoComplete()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onPlayerError() {
                    }
                }

                controller.addContentController(nativeAdVideoContentController)

                controller.eventsListener = object : NativeAdEventsListener() {
                    override fun onAdImpression(adSpot: InneractiveAdSpot) {
                        mediationNativeAdCallback?.onAdOpened()
                        mediationNativeAdCallback?.reportAdImpression()
                    }

                    override fun onAdClicked(adSpot: InneractiveAdSpot) {
                        mediationNativeAdCallback?.reportAdClicked()
                    }

                    override fun onAdWillCloseInternalBrowser(adSpot: InneractiveAdSpot) {}
                    override fun onAdWillOpenExternalApp(adSpot: InneractiveAdSpot) {
                        mediationNativeAdCallback?.onAdLeftApplication()
                    }
                }
                addUnitController(controller)
                setRequestListener(object : InneractiveAdSpot.NativeAdRequestListener() {
                    override fun onInneractiveFailedAdRequest(
                        adSpot: InneractiveAdSpot?,
                        errorCode: InneractiveErrorCode?
                    ) {
                        reportErrorAndDestroy("onInneractiveFailedAdRequest error: $errorCode", errorCode)
                    }

                    override fun onInneractiveSuccessfulNativeAdRequest(
                        adSpot: InneractiveAdSpot?,
                        content: NativeAdContent?
                    ) {
                        if (content != null) {
                            mapNativeAd(mediationNativeAdConfiguration.context, content)
                            mediationNativeAdCallback =
                                adLoadCallback.onSuccess(this@DTExchangeNativeAdMapper)
                        } else {
                            reportErrorAndDestroy(
                                "content is NOT NativeAdContent", InneractiveErrorCode.SDK_INTERNAL_ERROR
                            )
                        }
                    }

                })
                FyberAdapterUtils.updateFyberExtraParams(
                    mediationNativeAdConfiguration.mediationExtras
                )
                loadAd(bidResponse)
            }
    }

    override fun trackViews(
        containerView: View,
        clickableAssetViews: Map<String, View>,
        nonClickableAssetViews: Map<String, View>
    ) {
        containerView.tag = NativeAdContent.ViewTag.ROOT
        adContent?.mediaView?.tag = NativeAdContent.ViewTag.MEDIA_VIEW

        for ((nativeAdAssetName, view) in clickableAssetViews) {
            view.tag = mapAssetNameToViewTag(nativeAdAssetName)
        }

        adContent?.registerViewsForInteraction(
            containerView as ViewGroup,
            adContent?.mediaView,
            null,
            clickableAssetViews.values
        )
    }

    private fun mapAssetNameToViewTag(
        nativeAdAssetName: String
    ): String {
        return when (nativeAdAssetName) {
            NativeAdAssetNames.ASSET_CALL_TO_ACTION -> NativeAdContent.ViewTag.CTA
            NativeAdAssetNames.ASSET_HEADLINE -> NativeAdContent.ViewTag.AD_TITLE
            NativeAdAssetNames.ASSET_BODY -> NativeAdContent.ViewTag.AD_DESCRIPTION
            NativeAdAssetNames.ASSET_ICON -> NativeAdContent.ViewTag.AD_ICON
            NativeAdAssetNames.ASSET_STAR_RATING -> NativeAdContent.ViewTag.RATING
            else -> NativeAdContent.ViewTag.OTHER
        }
    }

    private fun reportErrorAndDestroy(
        message: String,
        errorCode: InneractiveErrorCode?
    ) {
        Log.e(TAG, message)
        adLoadCallback.onFailure(DTExchangeErrorCodes.getAdError(errorCode ?: InneractiveErrorCode.SDK_INTERNAL_ERROR))
        nativeAdSpot?.destroy()
        nativeAdSpot = null
    }

    private fun mapNativeAd(
        context: Context,
        nativeAdContent: NativeAdContent
    ) {
        val mediaView = MediaView(context)

        nativeAdContent.bindMediaView(mediaView)
        adContent = nativeAdContent

        headline = nativeAdContent.adTitle
        body = nativeAdContent.adDescription
        icon = NativeMappedImage(nativeAdContent.appIcon)
        callToAction = nativeAdContent.adCallToAction

        setMediaView(nativeAdContent.mediaView)
        starRating = nativeAdContent.rating.toDouble()
        mediaContentAspectRatio = nativeAdContent.mediaAspectRatio ?: 0f

        overrideClickHandling = true
        overrideImpressionRecording = true
    }

    override fun destroy() {
        super.destroy()
        nativeAdSpot?.destroy()
        nativeAdSpot = null

        adContent?.destroy()
        adContent = null

        mediationNativeAdCallback = null
    }

    class NativeMappedImage(
        private val uri: Uri
    ) : NativeAd.Image() {

        override fun getDrawable() = null

        override fun getUri() = uri

        override fun getScale() = 1.0

    }

    private companion object {
        private const val TAG = "DTExchangeNativeAdMapper"
    }
}