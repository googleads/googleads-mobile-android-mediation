package com.google.ads.mediation.vungle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.AdErrorMatcher
import com.google.ads.mediation.vungle.VungleInitializer.getInstance
import com.google.ads.mediation.vungle.VungleMediationAdapter.VUNGLE_SDK_ERROR_DOMAIN
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.RequestConfiguration
import com.google.common.truth.Truth.assertThat
import com.vungle.ads.VungleError
import com.vungle.ads.VunglePrivacySettings
import com.vungle.ads.internal.protos.Sdk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Tests for [VungleInitializer]. */
@RunWith(AndroidJUnit4::class)
class VungleInitializerTest {

  private lateinit var initializer: VungleInitializer

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val mockVungleInitializationListener =
    mock<VungleInitializer.VungleInitializationListener>()
  private val mockSdkWrapper = mock<SdkWrapper>() { on { isInitialized() } doReturn false }

  @Before
  fun setUp() {
    VungleSdkWrapper.delegate = mockSdkWrapper
    initializer = getInstance()
  }

  @Test
  fun multipleCallsToGetInstanceReturnsTheSameInstance() {
    assertThat(initializer).isEqualTo(getInstance())
  }

  @Test
  fun initialize_alreadyInitialized_callsOnSuccess() {
    whenever(mockSdkWrapper.isInitialized()) doReturn true

    initializer.initialize(TEST_APP_ID_1, context, mockVungleInitializationListener)

    verify(mockVungleInitializationListener).onInitializeSuccess()
  }

  @Test
  fun initialize_callsInit() {
    initializer.initialize(TEST_APP_ID_1, context, mockVungleInitializationListener)

    verify(mockSdkWrapper).init(eq(context), eq(TEST_APP_ID_1), eq(initializer))

    // Call onSuccess to clear the init listeners.
    initializer.onSuccess()
  }

  @Test
  fun initialize_onSuccessCalled_callsSuccessOnListener() {
    initializer.initialize(TEST_APP_ID_1, context, mockVungleInitializationListener)
    verify(mockSdkWrapper).init(eq(context), eq(TEST_APP_ID_1), eq(initializer))
    initializer.onSuccess()

    verify(mockVungleInitializationListener).onInitializeSuccess()
  }

  @Test
  fun multipleInitializeCalls_onSuccessCalled_callsSuccessOnListeners() {
    initializer.initialize(TEST_APP_ID_1, context, mockVungleInitializationListener)
    initializer.initialize(TEST_APP_ID_1, context, mockVungleInitializationListener)
    initializer.initialize(TEST_APP_ID_1, context, mockVungleInitializationListener)
    verify(mockSdkWrapper, times(1)).init(eq(context), eq(TEST_APP_ID_1), eq(initializer))
    initializer.onSuccess()

    verify(mockVungleInitializationListener, times(3)).onInitializeSuccess()
  }

  @Test
  fun onError_callsErrorOnListeners() {
    initializer.initialize(TEST_APP_ID_1, context, mockVungleInitializationListener)
    val anotherInitializationListener = mock<VungleInitializer.VungleInitializationListener>()
    initializer.initialize("another_app_id", context, anotherInitializationListener)
    val vungleError =
      mock<VungleError> {
        on { code } doReturn Sdk.SDKError.Reason.UNKNOWN_ERROR_VALUE
        on { errorMessage } doReturn "Liftoff Monetize SDK initialization failed."
      }

    initializer.onError(vungleError)

    val expectedAdError =
      AdError(Sdk.SDKError.Reason.UNKNOWN_ERROR_VALUE,
        "Liftoff Monetize SDK initialization failed.", VUNGLE_SDK_ERROR_DOMAIN)
    verify(mockVungleInitializationListener)
      .onInitializeError(argThat(AdErrorMatcher(expectedAdError)))
    verify(anotherInitializationListener)
      .onInitializeError(argThat(AdErrorMatcher(expectedAdError)))
  }

  @Test
  fun updateCoppaStatus_whenChildDirectedIsTrue_setsCoppaStatusTrue() {
    Mockito.mockStatic(VunglePrivacySettings::class.java).use {
      initializer.updateCoppaStatus(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)

      it.verify { VunglePrivacySettings.setCOPPAStatus(true) }
      it.verify({ VunglePrivacySettings.setCOPPAStatus(false) }, never())
    }
  }

  @Test
  fun updateCoppaStatus_whenChildDirectedIsFalse_setsCoppaStatusFalse() {
    Mockito.mockStatic(VunglePrivacySettings::class.java).use {
      initializer.updateCoppaStatus(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)

      it.verify { VunglePrivacySettings.setCOPPAStatus(false) }
      it.verify({ VunglePrivacySettings.setCOPPAStatus(true) }, never())
    }
  }

  @Test
  fun updateCoppaStatus_whenChildDirectedTagIsUnspecified_doesntSetCoppaStatus() {
    Mockito.mockStatic(VunglePrivacySettings::class.java).use {
      initializer.updateCoppaStatus(
        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_UNSPECIFIED
      )

      it.verify({ VunglePrivacySettings.setCOPPAStatus(any()) }, never())
    }
  }

  private companion object {
    const val TEST_APP_ID_1 = "testAppId1"
  }
}
