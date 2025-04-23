package com.google.ads.mediation.pubmatic

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ADAPTER_ERROR_DOMAIN
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.ERROR_AD_NOT_READY
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.core.POBReward
import com.pubmatic.sdk.rewardedad.POBRewardedAd
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for PubMaticRewardedAd. */
@RunWith(AndroidJUnit4::class)
class PubMaticRewardedAdTests {

  // Subject of testing
  private lateinit var pubMaticRewardedAd: PubMaticRewardedAd

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private val mediationRewardedAdCallback = mock<MediationRewardedAdCallback>()

  private val mediationAdLoadCallback =
    mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>> {
      on { onSuccess(any()) } doReturn mediationRewardedAdCallback
    }

  private val pobRewardedAd = mock<POBRewardedAd>()

  private val adErrorCaptor = argumentCaptor<AdError>()

  private val pubMaticAdFactory =
    mock<PubMaticAdFactory> { on { createPOBRewardedAd(any()) } doReturn pobRewardedAd }

  private val mediationRewardedAdConfiguration =
    MediationRewardedAdConfiguration(
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
    )

  @Before
  fun setUp() {
    PubMaticRewardedAd.newInstance(
        mediationRewardedAdConfiguration,
        mediationAdLoadCallback,
        pubMaticAdFactory,
      )
      .onSuccess { pubMaticRewardedAd = it }
  }

  @Test
  fun onAdReceived_invokesLoadSuccessCallback() {
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    verify(mediationAdLoadCallback).onSuccess(pubMaticRewardedAd)
  }

  @Test
  fun onAdFailedToLoad_invokesLoadFailureCallback() {
    val pobError = POBError(ERROR_PUBMATIC_AD_LOAD_FAILURE, "Ad load failed")

    pubMaticRewardedAd.onAdFailedToLoad(pobRewardedAd, pobError)

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_PUBMATIC_AD_LOAD_FAILURE)
    assertThat(adError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun showAd_ifAdIsReady_showsPubMaticAd() {
    whenever(pobRewardedAd.isReady).thenReturn(true)

    pubMaticRewardedAd.showAd(context)

    verify(pobRewardedAd).show()
  }

  @Test
  fun showAd_ifAdIsNotReady_invokesAdShowFailureCallback() {
    whenever(pobRewardedAd.isReady).thenReturn(false)
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    pubMaticRewardedAd.showAd(context)

    verify(mediationRewardedAdCallback).onAdFailedToShow(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_AD_NOT_READY)
    assertThat(adError.domain).isEqualTo(ADAPTER_ERROR_DOMAIN)
  }

  @Test
  fun onAdFailedToShow_invokesAdShowFailureCallback() {
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)
    val pobError = POBError(ERROR_PUBMATIC_AD_SHOW_FAILURE, "Ad show failed")

    pubMaticRewardedAd.onAdFailedToShow(pobRewardedAd, pobError)

    verify(mediationRewardedAdCallback).onAdFailedToShow(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_PUBMATIC_AD_SHOW_FAILURE)
    assertThat(adError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onAdImpression_reportsAdImpression() {
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    pubMaticRewardedAd.onAdImpression(pobRewardedAd)

    verify(mediationRewardedAdCallback).reportAdImpression()
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    pubMaticRewardedAd.onAdClicked(pobRewardedAd)

    verify(mediationRewardedAdCallback).reportAdClicked()
  }

  @Test
  fun onAdOpened_reportsAdOpened() {
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    pubMaticRewardedAd.onAdOpened(pobRewardedAd)

    verify(mediationRewardedAdCallback).onAdOpened()
  }

  @Test
  fun onAdClosed_reportsAdClosed() {
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    pubMaticRewardedAd.onAdClosed(pobRewardedAd)

    verify(mediationRewardedAdCallback).onAdClosed()
  }

  @Test
  fun onReceiveReward_reportsUserEarnedReward() {
    // Call onAdReceived() to set pubMaticRewardedAd.mediationRewardedAdCallback
    pubMaticRewardedAd.onAdReceived(pobRewardedAd)

    pubMaticRewardedAd.onReceiveReward(pobRewardedAd, POBReward("USD", 1))

    verify(mediationRewardedAdCallback).onUserEarnedReward()
  }

  private companion object {
    const val ERROR_PUBMATIC_AD_LOAD_FAILURE = 1002
    const val ERROR_PUBMATIC_AD_SHOW_FAILURE = 1003
  }
}
