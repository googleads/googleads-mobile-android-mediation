package com.google.ads.mediation.pangle.utils

import com.google.ads.mediation.pangle.PangleConstants
import com.google.ads.mediation.pangle.PangleInitializer
import com.google.ads.mediation.pangle.utils.TestConstants.PANGLE_INIT_FAILURE_CODE
import com.google.ads.mediation.pangle.utils.TestConstants.PANGLE_INIT_FAILURE_MESSAGE
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever

/** Mock that the Pangle SDK initialization succeeds. */
fun mockPangleSdkInitializationSuccess(pangleInitializer: PangleInitializer) {
  doAnswer { invocation ->
      val args: Array<Any> = invocation.getArguments()
      (args[2] as PangleInitializer.Listener).onInitializeSuccess()
    }
    .whenever(pangleInitializer)
    .initialize(any(), any(), any())
}

/** Mock that the Pangle SDK initialization fails. */
fun mockPangleSdkInitializationFailure(pangleInitializer: PangleInitializer) {
  doAnswer { invocation ->
      val args: Array<Any> = invocation.getArguments()
      (args[2] as PangleInitializer.Listener).onInitializeError(
        PangleConstants.createSdkError(PANGLE_INIT_FAILURE_CODE, PANGLE_INIT_FAILURE_MESSAGE)
      )
    }
    .whenever(pangleInitializer)
    .initialize(any(), any(), any())
}
