package com.google.ads.mediation.mytarget

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.assertGetSdkVersion
import com.google.ads.mediation.adaptertestkit.assertGetVersionInfo
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.ads.mediation.mytarget.MyTargetAdapterUtils.adapterVersion
import com.google.ads.mediation.mytarget.MyTargetSdkWrapper.sdkVersion
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

/** Class containing unit tests for MyTargetMediationAdapter.java */
@RunWith(AndroidJUnit4::class)
class MyTargetMediationAdapterTest {

  private var myTargetMediationAdapter: MyTargetMediationAdapter = MyTargetMediationAdapter()
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockInitializationCompleteCallback = mock<InitializationCompleteCallback>()

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
      /* serverParameters= */ bundleOf()
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
}
