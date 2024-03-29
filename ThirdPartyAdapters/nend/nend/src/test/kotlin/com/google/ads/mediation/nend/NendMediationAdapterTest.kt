package com.google.ads.mediation.nend

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.ads.mediation.nend.NendAdapterUtils.adapterVersion
import com.google.ads.mediation.nend.NendSdkWrapper.sdkVersion
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Class containing unit tests for [NendMediationAdapter] */
@RunWith(AndroidJUnit4::class)
class NendMediationAdapterTest {

  private var nendMediationAdapter: NendMediationAdapter = NendMediationAdapter()

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInitializationCompleteCallback = mock<InitializationCompleteCallback>()

  @Before
  fun setUp() {
    nendMediationAdapter = NendMediationAdapter()
  }

  // region Initialize Tests

  @Test
  fun initialize_invokesOnInitializationSucceeded() {
    nendMediationAdapter.mediationAdapterInitializeVerifySuccess(
      context,
      mockInitializationCompleteCallback,
      /* serverParameters= */ bundleOf()
    )
  }

  // endregion

  // region Version Info Tests

  @Test
  fun getVersionInfo_returnsCorrectVersionInfo() {
    mockStatic(NendAdapterUtils::class.java).use {
      whenever(adapterVersion) doReturn "1.2.3.4"

      nendMediationAdapter.assertGetVersionInfo(expectedValue = "1.2.304")
    }
  }

  @Test
  fun getVersionInfo_whenUnexpectedVersionFormat_returnZeroesVersionInfo() {
    mockStatic(NendAdapterUtils::class.java).use {
      whenever(adapterVersion) doReturn "3.2.1"

      nendMediationAdapter.assertGetVersionInfo(expectedValue = "0.0.0")
    }
  }

  // endregion

  // region SDK Version  Tests

  @Test
  fun getSdkVersion_returnsCorrectSdkVersionInfo() {
    mockStatic(NendSdkWrapper::class.java).use {
      whenever(sdkVersion) doReturn "3.2.1"

      nendMediationAdapter.assertGetSdkVersion(expectedValue = "3.2.1")
    }
  }

  @Test
  fun getSdkVersion_whenPatchVersionIsPresent_ignoresPatchVersion() {
    mockStatic(NendSdkWrapper::class.java).use {
      whenever(sdkVersion) doReturn "3.2.1.0"

      nendMediationAdapter.assertGetSdkVersion(expectedValue = "3.2.1")
    }
  }

  @Test
  fun getSdkVersion_whenLongerVersion_returnsCorrectSdkVersionInfo() {
    mockStatic(NendSdkWrapper::class.java).use {
      whenever(sdkVersion) doReturn "5.4.3.2.1.0"

      nendMediationAdapter.assertGetSdkVersion(expectedValue = "5.4.3")
    }
  }

  @Test
  fun getSdkVersion_whenUnexpectedVersionFormat_returnsZerosVersionInfo() {
    mockStatic(NendSdkWrapper::class.java).use {
      whenever(sdkVersion) doReturn "1.0"

      nendMediationAdapter.assertGetSdkVersion(expectedValue = "0.0.0")
    }
  }

  // endregion
}
