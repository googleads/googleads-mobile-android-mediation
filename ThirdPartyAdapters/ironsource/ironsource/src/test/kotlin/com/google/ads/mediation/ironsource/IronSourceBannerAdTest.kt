package com.google.ads.mediation.ironsource

import android.app.Activity
import androidx.core.view.size
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.FakeMediationAdLoadCallback
import com.google.ads.mediation.adaptertestkit.FakeMediationBannerAdCallback
import com.google.ads.mediation.adaptertestkit.assertThat
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.ironsource.IronSourceBannerAd.getFromAvailableInstances
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.common.truth.Truth.assertThat
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSource.createBannerForDemandOnly
import com.ironsource.mediationsdk.demandOnly.ISDemandOnlyBannerLayout
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.logger.IronSourceError.ERROR_CODE_DECRYPT_FAILED
import com.ironsource.mediationsdk.logger.IronSourceError.ERROR_DO_BN_LOAD_ALREADY_IN_PROGRESS
import com.ironsource.mediationsdk.logger.IronSourceError.ERROR_DO_IS_LOAD_ALREADY_IN_PROGRESS
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

/** Tests for [IronSourceBannerAd]. */
@RunWith(AndroidJUnit4::class)
class IronSourceBannerAdTest {

  private lateinit var ironSourceBannerAd: IronSourceBannerAd

  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()

  private val bannerAdCallback = FakeMediationBannerAdCallback()
  private val bannerAdLoadCallback =
    FakeMediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>(bannerAdCallback)

  private val mediationUtils: MediationUtilsWrapper = mock()

  @After
  fun tearDown() {
    IronSourceBannerAd.removeFromAvailableInstances(/* instanceId= */ "0")
  }

  @Test
  fun onBannerAdLoaded_withValidBannerAd_expectOnSuccessCallback() {
    mockStatic(IronSource::class.java).use {
      val ironSourceBannerListener = loadBannerAd()

      ironSourceBannerListener.onBannerAdLoaded(/* instanceId= */ "0")

      assertThat(ironSourceBannerAd.ironSourceAdView.size).isEqualTo(1)
      assertThat(bannerAdLoadCallback).hasSucceededWith(ironSourceBannerAd)
    }
  }

  @Test
  fun onBannerAdLoaded_withoutBannerAd_expectNoCallback() {
    mockStatic(IronSource::class.java).use {
      val ironSourceBannerListener = loadBannerAd()
      IronSourceBannerAd.removeFromAvailableInstances(/* instanceId= */ "0")

      ironSourceBannerListener.onBannerAdLoaded(/* instanceId= */ "0")

      assertThat(ironSourceBannerAd.ironSourceAdView.size).isEqualTo(0)
      assertThat(bannerAdLoadCallback).hasNotSucceeded()
      assertThat(bannerAdLoadCallback).hasNoFailure()
    }
  }

  @Test
  fun onBannerAdLoadFailed_withValidBannerAd_expectOnFailureCallback() {
    mockStatic(IronSource::class.java).use {
      val ironSourceBannerListener = loadBannerAd()
      val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")

      ironSourceBannerListener.onBannerAdLoadFailed(/* instanceId= */ "0", ironSourceError)

      val expectedError =
        AdError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.", IRONSOURCE_SDK_ERROR_DOMAIN)
      assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
      assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isNull()
    }
  }

  @Test
  fun onBannerAdLoadFailed_withValidBannerAdStillLoading_expectOnFailureCallback() {
    mockStatic(IronSource::class.java).use {
      val ironSourceBannerListener = loadBannerAd()
      val ironSourceError = IronSourceError(ERROR_DO_IS_LOAD_ALREADY_IN_PROGRESS, "Still Loading.")

      ironSourceBannerListener.onBannerAdLoadFailed(/* instanceId= */ "0", ironSourceError)

      val expectedError =
        AdError(ERROR_DO_IS_LOAD_ALREADY_IN_PROGRESS, "Still Loading.", IRONSOURCE_SDK_ERROR_DOMAIN)
      assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
      assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isEqualTo(ironSourceBannerAd)
    }
  }

  @Test
  fun onBannerAdLoadFailed_withValidBannerAdStillBnLoading_expectOnFailureCallback() {
    mockStatic(IronSource::class.java).use {
      val ironSourceBannerListener = loadBannerAd()
      val ironSourceError = IronSourceError(ERROR_DO_BN_LOAD_ALREADY_IN_PROGRESS, "Still Loading.")

      ironSourceBannerListener.onBannerAdLoadFailed(/* instanceId= */ "0", ironSourceError)

      val expectedError =
        AdError(ERROR_DO_BN_LOAD_ALREADY_IN_PROGRESS, "Still Loading.", IRONSOURCE_SDK_ERROR_DOMAIN)
      assertThat(bannerAdLoadCallback).hasFailedWith(expectedError)
      assertThat(getFromAvailableInstances(/* instanceId= */ "0")).isEqualTo(ironSourceBannerAd)
    }
  }

