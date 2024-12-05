import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.ironsource.IronSourceMediationAdapter.IRONSOURCE_SDK_ERROR_DOMAIN
import com.google.ads.mediation.ironsource.IronSourceRewardItem
import com.google.ads.mediation.ironsource.IronSourceRtbRewardedAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.ironsource.mediationsdk.logger.IronSourceError
import com.unity3d.ironsourceads.rewarded.RewardedAd
import com.unity3d.ironsourceads.rewarded.RewardedAdLoader
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IronSourceRtbRewardedAdTest {
  private lateinit var ironSourceRtbRewardedAd: IronSourceRtbRewardedAd

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val bundle = Bundle().apply { putString("instanceId", "mockInstanceId") }

  private val mockRewardedAdConfig: MediationRewardedAdConfiguration = mock {
    on { context } doReturn context
    on { serverParameters } doReturn bundle
    on { bidResponse } doReturn TEST_BID_RESPONSE
    on { watermark } doReturn TEST_WATERMARK
  }
  private val mockMediationRewardedAdCallback: MediationRewardedAdCallback = mock()
  private val mockMediationAdLoadCallback:
    MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockMediationRewardedAdCallback
    }
  private val mockRewardedAd: RewardedAd = mock()
  private val mockRewardedAdLoader = mockStatic(RewardedAdLoader::class.java)

  @Before
  fun setUp() {
    ironSourceRtbRewardedAd =
      IronSourceRtbRewardedAd(mockRewardedAdConfig, mockMediationAdLoadCallback)
  }

  @After
  fun tearDown() {
    mockRewardedAdLoader.close()
  }

  @Test
  fun onLoadRtbAd_verifyOnSuccessCallback() {
    // When
    ironSourceRtbRewardedAd.loadRtbAd()
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // Then
    verify(mockMediationAdLoadCallback).onSuccess(ironSourceRtbRewardedAd)
    verifyNoMoreInteractions(mockMediationAdLoadCallback)
  }

  @Test
  fun onRewardedVideoAdLoadSuccess_verifyOnSuccessCallback() {
    // When
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // Then
    verify(mockMediationAdLoadCallback).onSuccess(ironSourceRtbRewardedAd)
    verifyNoMoreInteractions(mockMediationAdLoadCallback)
  }

  @Test
  fun onRewardedAdLoadFailed_verifyOnFailureCallback() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd()
    val errorRes = "An error occurred"
    val errorCode = 123
    val ironSourceError = IronSourceError(errorCode, errorRes)

    // When
    ironSourceRtbRewardedAd.onRewardedAdLoadFailed(ironSourceError)

    // Then
    val expectedAdError = AdError(errorCode, errorRes, "com.ironsource.mediationsdk")
    verify(mockMediationAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun showAd_verifyShowAdInvoked() {
    // Given
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)
    val activity = Robolectric.buildActivity(Activity::class.java).get()

    // When
    ironSourceRtbRewardedAd.showAd(activity)

    // Then
    verify(mockRewardedAd).show(activity)
  }

  @Test
  fun onRewardedAdShowFailed_verifyOnAdFailedToShow() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd()
    val errorRes = "An error occurred"
    val errorCode = 123
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)
    val ironSourceError = IronSourceError(errorCode, errorRes)

    // When
    ironSourceRtbRewardedAd.onRewardedAdFailedToShow(mockRewardedAd, ironSourceError)

    // Then
    val expectedAdError = AdError(errorCode, errorRes, IRONSOURCE_SDK_ERROR_DOMAIN)
    verify(mockMediationRewardedAdCallback)
      .onAdFailedToShow(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun showAd_invalidContext_expectObFailureCallbackWithError() {
    // given
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // when
    ironSourceRtbRewardedAd.showAd(context)

    // then
    val captor = argumentCaptor<AdError>()
    verify(mockMediationRewardedAdCallback).onAdFailedToShow(captor.capture())
    val capturedError = captor.firstValue
    assertEquals(102, capturedError.code)
    assertEquals("IronSource requires an Activity context to load ads.", capturedError.message)
    assertEquals("com.google.ads.mediation.ironsource", capturedError.domain)
  }

  @Test
  fun onRewardedAdShowFailed_withoutRewardedAdCallbackInstance_verifyOnAdFailedToShow() {
    // Given
    doReturn(null).whenever(mockMediationAdLoadCallback).onSuccess(any())
    val errorRes = "An error occurred"
    val errorCode = 123
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)
    val ironSourceError = IronSourceError(errorCode, errorRes)

    // When
    ironSourceRtbRewardedAd.onRewardedAdFailedToShow(mockRewardedAd, ironSourceError)

    // Then
    verifyNoInteractions(mockMediationRewardedAdCallback)
  }

  @Test
  fun onRewardedAdOpened_withRewardedAd_verifyOnRewardedAdOpenedCallbacks() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd()
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // When
    ironSourceRtbRewardedAd.onRewardedAdShown(mockRewardedAd)

    // Then
    verify(mockMediationRewardedAdCallback).onAdOpened()
    verify(mockMediationRewardedAdCallback).reportAdImpression()
  }

  @Test
  fun onRewardedAdClosed_withRewardedAd_verifyOnAdClosedCallback() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd()
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // When
    ironSourceRtbRewardedAd.onRewardedAdDismissed(mockRewardedAd)

    // Then
    verify(mockMediationRewardedAdCallback).onAdClosed()
  }

  @Test
  fun onRewardedVideoAdRewarded_withRewardedAd_verifyOnRewardedCallbacks() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd()
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // When
    ironSourceRtbRewardedAd.onUserEarnedReward(mockRewardedAd)

    // Then
    verify(mockMediationRewardedAdCallback).onVideoComplete()
    verify(mockMediationRewardedAdCallback).onUserEarnedReward(any<IronSourceRewardItem>())
  }

  @Test
  fun onRewardedAdClicked_withRewardedAd_verifyReportAdClickedCallback() {
    // Given
    ironSourceRtbRewardedAd.loadRtbAd()
    ironSourceRtbRewardedAd.onRewardedAdLoaded(mockRewardedAd)

    // When
    ironSourceRtbRewardedAd.onRewardedAdClicked(mockRewardedAd)

    // Then
    verify(mockMediationRewardedAdCallback).reportAdClicked()
  }

  @Test
  fun onAdEvents_withoutRewardedAd_verifyNoCallbacks() {
    // Given
    clearInvocations(mockMediationRewardedAdCallback)

    // When
    ironSourceRtbRewardedAd.onRewardedAdShown(mockRewardedAd)
    ironSourceRtbRewardedAd.onRewardedAdDismissed(mockRewardedAd)
    ironSourceRtbRewardedAd.onRewardedAdClicked(mockRewardedAd)

    // Then
    verifyNoInteractions(mockMediationRewardedAdCallback)
  }
}
