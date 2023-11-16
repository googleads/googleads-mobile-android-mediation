package com.google.ads.mediation.facebook.rtb

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.facebook.FacebookMediationAdapter
import com.google.ads.mediation.facebook.MetaFactory
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/** Unit tests for [FacebookRtbBannerAd]. */
@RunWith(AndroidJUnit4::class)
class FacebookRtbBannerAdTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val mediationBannerAdCallback = mock<MediationBannerAdCallback>()
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mediationBannerAdCallback
    }
  private val metaFactory: MetaFactory = mock()
  private val metaAd: Ad = mock()

  /** The unit under test. */
  private lateinit var adapterBannerAd: FacebookRtbBannerAd

  @Before
  fun setup() {
    val serverParameters =
      bundleOf(
        FacebookMediationAdapter.RTB_PLACEMENT_PARAMETER to
          AdapterTestKitConstants.TEST_PLACEMENT_ID
      )
    val mediationBannerAdConfiguration =
      createMediationBannerAdConfiguration(context = context, serverParameters = serverParameters)

    adapterBannerAd =
      FacebookRtbBannerAd(mediationBannerAdConfiguration, mediationAdLoadCallback, metaFactory)
  }

  @Test
  fun onError_invokesOnFailureCallback() {
    val metaAdError = AdError(101, "Error from meta")
    val expectedAdError =
      com.google.android.gms.ads.AdError(
        metaAdError.errorCode,
        metaAdError.errorMessage,
        FacebookMediationAdapter.FACEBOOK_SDK_ERROR_DOMAIN
      )

    // invoke onError callback
    adapterBannerAd.onError(metaAd, metaAdError)

    verify(mediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun onAdLoaded_invokesOnSuccessCallback() {
    // invoke onAdLoaded callback
    adapterBannerAd.onAdLoaded(metaAd)

    verify(mediationAdLoadCallback).onSuccess(adapterBannerAd)
  }

  @Test
  fun onAdClick_invokesOnAdOpenedOnAdLeftApplicationAndReportAdClickedCallbacks() {
    // simulate successful ad load
    adapterBannerAd.onAdLoaded(metaAd)

    // invoke onAdClicked callback
    adapterBannerAd.onAdClicked(metaAd)

    verify(mediationBannerAdCallback).onAdOpened()
    verify(mediationBannerAdCallback).onAdLeftApplication()
    verify(mediationBannerAdCallback).reportAdClicked()
  }

  @Test
  fun onLoggingImpressions_invokesReportAdImpressionCallback() {
    // simulate successful ad load
    adapterBannerAd.onAdLoaded(metaAd)

    // invoke onLoggingImpression callback
    adapterBannerAd.onLoggingImpression(metaAd)

    verify(mediationBannerAdCallback).reportAdImpression()
  }
}
