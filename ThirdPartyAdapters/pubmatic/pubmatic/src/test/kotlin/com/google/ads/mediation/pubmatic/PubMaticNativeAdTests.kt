package com.google.ads.mediation.pubmatic

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.FutureTarget
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
import com.google.android.gms.ads.VersionInfo
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.nativead.POBNativeAd
import com.pubmatic.sdk.nativead.POBNativeAdLoader
import com.pubmatic.sdk.nativead.response.POBNativeAdDataResponseAsset
import com.pubmatic.sdk.nativead.response.POBNativeAdImageResponseAsset
import com.pubmatic.sdk.nativead.response.POBNativeAdTitleResponseAsset
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/** Tests for PubMaticNativeAd. */
@RunWith(AndroidJUnit4::class)
class PubMaticNativeAdTests {

  // Subject of testing
  private lateinit var pubMaticNativeAd: PubMaticNativeAd

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private val mediationNativeAdCallback = mock<MediationNativeAdCallback>()

  private val mediationAdLoadCallback =
    mock<MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback>> {
      on { onSuccess(any()) } doReturn mediationNativeAdCallback
    }

  private val pubMaticAdInfoIconView = View(context)

  private val pobNativeAd = mock<POBNativeAd>()

  private val pobNativeAdLoader = mock<POBNativeAdLoader>()

  private val adErrorCaptor = argumentCaptor<AdError>()

  private val pubMaticAdFactory =
    mock<PubMaticAdFactory> { on { createPOBNativeAdLoader(any()) } doReturn pobNativeAdLoader }

  private val mediationNativeAdConfiguration =
    MediationNativeAdConfiguration(
      context,
      "bid response",
      /*serverParameters = */ bundleOf(),
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      /*watermark=*/ "",
      /*nativeAdOptions=*/ null,
    )

  private lateinit var mockMobileAds: MockedStatic<MobileAds>

  @Before
  fun setUp() {
    mockMobileAds = mockStatic(MobileAds::class.java)
    whenever(MobileAds.getVersion()) doReturn VersionInfo(24, 4, 0)
    PubMaticNativeAd.newInstance(
        mediationNativeAdConfiguration,
        mediationAdLoadCallback,
        pubMaticAdFactory,
        Dispatchers.Unconfined,
      )
      .onSuccess { pubMaticNativeAd = it }
  }

  @After
  fun tearDown() {
    mockMobileAds.close()
  }

