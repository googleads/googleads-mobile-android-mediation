package com.google.ads.mediation.inmobi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.inmobi.InMobiConstants.ERROR_INMOBI_FAILED_INITIALIZATION
import com.google.ads.mediation.inmobi.InMobiInitializer.INITIALIZED
import com.google.ads.mediation.inmobi.InMobiInitializer.INITIALIZING
import com.google.ads.mediation.inmobi.InMobiInitializer.UNINITIALIZED
import com.google.android.gms.ads.AdError
import com.google.common.truth.Truth.assertThat
import java.lang.Error
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class InMobiInitializerTest {

  private val inMobiSdkWrapper = mock<InMobiSdkWrapper>()
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val initializationListener = mock<InMobiInitializer.Listener>()
  private val enqueuedInitializationListener = mock<InMobiInitializer.Listener>()

  private lateinit var inMobiInitializer: InMobiInitializer

  @Before
  fun setup() {
    inMobiInitializer = InMobiInitializer(inMobiSdkWrapper)
  }

  @Test
  fun getInstance_alwaysReturnsSameInstance() {
    val instance1 = InMobiInitializer.getInstance()
    val instance2 = InMobiInitializer.getInstance()
    assertThat(instance1).isEqualTo(instance2)
  }

  @Test
  fun init_whenInMobiSDKUninitialized_invokesInMobiSDKInit() {
    inMobiInitializer.init(context, accountId, initializationListener)

    verify(inMobiSdkWrapper, times(1)).init(eq(context), eq(accountId), any(), any())
    assertThat(inMobiInitializer.initializationStatus).isEqualTo(INITIALIZING)
  }

  @Test
  fun init_whenInMobiSDKInitialized_invokesOnInitializationSuccess() {
    // First mimic that InMobiSDK initialization is complete
    inMobiInitializer.onInitializationComplete(null)

    // ...and init is called
    inMobiInitializer.init(context, accountId, initializationListener)

    // init() method is not invoked on the InMobiSdkWrapper
    verify(inMobiSdkWrapper, times(0)).init(eq(context), eq(accountId), any(), any())
    assertThat(inMobiInitializer.initializationStatus).isEqualTo(INITIALIZED)
  }

  @Test
  fun init_whenInMobiSDKInitializing_doesNotInvokeInMobiSdkInitTwice() {
    // During the first call initializationStatus is set to INITIALIZING
    inMobiInitializer.init(context, accountId, initializationListener)

    // During the second call initializationStatus is already set to INITIALIZING
    inMobiInitializer.init(context, accountId, initializationListener)

    // init() method is only invoked once on the InMobiSdkWrapper ie during the first call
    verify(inMobiSdkWrapper, times(1)).init(eq(context), eq(accountId), any(), any())
    assertThat(inMobiInitializer.listeners).contains(initializationListener)
  }

  @Test
  fun inMobiSDKInitializationComplete_withNullError_invokesInitializeSuccessOnListener() {
    inMobiInitializer.init(context, accountId, initializationListener)
    // Mimic InMobi SDK Initialization complete
    inMobiInitializer.onInitializationComplete(null)

    assertThat(inMobiInitializer.initializationStatus).isEqualTo(INITIALIZED)
    verify(initializationListener).onInitializeSuccess()
  }

  @Test
  fun whenSDKInitializationComplete_withError_invokesInitializeErrorOnListeners() {
    inMobiInitializer.init(context, accountId, initializationListener)
    // Mimic InMobi SDK Initialization completed with Error
    inMobiInitializer.onInitializationComplete(Error())

    assertThat(inMobiInitializer.initializationStatus).isEqualTo(UNINITIALIZED)
    val captor = argumentCaptor<AdError>()
    verify(initializationListener).onInitializeError(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(ERROR_INMOBI_FAILED_INITIALIZATION)
  }

  @Test
  fun inMobiSDKInitializationComplete_withNullError_invokesOnInitializeSuccessOnAllListeners() {
    // Init is called for the first time
    inMobiInitializer.init(context, accountId, initializationListener)
    // During the second init call the listener is enqueued to the list of all listeners
    inMobiInitializer.init(context, accountId, enqueuedInitializationListener)
    // Mimic InMobi SDK Initialization complete
    inMobiInitializer.onInitializationComplete(null)

    // Success callback is invoked on all listeners.
    verify(initializationListener).onInitializeSuccess()
    verify(enqueuedInitializationListener).onInitializeSuccess()
  }

  @Test
  fun inMobiSDKInitializationComplete_withError_invokesOnInitializeErrorOnAllListeners() {
    // Init is called for the first time
    inMobiInitializer.init(context, accountId, initializationListener)
    // During the second init call the listener is enqueued to the list of all listeners
    inMobiInitializer.init(context, accountId, enqueuedInitializationListener)

    // Mimic InMobi SDK Initialization complete with error
    inMobiInitializer.onInitializationComplete(Error())

    // Failure callback is invoked on all listeners.
    val captor = argumentCaptor<AdError>()
    verify(initializationListener).onInitializeError(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(ERROR_INMOBI_FAILED_INITIALIZATION)
    verify(enqueuedInitializationListener).onInitializeError(captor.capture())
    assertThat(captor.firstValue.code).isEqualTo(ERROR_INMOBI_FAILED_INITIALIZATION)
  }

  companion object {
    private const val accountId = "12345"
  }
}
