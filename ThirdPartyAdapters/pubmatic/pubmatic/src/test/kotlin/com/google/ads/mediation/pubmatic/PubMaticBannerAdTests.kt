import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.pubmatic.PubMaticAdFactory
import com.google.ads.mediation.pubmatic.PubMaticBannerAd
import com.google.ads.mediation.pubmatic.PubMaticMediationAdapter.Companion.SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationBannerAd
import com.google.android.gms.ads.mediation.MediationBannerAdCallback
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration
import com.google.common.truth.Truth.assertThat
import com.pubmatic.sdk.common.POBError
import com.pubmatic.sdk.openwrap.banner.POBBannerView
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/** Tests for [PubMaticBannerAd]. */
@RunWith(AndroidJUnit4::class)
class PubMaticBannerAdTests {

  // Subject of testing
  private lateinit var pubMaticBannerAd: PubMaticBannerAd

  private val context = ApplicationProvider.getApplicationContext<Context>()

  private val mediationBannerAdCallback = mock<MediationBannerAdCallback>()

  private val mediationAdLoadCallback =
    mock<MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback>> {
      on { onSuccess(any()) } doReturn mediationBannerAdCallback
    }

  private val pobBannerView = mock<POBBannerView>()

  private val adErrorCaptor = argumentCaptor<AdError>()

  private val pubMaticAdFactory =
    mock<PubMaticAdFactory> { on { createPOBBannerView(any()) } doReturn pobBannerView }

  private val mediationBannerAdConfiguration =
    MediationBannerAdConfiguration(
      context,
      "bid response",
      /*serverParameters = */ bundleOf(),
      /*mediationExtras=*/ bundleOf(),
      /*isTesting=*/ true,
      /*location=*/ null,
      TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      AdSize.BANNER,
      /*watermark=*/ "",
    )

  @Before
  fun setUp() {
    PubMaticBannerAd.newInstance(
        mediationBannerAdConfiguration,
        mediationAdLoadCallback,
        pubMaticAdFactory,
      )
      .onSuccess { pubMaticBannerAd = it }
  }

  @Test
  fun onAdReceived_invokesLoadSuccessCallback() {
    pubMaticBannerAd.onAdReceived(pobBannerView)

    verify(mediationAdLoadCallback).onSuccess(pubMaticBannerAd)
  }

  @Test
  fun onAdFailed_invokesLoadFailureCallback() {
    val pobError = POBError(ERROR_PUBMATIC_AD_LOAD_FAILURE, "Ad load failed")

    pubMaticBannerAd.onAdFailed(pobBannerView, pobError)

    verify(mediationAdLoadCallback).onFailure(adErrorCaptor.capture())
    val adError = adErrorCaptor.firstValue
    assertThat(adError.code).isEqualTo(ERROR_PUBMATIC_AD_LOAD_FAILURE)
    assertThat(adError.domain).isEqualTo(SDK_ERROR_DOMAIN)
  }

  @Test
  fun onAdImpression_reportsAdImpression() {
    // Call onAdReceived() to set pubMaticBannerAd.mediationBannerAdCallback
    pubMaticBannerAd.onAdReceived(pobBannerView)

    pubMaticBannerAd.onAdImpression(pobBannerView)

    verify(mediationBannerAdCallback).reportAdImpression()
  }

  @Test
  fun onAdClicked_reportsAdClicked() {
    // Call onAdReceived() to set pubMaticBannerAd.mediationBannerAdCallback
    pubMaticBannerAd.onAdReceived(pobBannerView)

    pubMaticBannerAd.onAdClicked(pobBannerView)

    verify(mediationBannerAdCallback).reportAdClicked()
  }

  @Test
  fun onAppLeaving_reportsAdLeftApplication() {
    // Call onAdReceived() to set pubMaticBannerAd.mediationBannerAdCallback
    pubMaticBannerAd.onAdReceived(pobBannerView)

    pubMaticBannerAd.onAppLeaving(pobBannerView)

    verify(mediationBannerAdCallback).onAdLeftApplication()
  }

  @Test
  fun onAdOpened_reportsAdOpened() {
    // Call onAdReceived() to set pubMaticBannerAd.mediationBannerAdCallback
    pubMaticBannerAd.onAdReceived(pobBannerView)

    pubMaticBannerAd.onAdOpened(pobBannerView)

    verify(mediationBannerAdCallback).onAdOpened()
  }

  @Test
  fun onAdClosed_reportsAdClosed() {
    // Call onAdReceived() to set pubMaticBannerAd.mediationBannerAdCallback
    pubMaticBannerAd.onAdReceived(pobBannerView)

    pubMaticBannerAd.onAdClosed(pobBannerView)

    verify(mediationBannerAdCallback).onAdClosed()
  }

  @Test
  fun getView_returnsPubMaticBannerView() {
    assertThat(pubMaticBannerAd.view).isEqualTo(pobBannerView)
  }

  private companion object {
    const val ERROR_PUBMATIC_AD_LOAD_FAILURE = 1002
  }
}