  @Test
  fun onAdReceived_mapsNativeAdAssetsAndInvokesLoadSuccessCallback_setsOverrideImpressionRecordingAsTrue() {
    val pobNativeAdTitleResponseAsset =
      mock<POBNativeAdTitleResponseAsset> { on { title } doReturn PUBMATIC_NATIVE_AD_TITLE }
    whenever(pobNativeAd.title) doReturn pobNativeAdTitleResponseAsset
    val pobNativeAdDescriptionAsset =
      mock<POBNativeAdDataResponseAsset> { on { value } doReturn PUBMATIC_NATIVE_AD_DESCRIPTION }
    whenever(pobNativeAd.description) doReturn pobNativeAdDescriptionAsset
    val pobNativeAdIconAsset =
      mock<POBNativeAdImageResponseAsset> { on { imageURL } doReturn PUBMATIC_AD_ICON_URL }
    whenever(pobNativeAd.icon) doReturn pobNativeAdIconAsset
    val pobNativeAdCallToActionAsset =
      mock<POBNativeAdDataResponseAsset> { on { value } doReturn PUBMATIC_CALL_TO_ACTION_STRING }
    whenever(pobNativeAd.callToAction) doReturn pobNativeAdCallToActionAsset
    val pobNativeAdvertiserAsset =
      mock<POBNativeAdDataResponseAsset> { on { value } doReturn PUBMATIC_ADVERTISER }
    whenever(pobNativeAd.advertiser) doReturn pobNativeAdvertiserAsset
    val pobNativeAdPriceAsset =
      mock<POBNativeAdDataResponseAsset> { on { value } doReturn PUBMATIC_AD_PRICE }
    whenever(pobNativeAd.price) doReturn pobNativeAdPriceAsset
    val pobNativeAdRatingAsset =
      mock<POBNativeAdDataResponseAsset> { on { value } doReturn PUBMATIC_AD_RATING_STRING }
    whenever(pobNativeAd.rating) doReturn pobNativeAdRatingAsset
    val pobNativeAdMainImageAsset =
      mock<POBNativeAdImageResponseAsset> { on { imageURL } doReturn PUBMATIC_AD_MAIN_IMAGE_URL }
    whenever(pobNativeAd.mainImage) doReturn pobNativeAdMainImageAsset
    whenever(pobNativeAd.adInfoIcon) doReturn pubMaticAdInfoIconView
    val mockRequestManager: RequestManager = mock<RequestManager>()
    val mockRequestBuilder: RequestBuilder<Drawable?> = mock<RequestBuilder<Drawable?>>()
    val mockFutureTarget: FutureTarget<Drawable?> = mock<FutureTarget<Drawable?>>()
    val mockDrawable = mock<Drawable>()
    mockStatic(Glide::class.java).use { mockedGlide ->
      whenever(Glide.with(context)) doReturn mockRequestManager
      whenever(mockRequestManager.asDrawable()) doReturn mockRequestBuilder
      whenever(mockRequestBuilder.load(PUBMATIC_AD_ICON_URL)) doReturn mockRequestBuilder
      whenever(mockRequestBuilder.load(PUBMATIC_AD_MAIN_IMAGE_URL)) doReturn mockRequestBuilder
      whenever(mockRequestBuilder.submit()) doReturn mockFutureTarget
      whenever(mockFutureTarget.get()) doReturn mockDrawable

      pubMaticNativeAd.onAdReceived(pobNativeAdLoader, pobNativeAd)
    }

    assertThat(pubMaticNativeAd.headline).isEqualTo(PUBMATIC_NATIVE_AD_TITLE)
    assertThat(pubMaticNativeAd.body).isEqualTo(PUBMATIC_NATIVE_AD_DESCRIPTION)
    assertThat(pubMaticNativeAd.icon.uri.toString()).isEqualTo(PUBMATIC_AD_ICON_URL)
    assertThat(pubMaticNativeAd.callToAction).isEqualTo(PUBMATIC_CALL_TO_ACTION_STRING)
    assertThat(pubMaticNativeAd.advertiser).isEqualTo(PUBMATIC_ADVERTISER)
    assertThat(pubMaticNativeAd.price).isEqualTo(PUBMATIC_AD_PRICE)
    assertThat(pubMaticNativeAd.starRating).isEqualTo(PUBMATIC_AD_RATING_STRING.toDouble())
    assertThat(pubMaticNativeAd.images[0].uri.toString()).isEqualTo(PUBMATIC_AD_MAIN_IMAGE_URL)
    val adChoicesContentLayout = pubMaticNativeAd.adChoicesContent as FrameLayout
    assertThat(adChoicesContentLayout.getChildAt(0)).isEqualTo(pubMaticAdInfoIconView)
    assertThat(pubMaticNativeAd.hasVideoContent()).isFalse()
    assertThat(pubMaticNativeAd.overrideClickHandling).isTrue()
    assertThat(pubMaticNativeAd.overrideImpressionRecording).isTrue()
    verify(mediationAdLoadCallback).onSuccess(pubMaticNativeAd)
  }

  @Test
  fun onAdReceived_mapsNativeAdAssetsAndInvokesLoadSuccessCallback_setsOverrideImpressionRecordingAsFalse() {
    val pobNativeAdTitleResponseAsset =
      mock<POBNativeAdTitleResponseAsset> { on { title } doReturn PUBMATIC_NATIVE_AD_TITLE }
    whenever(pobNativeAd.title) doReturn pobNativeAdTitleResponseAsset
    val pobNativeAdDescriptionAsset =
      mock<POBNativeAdDataResponseAsset> { on { value } doReturn PUBMATIC_NATIVE_AD_DESCRIPTION }
    whenever(pobNativeAd.description) doReturn pobNativeAdDescriptionAsset
    val pobNativeAdIconAsset =
      mock<POBNativeAdImageResponseAsset> { on { imageURL } doReturn PUBMATIC_AD_ICON_URL }
    whenever(pobNativeAd.icon) doReturn pobNativeAdIconAsset
    val pobNativeAdCallToActionAsset =
      mock<POBNativeAdDataResponseAsset> { on { value } doReturn PUBMATIC_CALL_TO_ACTION_STRING }
    whenever(pobNativeAd.callToAction) doReturn pobNativeAdCallToActionAsset
    val pobNativeAdvertiserAsset =
      mock<POBNativeAdDataResponseAsset> { on { value } doReturn PUBMATIC_ADVERTISER }
    whenever(pobNativeAd.advertiser) doReturn pobNativeAdvertiserAsset
    val pobNativeAdPriceAsset =
      mock<POBNativeAdDataResponseAsset> { on { value } doReturn PUBMATIC_AD_PRICE }
    whenever(pobNativeAd.price) doReturn pobNativeAdPriceAsset
    val pobNativeAdRatingAsset =
      mock<POBNativeAdDataResponseAsset> { on { value } doReturn PUBMATIC_AD_RATING_STRING }
    whenever(pobNativeAd.rating) doReturn pobNativeAdRatingAsset
    val pobNativeAdMainImageAsset =
      mock<POBNativeAdImageResponseAsset> { on { imageURL } doReturn PUBMATIC_AD_MAIN_IMAGE_URL }
    whenever(pobNativeAd.mainImage) doReturn pobNativeAdMainImageAsset
    whenever(pobNativeAd.adInfoIcon) doReturn pubMaticAdInfoIconView
    whenever(MobileAds.getVersion()) doReturn VersionInfo(24, 3, 0)

    pubMaticNativeAd.onAdReceived(pobNativeAdLoader, pobNativeAd)

    assertThat(pubMaticNativeAd.overrideImpressionRecording).isFalse()
  }

