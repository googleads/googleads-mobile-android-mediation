package com.google.ads.mediation.unity

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdSize
import com.google.common.truth.Truth.assertThat
import com.unity3d.ads.UnityAds.UnityAdsInitializationError
import com.unity3d.ads.UnityAds.UnityAdsLoadError
import com.unity3d.ads.UnityAds.UnityAdsShowError
import com.unity3d.services.banners.BannerErrorCode
import com.unity3d.services.banners.BannerErrorInfo
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/** Unit tests for [UnityAdsAdapterUtils]. */
@RunWith(AndroidJUnit4::class)
class UnityAdsAdapterUtilsTest {

  private val context: Context = ApplicationProvider.getApplicationContext()
  private var bannerErrorInfo: BannerErrorInfo = mock()

  @Test
  fun getMediationErrorCode_withBannerErrorInfo_returnsCorrectValueForUnknownEnum() {
    bannerErrorInfo.errorCode = BannerErrorCode.UNKNOWN

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(bannerErrorInfo)

    assertThat(errorCode).isEqualTo(201)
  }

  @Test
  fun getMediationErrorCode_withBannerErrorInfo_returnsCorrectValueForNativeErrorEnum() {
    bannerErrorInfo.errorCode = BannerErrorCode.NATIVE_ERROR

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(bannerErrorInfo)

    assertThat(errorCode).isEqualTo(202)
  }

  @Test
  fun getMediationErrorCode_withBannerErrorInfo_returnsCorrectValueForWebviewErrorEnum() {
    bannerErrorInfo.errorCode = BannerErrorCode.WEBVIEW_ERROR

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(bannerErrorInfo)

    assertThat(errorCode).isEqualTo(203)
  }

  @Test
  fun getMediationErrorCode_withBannerErrorInfo_returnsCorrectValueForNoFillEnum() {
    bannerErrorInfo.errorCode = BannerErrorCode.NO_FILL

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(bannerErrorInfo)

    assertThat(errorCode).isEqualTo(204)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsInitializationError_returnsCorectValueForInternalError() {
    val initializationError: UnityAdsInitializationError =
      UnityAdsInitializationError.INTERNAL_ERROR

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(initializationError)

    assertThat(errorCode).isEqualTo(301)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsInitializationError_returnsCorectValueForInvalidArgument() {
    val initializationError: UnityAdsInitializationError =
      UnityAdsInitializationError.INVALID_ARGUMENT

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(initializationError)

    assertThat(errorCode).isEqualTo(302)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsInitializationError_returnsCorectValueForAdBlockerDetected() {
    val initializationError: UnityAdsInitializationError =
      UnityAdsInitializationError.AD_BLOCKER_DETECTED

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(initializationError)

    assertThat(errorCode).isEqualTo(303)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsLoadError_returnsCorectValueForInitializeFailed() {
    val loadError: UnityAdsLoadError = UnityAdsLoadError.INITIALIZE_FAILED

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(loadError)

    assertThat(errorCode).isEqualTo(401)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsLoadError_returnsCorectValueForInternalError() {
    val loadError: UnityAdsLoadError = UnityAdsLoadError.INTERNAL_ERROR

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(loadError)

    assertThat(errorCode).isEqualTo(402)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsLoadError_returnsCorectValueForInvalidArgument() {
    val loadError: UnityAdsLoadError = UnityAdsLoadError.INVALID_ARGUMENT

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(loadError)

    assertThat(errorCode).isEqualTo(403)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsLoadError_returnsCorectValueForNoFill() {
    val loadError: UnityAdsLoadError = UnityAdsLoadError.NO_FILL

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(loadError)

    assertThat(errorCode).isEqualTo(404)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsLoadError_returnsCorectValueForTimeout() {
    val loadError: UnityAdsLoadError = UnityAdsLoadError.TIMEOUT

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(loadError)

    assertThat(errorCode).isEqualTo(405)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsShowError_returnsCorectValueForNotInitialized() {
    val showError: UnityAdsShowError = UnityAdsShowError.NOT_INITIALIZED

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(showError)

    assertThat(errorCode).isEqualTo(501)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsShowError_returnsCorectValueForNotReady() {
    val showError: UnityAdsShowError = UnityAdsShowError.NOT_READY

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(showError)

    assertThat(errorCode).isEqualTo(502)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsShowError_returnsCorectValueForVideoPlayerError() {
    val showError: UnityAdsShowError = UnityAdsShowError.VIDEO_PLAYER_ERROR

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(showError)

    assertThat(errorCode).isEqualTo(503)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsShowError_returnsCorectValueForInvalidArgument() {
    val showError: UnityAdsShowError = UnityAdsShowError.INVALID_ARGUMENT

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(showError)

    assertThat(errorCode).isEqualTo(504)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsShowError_returnsCorectValueForNoConnection() {
    val showError: UnityAdsShowError = UnityAdsShowError.NO_CONNECTION

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(showError)

    assertThat(errorCode).isEqualTo(505)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsShowError_returnsCorectValueForAlreadyShowing() {
    val showError: UnityAdsShowError = UnityAdsShowError.ALREADY_SHOWING

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(showError)

    assertThat(errorCode).isEqualTo(506)
  }

  @Test
  fun getMediationErrorCode_withUnityAdsShowError_returnsCorectValueForInternalError() {
    val showError: UnityAdsShowError = UnityAdsShowError.INTERNAL_ERROR

    val errorCode = UnityAdsAdapterUtils.getMediationErrorCode(showError)

    assertThat(errorCode).isEqualTo(507)
  }

  @Test
  fun getUnityBannerSize_returnsNullOnInvalidSize() {
    val adSize = AdSize.WIDE_SKYSCRAPER

    val unityBannerAdSize =
      UnityAdsAdapterUtils.getUnityBannerSize(context, adSize, /* isRtb= */ false)

    assertThat(unityBannerAdSize).isNull()
  }

  @Test
  fun getUnityBannerSize_returnsCorrectBannerSize() {
    val adSize = AdSize.BANNER

    val unityBannerAdSize =
      UnityAdsAdapterUtils.getUnityBannerSize(context, adSize, /* isRtb= */ false)

    assertThat(unityBannerAdSize).isNotNull()
    assertThat(unityBannerAdSize?.width).isEqualTo(320)
    assertThat(unityBannerAdSize?.height).isEqualTo(50)
  }

  @Test
  fun getUnityBannerSize_returnsCorrectLeaderboardSize() {
    val adSize = AdSize.LEADERBOARD

    val unityBannerAdSize =
      UnityAdsAdapterUtils.getUnityBannerSize(context, adSize, /* isRtb= */ false)

    assertThat(unityBannerAdSize).isNotNull()
    assertThat(unityBannerAdSize?.width).isEqualTo(728)
    assertThat(unityBannerAdSize?.height).isEqualTo(90)
  }

  @Test
  fun createAdError_returnsAdErrorWithCorrectValues() {
    val adError = UnityAdsAdapterUtils.createAdError(200, "Description")

    assertThat(adError.getCode()).isEqualTo(200)
    assertThat(adError.getMessage()).isEqualTo("Description")
    assertThat(adError.getDomain()).isEqualTo(UnityMediationAdapter.SDK_ERROR_DOMAIN)
  }
}
