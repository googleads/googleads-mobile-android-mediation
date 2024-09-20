package com.google.ads.mediation.mytarget

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.ads.mediation.mytarget.MyTargetAdapterUtils.adapterVersion
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_DOMAIN
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_INVALID_SERVER_PARAMETERS
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.ERROR_MY_TARGET_SDK
import com.google.ads.mediation.mytarget.MyTargetMediationAdapter.MY_TARGET_SDK_ERROR_DOMAIN
import com.google.ads.mediation.mytarget.MyTargetSdkWrapper.sdkVersion
import com.google.ads.mediation.mytarget.MyTargetTools.KEY_SLOT_ID
import com.google.ads.mediation.mytarget.MyTargetTools.PARAM_MEDIATION_KEY
import com.google.ads.mediation.mytarget.MyTargetTools.PARAM_MEDIATION_VALUE
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationRewardedAd
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration
import com.my.target.ads.Reward
import com.my.target.ads.RewardedAd
import com.my.target.common.CustomParams
import com.my.target.common.models.IAdLoadingError
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Class containing unit tests for MyTargetMediationAdapter.java */
@RunWith(AndroidJUnit4::class)
class MyTargetMediationAdapterTest {

  private var myTargetMediationAdapter: MyTargetMediationAdapter = MyTargetMediationAdapter()

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInitializationCompleteCallback = mock<InitializationCompleteCallback>()
  private val mockRewardedAdCallback = mock<MediationRewardedAdCallback>()
  private val mockMediationRewardedAdLoadCallback =
    mock<MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback>> {
      on { onSuccess(any()) } doReturn mockRewardedAdCallback
    }

  @Before
  fun setUp() {
    myTargetMediationAdapter = MyTargetMediationAdapter()
  }

  // region Initialize Tests
  @Test
  fun initialize_invokesOnInitializationSucceeded() {
    myTargetMediationAdapter.mediationAdapterInitializeVerifySuccess(
      context,
      mockInitializationCompleteCallback,
      /* serverParameters= */ bundleOf(),
    )
  }

  // endregion

  // region Version Info Tests
  @Test
  fun getVersionInfo_returnsCorrectVersionInfo() {
    mockStatic(MyTargetAdapterUtils::class.java).use {
      whenever(adapterVersion) doReturn "1.2.3.4"

      myTargetMediationAdapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getVersionInfo_whenUnexpectedVersionFormat_returnZeroesVersionInfo() {
    mockStatic(MyTargetAdapterUtils::class.java).use {
      whenever(adapterVersion) doReturn "3.2.1"

      myTargetMediationAdapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  // endregion

  // region SDK Version  Tests

  @Test
  fun getSdkVersion_returnsCorrectSdkVersionInfo() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      whenever(sdkVersion) doReturn "3.2.1"

      myTargetMediationAdapter.assertGetSdkVersion(expectedValue = "3.2.1")
    }
  }

  @Test
  fun getSdkVersion_whenPatchVersion_returnsCorrectSdkVersionInfo() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      whenever(sdkVersion) doReturn "3.2.1.0"

      myTargetMediationAdapter.assertGetSdkVersion(expectedValue = "3.2.1")
    }
  }

  @Test
  fun getSdkVersion_whenLongerVersion_returnsCorrectSdkVersionInfo() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      whenever(sdkVersion) doReturn "5.4.3.2.1.0"