  @Test
  fun onFailedToLoad_invokesLoadFailureCallback() {
    val pobError = POBError(ERROR_PUBMATIC_AD_LOAD_FAILURE, "Ad load failed")

    pubMaticNativeAd.onFailedToLoad(pobNativeAdLoader, pobError)

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_PUBMATIC_AD_LOAD_FAILURE)
    assertThat(adError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onNativeAdImpression_reportsAdImpression() {
    // Call onAdReceived() to set pubMaticNativeAd.mediationNativeAdCallback
    pubMaticNativeAd.onAdReceived(pobNativeAdLoader, pobNativeAd)

    pubMaticNativeAd.onNativeAdImpression(pobNativeAd)

    verify(mediationNativeAdCallback).reportAdImpression()
  }

  @Test
  fun onNativeAdClicked_reportsAdClicked() {
    // Call onAdReceived() to set pubMaticNativeAd.mediationNativeAdCallback
    pubMaticNativeAd.onAdReceived(pobNativeAdLoader, pobNativeAd)

    pubMaticNativeAd.onNativeAdClicked(pobNativeAd)

    verify(mediationNativeAdCallback).reportAdClicked()
  }

  @Test
  fun onNativeAdLeavingApplication_reportsAdLeftApplication() {
    // Call onAdReceived() to set pubMaticNativeAd.mediationNativeAdCallback
    pubMaticNativeAd.onAdReceived(pobNativeAdLoader, pobNativeAd)

    pubMaticNativeAd.onNativeAdLeavingApplication(pobNativeAd)

    verify(mediationNativeAdCallback).onAdLeftApplication()
  }

  @Test
  fun onNativeAdOpened_reportsAdOpened() {
    // Call onAdReceived() to set pubMaticNativeAd.mediationNativeAdCallback
    pubMaticNativeAd.onAdReceived(pobNativeAdLoader, pobNativeAd)

    pubMaticNativeAd.onNativeAdOpened(pobNativeAd)

    verify(mediationNativeAdCallback).onAdOpened()
  }

  @Test
  fun onNativeAdClosed_reportsAdClosed() {
    // Call onAdReceived() to set pubMaticNativeAd.mediationNativeAdCallback
    pubMaticNativeAd.onAdReceived(pobNativeAdLoader, pobNativeAd)

    pubMaticNativeAd.onNativeAdClosed(pobNativeAd)

    verify(mediationNativeAdCallback).onAdClosed()
  }

  @Test
  fun onCallingMethodsNotMeantForSdkBidding_noInteractionWithNativeAdCallback() {
    pubMaticNativeAd.onNativeAdRendered(pobNativeAd)
    pubMaticNativeAd.onNativeAdRenderingFailed(
      pobNativeAd,
      POBError(1003, "Native ad rendering failed"),
    )
    pubMaticNativeAd.onNativeAdClicked(pobNativeAd, assetId = "anAssetId")

    verifyNoInteractions(mediationNativeAdCallback)
  }

  @Test
  fun trackViews_registersViewsForInteraction() {
    val containerView = View(context)
    val clickableAssetView1 = View(context)
    val clickableAssetView2 = View(context)
    // Call onAdReceived() to set pubMaticNativeAd.pobNativeAd
    pubMaticNativeAd.onAdReceived(pobNativeAdLoader, pobNativeAd)

    pubMaticNativeAd.trackViews(
      containerView,
      mapOf(
        "Clickable asset view 1" to clickableAssetView1,
        "Clickable asset view 2" to clickableAssetView2,
      ),
      emptyMap(),
    )

    verify(pobNativeAd)
      .registerViewForInteraction(
        containerView,
        listOf(clickableAssetView1, clickableAssetView2),
        pubMaticNativeAd,
      )
  }

  private companion object {
    const val ERROR_PUBMATIC_AD_LOAD_FAILURE = 1002
    const val PUBMATIC_NATIVE_AD_TITLE = "PubMatic native ad title"
    const val PUBMATIC_NATIVE_AD_DESCRIPTION = "PubMatic native ad description"
    const val PUBMATIC_AD_ICON_URL = "http://pubmatic.com/icon"
    const val PUBMATIC_CALL_TO_ACTION_STRING = "PubMatic call to action"
    const val PUBMATIC_ADVERTISER = "PubMatic advertiser"
    const val PUBMATIC_AD_PRICE = "$2"
    const val PUBMATIC_AD_RATING_STRING = "4.0"
    const val PUBMATIC_AD_MAIN_IMAGE_URL = "http://pubmatic.com/main-image"
  }
}
