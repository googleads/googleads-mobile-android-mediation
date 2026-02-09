package com.google.ads.mediation.pangle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bytedance.sdk.openadsdk.api.init.PAGConfig
import com.google.testing.junit.testparameterinjector.TestParameter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestParameterInjector

/** Unit Test class for [PangleInitializer]. */
@RunWith(RobolectricTestParameterInjector::class)
class PangleInitializerTest {

  private lateinit var pangleInitializer: PangleInitializer

  private val pangleSdkWrapper: PangleSdkWrapper = mock()
  private val pagConfig: PAGConfig = mock()
  private val pagConfigBuilder: PAGConfig.Builder = mock {
    on { appId(any()) } doReturn this.mock
    on { setAdxId(PangleConstants.ADX_ID) } doReturn this.mock
    on { setPAConsent(any()) } doReturn this.mock
    on { setUserData(any()) } doReturn this.mock
    on { build() } doReturn pagConfig
  }
  private val pangleFactory: PangleFactory = mock {
    on { createPAGConfigBuilder() } doReturn pagConfigBuilder
  }

  private val context: Context = ApplicationProvider.getApplicationContext()
  private val initializerListener: PangleInitializer.Listener = mock()

  @Before
  fun setUp() {
    pangleInitializer = PangleInitializer(pangleSdkWrapper, pangleFactory)
  }

  @After
  fun afterEachTest() {
    // PangleInitializer has two internal flags that help track the status of the SDK initialization
    // Calling fail() resets both booleans to false as the starting state so that new tests
    // can be called with the initial PangleInitializer state.
    pangleInitializer.fail(0, "Resetting inner variables")
  }

  @Test
  fun initialize_startsSdkInit() {
    // Calling initialize() with proper parameters should go ahead and initialize the
    // PAGSdk by calling its init() without exiting the initialise method() or failing.
    pangleInitializer.initialize(context, APP_ID, initializerListener)

    verify(initializerListener, never()).onInitializeSuccess()
    verify(initializerListener, never()).onInitializeError(any())
    verify(pangleSdkWrapper).init(eq(context), any(), any())
  }

  @Test
  fun initialize_withEmptyAppId_triggersErrorCallback() {
    // When initialize is called with an empty string as the appId (second parameter)
    // the onInitializeError of the listener should be call and the code flow should end.
    pangleInitializer.initialize(context, "", initializerListener)

    verify(initializerListener, times(1)).onInitializeError(any())
    verify(initializerListener, never()).onInitializeSuccess()
  }

  @Test
  fun initializeCall_afterSuccess_returnsAnotherSucessWithoutExtraSDKInitialization() {
    // When initialize is called with proper parameters, at some point onInitializeSuccess()
    // should be called from the listener if the initialization was successful.
    pangleInitializer.initialize(context, APP_ID, initializerListener)
    // Success is called here to simulate a successful initialization of the SDK. The first
    // call of onInitializeSuccess should happen here.
    pangleInitializer.success()

    // Initializing the SDK again should not trigger another initialization but should
    // call the onInitializeSuccess method directly.
    pangleInitializer.initialize(context, APP_ID, initializerListener)

    // At the end of the test, onInitializeSuccess have should been called twice
    verify(initializerListener, times(2)).onInitializeSuccess()
    verify(initializerListener, never()).onInitializeError(any())
  }

  @Test
  fun initializeCall_beforeSuccess_enqueuesListenersAndTriggersAllAfterSuccess() {
    // If the sdk initialization is called twice before receiving a result should not throw any
    // error but should enqueue the listener of the second initialize() call so that when the
    // response is received...
    pangleInitializer.initialize(context, APP_ID, initializerListener)
    pangleInitializer.initialize(context, APP_ID, initializerListener)

    // ... and it is a success...
    pangleInitializer.success()

    // ... onInitializeSuccess() is called for every listener enqueued. Giving a total of two
    // times for this test.
    verify(initializerListener, times(2)).onInitializeSuccess()
    verify(initializerListener, never()).onInitializeError(any())
  }

  @Test
  fun whenInitializationFails_onInitializeErrorIsCalled() {
    // When initialize is called with proper parameters, at some point onInitializeError() should
    // be called from the listener if the initialization had an error.
    pangleInitializer.initialize(context, APP_ID, initializerListener)

    // fail() is called here to simulate an error on the initialization process of the SDK.
    pangleInitializer.fail(0, "errorTest")

    verify(initializerListener, times(1)).onInitializeError(any())
    verify(initializerListener, never()).onInitializeSuccess()
  }

  @Test
  fun initializeCall_beforeFail_enqueuesListenersAndTriggersAllAfterFail() {
    // If the sdk initialization is called twice before receiving a result should not throw any
    // error but should enqueue the listener of the second initialize() call so that when the
    // response is received...
    pangleInitializer.initialize(context, APP_ID, initializerListener)
    pangleInitializer.initialize(context, APP_ID, initializerListener)

    // ... and if it is an error...
    pangleInitializer.fail(0, "errorTest")

    // .. onInitializeError is called for each listener. In this test, it would be called twice.
    verify(initializerListener, times(2)).onInitializeError(any())
    verify(initializerListener, never()).onInitializeSuccess()
  }

  @Test
  fun initialize_configuresPangleSdkWithAppId() {
    pangleInitializer.initialize(context, APP_ID, initializerListener)

    verify(pagConfigBuilder).appId(APP_ID)
  }


  companion object {
    private const val APP_ID = "appId"
  }
}
