package com.google.ads.mediation.imobile

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifyFailure
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.ads.mediation.imobile.AdapterHelper.getAdapterVersion
import com.google.ads.mediation.imobile.IMobileMediationAdapter.ERROR_USER_IS_AGE_RESTRICTED_MSG
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
import com.google.android.gms.ads.RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED
import com.google.android.gms.ads.mediation.Adapter
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for [IMobileMediationAdapter]. */
@RunWith(AndroidJUnit4::class)
class IMobileMediationAdapterTest {

  private lateinit var adapter: IMobileMediationAdapter

  private val initializationCompleteCallback: InitializationCompleteCallback = mock()
  private val mediationConfiguration: MediationConfiguration = mock()
  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Before
  fun setUp() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)
    adapter = IMobileMediationAdapter()
  }

  @Test
  fun instanceOfIMobileMediationAdapter_returnsAnInstanceOfAdapter() {
    assertThat(adapter is Adapter).isTrue()
  }

  @Test
  fun getVersionInfo_invalid3Digits_returnsZeros() {
    mockStatic(AdapterHelper::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_invalidString_returnsZeros() {
    mockStatic(AdapterHelper::class.java).use {
      whenever(getAdapterVersion()) doReturn "foobar"

      adapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  @Test
  fun getVersionInfo_valid4Digits_returnsValid() {
    mockStatic(AdapterHelper::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getVersionInfo_valid5Digits_returnsValid() {
    mockStatic(AdapterHelper::class.java).use {
      whenever(getAdapterVersion()) doReturn "1.2.3.4.5"

      adapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getSDKVersionInfo_returnsDefault() {
    adapter.assertGetSdkVersion(expectedValue = "0.0.0")
  }

  @Test
  fun initialize_success() {
    adapter.initialize(context, initializationCompleteCallback, listOf(mediationConfiguration))
    verify(initializationCompleteCallback).onInitializationSucceeded()
  }

  @Test
  fun initialize_withTFCDAndTFUAFalse_invokesOnInitializationSucceeded() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    adapter.mediationAdapterInitializeVerifySuccess(
      context,
      initializationCompleteCallback,
      /* serverParameters= */ bundleOf(),
    )
  }

  @Test
  fun initialize_withTFCDTrue_invokesOnInitializationFailed() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_UNSPECIFIED)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    adapter.mediationAdapterInitializeVerifyFailure(
      context,
      initializationCompleteCallback,
      /* serverParameters= */ bundleOf(),
      ERROR_USER_IS_AGE_RESTRICTED_MSG,
    )
  }

  @Test
  fun initialize_withTFUATrue_invokesOnInitializationFailed() {
    val requestConfiguration =
      RequestConfiguration.Builder()
        .setTagForChildDirectedTreatment(TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED)
        .setTagForUnderAgeOfConsent(TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE)
        .build()
    MobileAds.setRequestConfiguration(requestConfiguration)

    adapter.mediationAdapterInitializeVerifyFailure(
      context,
      initializationCompleteCallback,
      /* serverParameters= */ bundleOf(),
      ERROR_USER_IS_AGE_RESTRICTED_MSG,
    )
  }
}