      myTargetMediationAdapter.assertGetSdkVersion(expectedValue = "5.4.3")
    }
  }

  @Test
  fun getSdkVersion_whenUnexpectedVersionFormat_returnsZerosVersionInfo() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      whenever(sdkVersion) doReturn "1.0"

      myTargetMediationAdapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  // endregion

  // region Rewarded ad tests
  @Test
  fun loadRewardedAd_withNullSlotId_invokesOnFailure() {
    val rewardedAdConfiguration = createRewardedAdConfiguration(context = context)

    myTargetMediationAdapter.loadRewardedAd(
      rewardedAdConfiguration,
      mockMediationRewardedAdLoadCallback,
    )

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Slot ID.", ERROR_DOMAIN)
    verify(mockMediationRewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedAd_withEmptyKeyMedia_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_SLOT_ID to "")
    val rewardedAdConfiguration = createRewardedAdConfiguration(serverParameters = serverParameters)

    myTargetMediationAdapter.loadRewardedAd(
      rewardedAdConfiguration,
      mockMediationRewardedAdLoadCallback,
    )

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Slot ID.", ERROR_DOMAIN)
    verify(mockMediationRewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedAd_slotIdIsNotParseableToInt_invokesOnFailure() {
    val serverParameters = bundleOf(KEY_SLOT_ID to "NotAnInt")
    val rewardedAdConfiguration = createRewardedAdConfiguration(serverParameters = serverParameters)

    myTargetMediationAdapter.loadRewardedAd(
      rewardedAdConfiguration,
      mockMediationRewardedAdLoadCallback,
    )

    val expectedAdError =
      AdError(ERROR_INVALID_SERVER_PARAMETERS, "Missing or invalid Slot ID.", ERROR_DOMAIN)
    verify(mockMediationRewardedAdLoadCallback).onFailure(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun loadRewardedAd_withValidValues_invokesLoadAdAfterInitialization() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val mockRewardedAd = mock<RewardedAd>()
      val mockCustomParams = mock<CustomParams>()
      whenever(MyTargetSdkWrapper.createRewardedAd(eq(1234), eq(context))) doReturn mockRewardedAd
      whenever(mockRewardedAd.customParams) doReturn mockCustomParams
      val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
      val rewardedAdConfiguration =
        createRewardedAdConfiguration(serverParameters = serverParameters)

      myTargetMediationAdapter.loadRewardedAd(
        rewardedAdConfiguration,
        mockMediationRewardedAdLoadCallback,
      )

      verify(mockRewardedAd).customParams
      verify(mockCustomParams).setCustomParam(eq(PARAM_MEDIATION_KEY), eq(PARAM_MEDIATION_VALUE))
      verify(mockRewardedAd).listener = myTargetMediationAdapter
      verify(mockRewardedAd).load()
    }
  }

  @Test
  fun showAd_invokesShow() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val mockRewardedAd = mock<RewardedAd>()
      val mockCustomParams = mock<CustomParams>()
      whenever(MyTargetSdkWrapper.createRewardedAd(eq(1234), eq(context))) doReturn mockRewardedAd
      whenever(mockRewardedAd.customParams) doReturn mockCustomParams
      val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
      val rewardedAdConfiguration =
        createRewardedAdConfiguration(serverParameters = serverParameters)
      myTargetMediationAdapter.loadRewardedAd(
        rewardedAdConfiguration,
        mockMediationRewardedAdLoadCallback,
      )

      myTargetMediationAdapter.showAd(context)

      verify(mockRewardedAd).show()
    }
  }

  @Test
  fun onLoad_invokesOnSuccess() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val mockRewardedAd = mock<RewardedAd>()
      val mockCustomParams = mock<CustomParams>()
      whenever(MyTargetSdkWrapper.createRewardedAd(eq(1234), eq(context))) doReturn mockRewardedAd
      whenever(mockRewardedAd.customParams) doReturn mockCustomParams
      val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
      val rewardedAdConfiguration =
        createRewardedAdConfiguration(serverParameters = serverParameters)
      myTargetMediationAdapter.loadRewardedAd(
        rewardedAdConfiguration,
        mockMediationRewardedAdLoadCallback,
      )

      myTargetMediationAdapter.onLoad(mockRewardedAd)

      verify(mockMediationRewardedAdLoadCallback).onSuccess(eq(myTargetMediationAdapter))
    }
  }

  @Test
  fun onNoAd_invokesOnFailure() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val mockRewardedAd = mock<RewardedAd>()
      val mockCustomParams = mock<CustomParams>()
      val mockAdLoadingError = mock<IAdLoadingError>()
      whenever(mockAdLoadingError.message) doReturn "TEST_ERROR_MESSAGE"
      whenever(MyTargetSdkWrapper.createRewardedAd(eq(1234), eq(context))) doReturn mockRewardedAd
      whenever(mockRewardedAd.customParams) doReturn mockCustomParams
      val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
      val rewardedAdConfiguration =
        createRewardedAdConfiguration(serverParameters = serverParameters)
      myTargetMediationAdapter.loadRewardedAd(
        rewardedAdConfiguration,
        mockMediationRewardedAdLoadCallback,
      )

      myTargetMediationAdapter.onNoAd(mockAdLoadingError, mockRewardedAd)

      val expectedAdError =
        AdError(ERROR_MY_TARGET_SDK, "TEST_ERROR_MESSAGE", MY_TARGET_SDK_ERROR_DOMAIN)
      verify(mockMediationRewardedAdLoadCallback)
        .onFailure(argThat(AdErrorMatcher(expectedAdError)))
    }
  }

  @Test
  fun onClick_invokesReportAdClicked() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val mockRewardedAd = mock<RewardedAd>()
      val mockCustomParams = mock<CustomParams>()
      whenever(MyTargetSdkWrapper.createRewardedAd(eq(1234), eq(context))) doReturn mockRewardedAd
      whenever(mockRewardedAd.customParams) doReturn mockCustomParams
      val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
      val rewardedAdConfiguration =
        createRewardedAdConfiguration(serverParameters = serverParameters)
      myTargetMediationAdapter.loadRewardedAd(
        rewardedAdConfiguration,
        mockMediationRewardedAdLoadCallback,
      )
      myTargetMediationAdapter.onLoad(mockRewardedAd)

      myTargetMediationAdapter.onClick(mockRewardedAd)

      verify(mockRewardedAdCallback).reportAdClicked()
    }
  }

  @Test
  fun onDismiss_invokesOnAdClosed() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val mockRewardedAd = mock<RewardedAd>()
      val mockCustomParams = mock<CustomParams>()
      whenever(MyTargetSdkWrapper.createRewardedAd(eq(1234), eq(context))) doReturn mockRewardedAd
      whenever(mockRewardedAd.customParams) doReturn mockCustomParams
      val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
      val rewardedAdConfiguration =
        createRewardedAdConfiguration(serverParameters = serverParameters)
      myTargetMediationAdapter.loadRewardedAd(
        rewardedAdConfiguration,
        mockMediationRewardedAdLoadCallback,
      )
      myTargetMediationAdapter.onLoad(mockRewardedAd)

      myTargetMediationAdapter.onDismiss(mockRewardedAd)

      verify(mockRewardedAdCallback).onAdClosed()
    }
  }

  @Test
  fun onReward_invokesOnVideoCompleteAndOnUserEarnedReward() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val mockRewardedAd = mock<RewardedAd>()
      val mockCustomParams = mock<CustomParams>()
      whenever(MyTargetSdkWrapper.createRewardedAd(eq(1234), eq(context))) doReturn mockRewardedAd
      whenever(mockRewardedAd.customParams) doReturn mockCustomParams
      val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
      val rewardedAdConfiguration =
        createRewardedAdConfiguration(serverParameters = serverParameters)
      myTargetMediationAdapter.loadRewardedAd(
        rewardedAdConfiguration,
        mockMediationRewardedAdLoadCallback,
      )
      myTargetMediationAdapter.onLoad(mockRewardedAd)

      myTargetMediationAdapter.onReward(mock<Reward>(), mockRewardedAd)

      verify(mockRewardedAdCallback).onVideoComplete()
      verify(mockRewardedAdCallback).onUserEarnedReward(any())
    }
  }

  @Test
  fun onDisplay_invokesOnAdOpenedOnVideoStartAndReportAdImpression() {
    mockStatic(MyTargetSdkWrapper::class.java).use {
      val mockRewardedAd = mock<RewardedAd>()
      val mockCustomParams = mock<CustomParams>()
      whenever(MyTargetSdkWrapper.createRewardedAd(eq(1234), eq(context))) doReturn mockRewardedAd
      whenever(mockRewardedAd.customParams) doReturn mockCustomParams
      val serverParameters = bundleOf(KEY_SLOT_ID to TEST_SLOT_ID)
      val rewardedAdConfiguration =
        createRewardedAdConfiguration(serverParameters = serverParameters)
      myTargetMediationAdapter.loadRewardedAd(
        rewardedAdConfiguration,
        mockMediationRewardedAdLoadCallback,
      )
      myTargetMediationAdapter.onLoad(mockRewardedAd)

      myTargetMediationAdapter.onDisplay(mockRewardedAd)

      verify(mockRewardedAdCallback).onAdOpened()
      verify(mockRewardedAdCallback).onVideoStart()
      verify(mockRewardedAdCallback).reportAdImpression()
    }
  }

  private fun createRewardedAdConfiguration(
    context: Context = this.context,
    serverParameters: Bundle = bundleOf(),
  ) =
    MediationRewardedAdConfiguration(
      context,
      /*bidresponse=*/ "",
      serverParameters,
      /*mediationExtras=*/ Bundle(),
      /*isTesting=*/ true,
      /*location=*/ null,
      RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED,
      RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED,
      /*maxAdContentRating=*/ "",
      TEST_WATERMARK,
    )

  // endregion

  private companion object {
    const val TEST_SLOT_ID = "1234"
  }
}
