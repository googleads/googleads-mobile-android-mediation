import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.google.ads.mediation.ironsource.IronSourceRewardItem
import com.google.ads.mediation.ironsource.IronSourceRtbRewardedAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.ironsource.mediationsdk.logger.IronSourceError
import com.unity3d.ironsourceads.rewarded.RewardedAdLoader
import com.unity3d.ironsourceads.rewarded.RewardedAdLoaderListener
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class IronSourceRtbRewardedAdTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var rewardedAdConfig: MediationRewardedAdConfiguration

    @Mock
    private lateinit var mediationAdLoadCallback: MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>

    @Mock
    private lateinit var mediationRewardedAdCallback: MediationRewardedAdCallback

    @Mock
    private lateinit var rewardedAd: com.unity3d.ironsourceads.rewarded.RewardedAd

    private lateinit var ironSourceRtbRewardedAd: IronSourceRtbRewardedAd
    private lateinit var mockedRewardedAdLoader: MockedStatic<RewardedAdLoader>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val bundleMock = mock(Bundle::class.java)
        whenever(bundleMock.getString("instanceId", "")).thenReturn("mockInstanceId")
        whenever(rewardedAdConfig.context).thenReturn(context)
        whenever(rewardedAdConfig.serverParameters).thenReturn(bundleMock)
        whenever(rewardedAdConfig.bidResponse).thenReturn("sampleBidToken")
        whenever(rewardedAdConfig.watermark).thenReturn("sampleWatermark")
        whenever(mediationAdLoadCallback.onSuccess(any())).thenReturn(mediationRewardedAdCallback)

        mockedRewardedAdLoader = mockStatic(RewardedAdLoader::class.java)
        mockedRewardedAdLoader.`when`<Unit> {
            RewardedAdLoader.loadAd(any(), any())
        }.then { invocation ->
            val listener = invocation.arguments[1] as RewardedAdLoaderListener
            listener.onRewardedAdLoaded(rewardedAd)
        }

        ironSourceRtbRewardedAd = IronSourceRtbRewardedAd(rewardedAdConfig, mediationAdLoadCallback)
    }

    @After
    fun tearDown() {
        mockedRewardedAdLoader.close()
        reset(
            context,
            rewardedAdConfig,
            mediationAdLoadCallback,
            mediationRewardedAdCallback,
            rewardedAd
        )
    }

    @Test
    fun onLoadRtbAd_verifyOnSuccessCallback() {
        // When
        ironSourceRtbRewardedAd.loadRtbAd()

        // Then
        verify(mediationAdLoadCallback).onSuccess(ironSourceRtbRewardedAd)
        verifyNoMoreInteractions(mediationAdLoadCallback)
    }

    @Test
    fun onRewardedVideoAdLoadSuccess_verifyOnSuccessCallback() {
        // When
        ironSourceRtbRewardedAd.onRewardedAdLoaded(rewardedAd)

        // Then
        verify(mediationAdLoadCallback).onSuccess(ironSourceRtbRewardedAd)
        verifyNoMoreInteractions(mediationAdLoadCallback)
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
        val captor = argumentCaptor<AdError>()
        verify(mediationAdLoadCallback).onFailure(captor.capture())
        val capturedError = captor.firstValue
        assertEquals(errorCode, capturedError.code)
        assertEquals(errorRes, capturedError.message)
        assertEquals("com.google.ads.mediation.ironsource", capturedError.domain)
    }

    @Test
    fun showAd_verifyShowAdInvoked() {
        // Given
        `when`(rewardedAdConfig.context).thenReturn(mock(Activity::class.java))
        ironSourceRtbRewardedAd.onRewardedAdLoaded(rewardedAd)
        val mockActivity = mock(Activity::class.java)

        // When
        ironSourceRtbRewardedAd.showAd(mockActivity)

        // Then
        verify(rewardedAd).show(mockActivity)
    }

    @Test
    fun onRewardedAdShowFailed_verifyOnAdFailedToShow() {
        // Given
        ironSourceRtbRewardedAd.loadRtbAd()
        val errorRes = "An error occurred"
        val errorCode = 123
        ironSourceRtbRewardedAd.onRewardedAdLoaded(rewardedAd)
        val ironSourceError = IronSourceError(errorCode, errorRes)
        val captor = argumentCaptor<AdError>()

        // When
        ironSourceRtbRewardedAd.onRewardedAdFailedToShow(rewardedAd, ironSourceError)

        // Then
        verify(mediationRewardedAdCallback).onAdFailedToShow(captor.capture())
        val capturedError = captor.firstValue
        assertEquals(errorCode, capturedError.code)
        assertEquals(errorRes, capturedError.message)
        assertEquals("com.google.ads.mediation.ironsource", capturedError.domain)
    }

    @Test
    fun showAd_invalidContext_expectObFailureCallbackWithError() {
        // given
        whenever(rewardedAdConfig.context).thenReturn(mock(Activity::class.java))
        ironSourceRtbRewardedAd.onRewardedAdLoaded(rewardedAd)
        val nonActivityContext = mock(Context::class.java)

        // when
        ironSourceRtbRewardedAd.showAd(nonActivityContext)

        // then
        val captor = argumentCaptor<AdError>()
        verify(mediationRewardedAdCallback).onAdFailedToShow(captor.capture())
        val capturedError = captor.firstValue
        assertEquals(102, capturedError.code)
        assertEquals("IronSource requires an Activity context to load ads.", capturedError.message)
        assertEquals("com.google.ads.mediation.ironsource", capturedError.domain)
    }

    @Test
    fun onRewardedAdShowFailed_withoutRewardedAdCallbackInstance_verifyOnAdFailedToShow() {
        // Given
        whenever(mediationAdLoadCallback.onSuccess(any())).thenReturn(null)
        val errorRes = "An error occurred"
        val errorCode = 123
        ironSourceRtbRewardedAd.onRewardedAdLoaded(rewardedAd)
        val ironSourceError = IronSourceError(errorCode, errorRes)

        // When
        ironSourceRtbRewardedAd.onRewardedAdFailedToShow(rewardedAd, ironSourceError)

        // Then
        verifyNoInteractions(mediationRewardedAdCallback)
    }

    @Test
    fun onRewardedAdOpened_withRewardedAd_verifyOnRewardedAdOpenedCallbacks() {
        // Given
        ironSourceRtbRewardedAd.loadRtbAd()
        ironSourceRtbRewardedAd.onRewardedAdLoaded(rewardedAd)

        // When
        ironSourceRtbRewardedAd.onRewardedAdShown(rewardedAd)

        // Then
        verify(mediationRewardedAdCallback).onAdOpened()
        verify(mediationRewardedAdCallback).reportAdImpression()
    }

    @Test
    fun onRewardedAdClosed_withRewardedAd_verifyOnAdClosedCallback() {
        // Given
        ironSourceRtbRewardedAd.loadRtbAd()

        // When
        ironSourceRtbRewardedAd.onRewardedAdDismissed(rewardedAd)

        // Then
        verify(mediationRewardedAdCallback).onAdClosed()
    }

    fun onRewardedVideoAdRewarded_withRewardedAd_verifyOnRewardedCallbacks() {
        // Given
        ironSourceRtbRewardedAd.loadRtbAd()

        // When
        ironSourceRtbRewardedAd.onUserEarnedReward(rewardedAd);

        // Then
        verify(mediationRewardedAdCallback).onVideoComplete();
        verify(mediationRewardedAdCallback).onUserEarnedReward(any<IronSourceRewardItem>());
    }

    @Test
    fun onRewardedAdClicked_withRewardedAd_verifyReportAdClickedCallback() {
        // Given
        ironSourceRtbRewardedAd.loadRtbAd()

        // When
        ironSourceRtbRewardedAd.onRewardedAdClicked(rewardedAd)

        // Then
        verify(mediationRewardedAdCallback).reportAdClicked()
    }

    fun onAdEvents_withoutRewardedAd_verifyNoCallbacks() {
        // Given
        clearInvocations(mediationRewardedAdCallback)

        // When
        ironSourceRtbRewardedAd.onRewardedAdShown(rewardedAd)
        ironSourceRtbRewardedAd.onRewardedAdDismissed(rewardedAd)
        ironSourceRtbRewardedAd.onRewardedAdClicked(rewardedAd)

        // Then
        verifyNoInteractions(mediationRewardedAdCallback)
    }
}
