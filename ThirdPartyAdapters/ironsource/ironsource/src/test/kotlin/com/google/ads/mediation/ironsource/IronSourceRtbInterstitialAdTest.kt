import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.google.ads.mediation.ironsource.IronSourceRtbInterstitialAd
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.MediationInterstitialAd
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration
import com.ironsource.mediationsdk.logger.IronSourceError
import com.unity3d.ironsourceads.interstitial.InterstitialAd
import com.unity3d.ironsourceads.interstitial.InterstitialAdLoader
import com.unity3d.ironsourceads.interstitial.InterstitialAdRequest
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class IronSourceRtbInterstitialAdTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var interstitialAdConfig: MediationInterstitialAdConfiguration

    @Mock
    private lateinit var mediationAdLoadCallback: MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback>

    @Mock
    private lateinit var mediationInterstitialAdCallback: MediationInterstitialAdCallback

    @Mock
    private lateinit var interstitialAd: InterstitialAd

    private lateinit var ironSourceRtbInterstitialAd: IronSourceRtbInterstitialAd
    private lateinit var mockedInterstitialAdLoader: MockedStatic<InterstitialAdLoader>

    private lateinit var mediationConfigurations: List<MediationConfiguration>
    private lateinit var initializationCompleteCallback: InitializationCompleteCallback

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val mockMediationConfiguration = mock(MediationConfiguration::class.java)
        mediationConfigurations = listOf(mockMediationConfiguration)
        initializationCompleteCallback = mock(InitializationCompleteCallback::class.java)

        mockedInterstitialAdLoader = Mockito.mockStatic(InterstitialAdLoader::class.java)
        whenever(InterstitialAdLoader.loadAd(any(), any())).then { }

        val bundleMock = mock(Bundle::class.java)
        whenever(bundleMock.getString("instanceId", "")).thenReturn("mockInstanceId")

        whenever(interstitialAdConfig.context).thenReturn(context)
        whenever(interstitialAdConfig.serverParameters).thenReturn(bundleMock)
        whenever(interstitialAdConfig.getBidResponse()).thenReturn("sampleBidToken")
        whenever(interstitialAdConfig.getWatermark()).thenReturn("sampleWatermark")
        whenever(mediationAdLoadCallback.onSuccess(any())).thenReturn(
            mediationInterstitialAdCallback
        )

        ironSourceRtbInterstitialAd =
            IronSourceRtbInterstitialAd(interstitialAdConfig, mediationAdLoadCallback)

        context = mock(Context::class.java)
        initializationCompleteCallback = mock(InitializationCompleteCallback::class.java)
        mediationConfigurations = listOf(mockMediationConfiguration)

        // Mock the behavior of serverParameters
        val mockBundle = mock(Bundle::class.java)
        whenever(mockBundle.getString("APP_KEY")).thenReturn("validAppKey")
        whenever(mockMediationConfiguration.serverParameters).thenReturn(mockBundle)
    }

    @After
    fun tearDown() {
        mockedInterstitialAdLoader.close()
        Mockito.reset(
            context,
            interstitialAdConfig,
            mediationAdLoadCallback,
            mediationInterstitialAdCallback,
            interstitialAd
        )
    }

    @Test
    fun testLoadRtbAd_Success() {
        // given

        // when
        ironSourceRtbInterstitialAd.loadRtbAd()
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)

        // then
        verify(mediationAdLoadCallback).onSuccess(ironSourceRtbInterstitialAd)
        verifyNoMoreInteractions(mediationAdLoadCallback)
    }

    @Test
    fun testOnInterstitialAdLoaded() {
        // given

        // when
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)

        // then
        verify(mediationAdLoadCallback).onSuccess(ironSourceRtbInterstitialAd)
        verifyNoMoreInteractions(mediationAdLoadCallback)
    }

    @Test
    fun onInterstitialAdLoadFailed_verifyOnFailureCallback() {
        // given
        ironSourceRtbInterstitialAd.loadRtbAd()
        val ironSourceError = IronSourceError(123, "An error occurred")

        // when
        ironSourceRtbInterstitialAd.onInterstitialAdLoadFailed(ironSourceError)

        // then
        val captor = argumentCaptor<AdError>()
        verify(mediationAdLoadCallback).onFailure(captor.capture())
        val capturedError = captor.firstValue
        assertEquals(123, capturedError.code)
        assertEquals("An error occurred", capturedError.message)
        assertEquals("com.google.ads.mediation.ironsource", capturedError.domain)
    }


    @Test
    fun showAd_verifyShowAdInvoked() {
        // given
        whenever(interstitialAdConfig.context).thenReturn(mock(Activity::class.java))
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)
        val mockActivity = mock(Activity::class.java)

        // when
        ironSourceRtbInterstitialAd.showAd(mockActivity)

        // then
        verify(interstitialAd).show(mockActivity)
    }

    @Test
    fun showAd_invalidContext_expectObFailureCallbackWithError() {
        // given
        whenever(interstitialAdConfig.context).thenReturn(mock(Activity::class.java))
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)
        val nonActivityContext = mock(Context::class.java)

        // when
        ironSourceRtbInterstitialAd.showAd(nonActivityContext)

        // then
        val captor = argumentCaptor<AdError>()
        verify(mediationInterstitialAdCallback).onAdFailedToShow(captor.capture())
        val capturedError = captor.firstValue
        assertEquals(102, capturedError.code)
        assertEquals("IronSource requires an Activity context to load ads.", capturedError.message)
        assertEquals("com.google.ads.mediation.ironsource", capturedError.domain)
    }

    @Test
    fun onInterstitialAdShowFailed_verifyOnAdFailedToShow() {
        // given
        ironSourceRtbInterstitialAd.loadRtbAd()
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)
        val ironSourceError = IronSourceError(123, "An error occurred")

        // when
        ironSourceRtbInterstitialAd.onInterstitialAdFailedToShow(interstitialAd, ironSourceError)

        // then
        val captor = argumentCaptor<AdError>()
        verify(mediationInterstitialAdCallback).onAdFailedToShow(captor.capture())
        val capturedError = captor.firstValue
        assertEquals(123, capturedError.code)
        assertEquals("An error occurred", capturedError.message)
        assertEquals("com.google.ads.mediation.ironsource", capturedError.domain)
    }

    @Test
    fun onInterstitialAdShowFailed_withoutInterstitialAdCallbackInstance_verifyOnAdFailedToShow() {
        // given
        ironSourceRtbInterstitialAd.loadRtbAd()
        val errorRes = "An error occurred"
        val errorCode = 123
        val ironSourceError = IronSourceError(errorCode, errorRes)

        // When
        whenever(mediationAdLoadCallback.onSuccess(any())).thenReturn(null)
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)
        ironSourceRtbInterstitialAd.onInterstitialAdFailedToShow(interstitialAd, ironSourceError)

        // Then
        verifyNoInteractions(mediationInterstitialAdCallback)
    }

    @Test
    fun onInterstitialAdOpened_verifyOnInterstitialAdOpenedCallbacks() {
        // given
        ironSourceRtbInterstitialAd.loadRtbAd()
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)

        // when
        ironSourceRtbInterstitialAd.onInterstitialAdShown(interstitialAd)

        // then
        verify(mediationInterstitialAdCallback).onAdOpened()
        verify(mediationInterstitialAdCallback).reportAdImpression()
    }

    @Test
    fun onInterstitialAdClosed_verifyOnAdClosedCallback() {
        // given
        ironSourceRtbInterstitialAd.loadRtbAd()
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)

        // when
        ironSourceRtbInterstitialAd.onInterstitialAdDismissed(interstitialAd)

        // then
        verify(mediationInterstitialAdCallback).onAdClosed()
    }

    @Test
    fun onInterstitialAdClicked_verifyReportAdClickedCallback() {
        // given
        ironSourceRtbInterstitialAd.loadRtbAd()
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)

        // when
        ironSourceRtbInterstitialAd.onInterstitialAdClicked(interstitialAd)

        // then
        verify(mediationInterstitialAdCallback).reportAdClicked()
    }

    @Test
    fun onAdEvents_withoutInterstitialAd_verifyNoCallbacks() {
        // given
        ironSourceRtbInterstitialAd.loadRtbAd()

        // when
        ironSourceRtbInterstitialAd.onInterstitialAdShown(interstitialAd)
        ironSourceRtbInterstitialAd.onInterstitialAdDismissed(interstitialAd)
        ironSourceRtbInterstitialAd.onInterstitialAdClicked(interstitialAd)

        // then
        verifyNoMoreInteractions(mediationInterstitialAdCallback)
    }

    @Test
    fun onAdEvents_withoutInterstitialAdCallbackInstance_verifyNoCallbacks() {

    }

    @Test
    fun testShowAd_NullAd() {
        // given
        val mockContext = mock(Context::class.java)
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)

        // when
        ironSourceRtbInterstitialAd.showAd(mockContext)

        // then
        verify(interstitialAd, Mockito.never()).show(any())
    }

    @Test
    fun testShowAd_NonActivityContext() {
        // given
        val mockContext = mock(Context::class.java)
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)

        // when
        ironSourceRtbInterstitialAd.showAd(mockContext)

        // then
        verify(interstitialAd, Mockito.never()).show(any())
    }

    @Test
    fun testOnInterstitialAdShown_CallbackSet() {
        // given
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)

        // when
        ironSourceRtbInterstitialAd.onInterstitialAdShown(interstitialAd)

        // then
        verify(mediationInterstitialAdCallback).onAdOpened()
        verify(mediationInterstitialAdCallback).reportAdImpression()
    }

    @Test
    fun testOnInterstitialAdDismissed_CallbackSet() {
        // given
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)

        // when
        ironSourceRtbInterstitialAd.onInterstitialAdDismissed(interstitialAd)

        // then
        verify(mediationInterstitialAdCallback).onAdClosed()
    }

    @Test
    fun testOnInterstitialAdClicked_CallbackSet() {
        // given
        ironSourceRtbInterstitialAd.onInterstitialAdLoaded(interstitialAd)

        // when
        ironSourceRtbInterstitialAd.onInterstitialAdClicked(interstitialAd)

        // then
        verify(mediationInterstitialAdCallback).reportAdClicked()
    }

    @Test
    fun testLoadRtbAd_VerifyWatermarkInRequest() {
        // given
        val requestCaptor = argumentCaptor<InterstitialAdRequest>()

        // when
        ironSourceRtbInterstitialAd.loadRtbAd()

        // then
        mockedInterstitialAdLoader.verify({
            InterstitialAdLoader.loadAd(requestCaptor.capture(), any())
        })

        val capturedRequest = requestCaptor.firstValue
        val actualWatermark = capturedRequest.extraParams?.getString("google_watermark")
        assertEquals("sampleWatermark", actualWatermark)
    }
}