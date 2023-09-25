package com.google.ads.mediation.facebook.rtb

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.ads.Ad
import com.facebook.ads.AdError.AD_PRESENTATION_ERROR
import com.facebook.ads.InterstitialAd
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_AD_UNIT
import com.google.ads.mediation.adaptertestkit.createMediationInterstitialAdConfiguration
import com.google.ads.mediation.facebook.FacebookMediationAdapter.FACEBOOK_SDK_ERROR_DOMAIN
import com.google.ads.mediation.facebook.FacebookMediationAdapter.RTB_PLACEMENT_PARAMETER
import com.google.ads.mediation.facebook.MetaFactory
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for [FacebookRtbInterstitialAd]. */
@RunWith(AndroidJUnit4::class)
class FacebookRtbInterstitialAdTest {

  /** The unit under test. */
  private lateinit var adapterInterstitialAd: FacebookRtbInterstitialAd

  private lateinit var mediationInterstitialAdConfig: MediationInterstitialAdConfiguration
  private val serverParameters = Bundle()
  private val context: Context = ApplicationProvider.getApplicationContext()
  private val mediationInterstitialAdCallback: MediationInterstitialAdCallback = mock()
  private val mediationAdLoadCallback:
    MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mediationInterstitialAdCallback
    }
  private val metaInterstitialAdLoadConfigBuilder: InterstitialAd.InterstitialAdLoadConfigBuilder =
    mock {
      on { withBid(any()) } doReturn this.mock
      on { withAdListener(any()) } doReturn this.mock
    }
  private val metaInterstitialAd: InterstitialAd = mock {
    on { buildLoadAdConfig() } doReturn metaInterstitialAdLoadConfigBuilder
  }
  private val metaFactory: MetaFactory = mock {
    on { createInterstitialAd(any(), any()) } doReturn metaInterstitialAd
  }
  private val metaAd: Ad = mock()

  @Before
  fun setUp() {
    serverParameters.putString(RTB_PLACEMENT_PARAMETER, TEST_AD_UNIT)
    mediationInterstitialAdConfig =
      createMediationInterstitialAdConfiguration(context, serverParameters = serverParameters)
    adapterInterstitialAd =
      FacebookRtbInterstitialAd(mediationInterstitialAdConfig, mediationAdLoadCallback, metaFactory)
  }

  @Test
  fun onError_ifMetaReportsShowError_callsOnAdFailedToShow() {
    // As part of setting this test up, render the ad.
    adapterInterstitialAd.render()
    // Simulate Meta ad load success.
    adapterInterstitialAd.onAdLoaded(metaAd)
    // Stub metaInterstitialAd.show() to return true (i.e. we are able to successfully ask Meta's
    // interstitial ad to be shown).
    whenever(metaInterstitialAd.show()) doReturn true
    // As part of setting this test up, show the ad.
    adapterInterstitialAd.showAd(context)

    // Simulate Meta reporting a show error.
    adapterInterstitialAd.onError(metaAd, AD_PRESENTATION_ERROR)

    val expectedAdError =
      AdError(
        AD_PRESENTATION_ERROR.errorCode,
        AD_PRESENTATION_ERROR.errorMessage,
        FACEBOOK_SDK_ERROR_DOMAIN
      )
    verify(mediationInterstitialAdCallback)
      .onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }
}
