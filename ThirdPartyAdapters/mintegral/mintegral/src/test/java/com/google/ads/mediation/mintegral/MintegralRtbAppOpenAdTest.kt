package com.google.ads.mediation.mintegral

import android.app.Activity
import androidx.core.os.bundleOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_AD_UNIT
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_PLACEMENT_ID
import com.google.ads.mediation.adaptertestkit.createMediationAppOpenAdConfiguration
import com.google.ads.mediation.mintegral.MintegralConstants.AD_UNIT_ID
import com.google.ads.mediation.mintegral.MintegralConstants.PLACEMENT_ID
import com.google.ads.mediation.mintegral.rtb.MintegralRtbAppOpenAd
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationAppOpenAd
import com.google.android.gms.ads.mediation.MediationAppOpenAdCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class MintegralRtbAppOpenAdTest {
  // Subject under testing
  private lateinit var mintegralAppOpenAd: MintegralRtbAppOpenAd

  private val activity = Robolectric.buildActivity(Activity::class.java).get()
  private val serverParameters =
    bundleOf(AD_UNIT_ID to TEST_AD_UNIT, PLACEMENT_ID to TEST_PLACEMENT_ID)
  private val mockSplashAdWrapper: MintegralSplashAdWrapper = mock()
  private val mockAdCallback: MediationAppOpenAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<MediationAppOpenAd, MediationAppOpenAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockAdCallback
    }

  @Before
  fun setUp() {
    mintegralAppOpenAd = MintegralRtbAppOpenAd(mockAdLoadCallback)
  }

  @Test
  fun showAd_invokesSplashAdShowWithLayoutAndBidToken() {
    Mockito.mockStatic(MintegralFactory::class.java).use {
      whenever(MintegralFactory.createSplashAdWrapper()) doReturn mockSplashAdWrapper
      mintegralAppOpenAd.loadAd(
        createMediationAppOpenAdConfiguration(
          context = activity,
          serverParameters = serverParameters,
          bidResponse = TEST_BID_RESPONSE,
        )
      )

      mintegralAppOpenAd.showAd(activity)

      verify(mockSplashAdWrapper).show(any(), eq(TEST_BID_RESPONSE))
    }
  }
}