  @Test
  fun onBannerAdLoadFailed_withoutBannerAd_expectNoCallbacks() {
    mockStatic(IronSource::class.java).use {
      val ironSourceBannerListener = loadBannerAd()
      val ironSourceError = IronSourceError(ERROR_CODE_DECRYPT_FAILED, "Decrypt failed.")
      IronSourceBannerAd.removeFromAvailableInstances(/* instanceId= */ "0")

      ironSourceBannerListener.onBannerAdLoadFailed(/* instanceId= */ "0", ironSourceError)

      assertThat(bannerAdLoadCallback).hasNotSucceeded()
      assertThat(bannerAdLoadCallback).hasNoFailure()
    }
  }

  @Test
  fun onBannerAdShown_withValidBannerAd_expectReportAdImpression() {
    mockStatic(IronSource::class.java).use {
      val ironSourceBannerListener = loadBannerAd()
      ironSourceBannerListener.onBannerAdLoaded(/* instanceId= */ "0")
      assertThat(bannerAdLoadCallback).hasSucceededWith(ironSourceBannerAd)

      ironSourceBannerListener.onBannerAdShown(/* instanceId= */ "0")

      assertThat(bannerAdCallback.isImpressionReported).isTrue()
    }
  }

  @Test
  fun onBannerAdClicked_withValidBannerAd_expectOnBannerAdClickedCallbacks() {
    mockStatic(IronSource::class.java).use {
      val ironSourceBannerListener = loadBannerAd()
      ironSourceBannerListener.onBannerAdLoaded(/* instanceId= */ "0")
      assertThat(bannerAdLoadCallback).hasSucceededWith(ironSourceBannerAd)

      ironSourceBannerListener.onBannerAdClicked(/* instanceId= */ "0")

      assertThat(bannerAdCallback.isOpened).isTrue()
      assertThat(bannerAdCallback.isClicked).isTrue()
    }
  }

  @Test
  fun onBannerAdLeftApplication_withValidBannerAd_expectOnAdLeftApplicationCallback() {
    mockStatic(IronSource::class.java).use {
      val ironSourceBannerListener = loadBannerAd()
      ironSourceBannerListener.onBannerAdLoaded(/* instanceId= */ "0")
      assertThat(bannerAdLoadCallback).hasSucceededWith(ironSourceBannerAd)

      ironSourceBannerListener.onBannerAdLeftApplication(/* instanceId= */ "0")

      assertThat(bannerAdCallback.isLeftApplication).isTrue()
    }
  }

  @Test
  fun onEventCallbacks_withoutBannerAd_expectNoEventCallbacks() {
    mockStatic(IronSource::class.java).use {
      val ironSourceBannerListener = loadBannerAd()
      ironSourceBannerListener.onBannerAdLoaded(/* instanceId= */ "0")
      assertThat(bannerAdLoadCallback).hasSucceededWith(ironSourceBannerAd)
      IronSourceBannerAd.removeFromAvailableInstances(/* instanceId= */ "0")

      ironSourceBannerListener.onBannerAdShown(/* instanceId= */ "0")
      ironSourceBannerListener.onBannerAdClicked(/* instanceId= */ "0")
      ironSourceBannerListener.onBannerAdLeftApplication(/* instanceId= */ "0")

      assertThat(bannerAdCallback.isImpressionReported).isFalse()
      assertThat(bannerAdCallback.isOpened).isFalse()
      assertThat(bannerAdCallback.isClicked).isFalse()
      assertThat(bannerAdCallback.isLeftApplication).isFalse()
    }
  }

  @Test
  fun onEventCallbacks_withoutBannerAdCallbackInstance_expectNoEventCallbacks() {
    mockStatic(IronSource::class.java).use {
      val ironSourceBannerListener = loadBannerAd()
      ironSourceBannerAd.setBannerAdCallback(null)

      ironSourceBannerListener.onBannerAdShown(/* instanceId= */ "0")
      ironSourceBannerListener.onBannerAdClicked(/* instanceId= */ "0")
      ironSourceBannerListener.onBannerAdLeftApplication(/* instanceId= */ "0")

      assertThat(bannerAdCallback.isImpressionReported).isFalse()
      assertThat(bannerAdCallback.isOpened).isFalse()
      assertThat(bannerAdCallback.isClicked).isFalse()
      assertThat(bannerAdCallback.isLeftApplication).isFalse()
    }
  }

  private fun loadBannerAd(): IronSourceBannerAdListener {
    val mediationAdConfiguration = createMediationBannerAdConfiguration(activity)
    whenever(mediationUtils.findClosestSize(eq(activity), eq(AdSize.BANNER), any())) doReturn
      AdSize.BANNER
    ironSourceBannerAd = IronSourceBannerAd(bannerAdLoadCallback)
    val mockIronSourceBannerLayout = mock<ISDemandOnlyBannerLayout>()
    whenever(createBannerForDemandOnly(any(), any())) doReturn mockIronSourceBannerLayout
    val argumentCaptor = argumentCaptor<IronSourceBannerAdListener>()
    ironSourceBannerAd.loadAd(mediationAdConfiguration, mediationUtils)
    verify(mockIronSourceBannerLayout).bannerDemandOnlyListener = argumentCaptor.capture()
    return argumentCaptor.firstValue
  }
}
