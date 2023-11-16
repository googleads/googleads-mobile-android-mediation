package com.google.ads.mediation.nend

import android.content.Context
import androidx.core.os.bundleOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.mediationAdapterInitializeVerifySuccess
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

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
}
