package com.google.ads.mediation.mytarget

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Class containing unit tests for MyTargetMediationAdapter.java */
@RunWith(AndroidJUnit4::class)
class MyTargetMediationAdapterTest {

  private var myTargetMediationAdapter: MyTargetMediationAdapter = MyTargetMediationAdapter()

  @Before
  fun setUp() {
    myTargetMediationAdapter = MyTargetMediationAdapter()
  }

  @Test
  fun instanceOfMyTargetAdapter_returnsAnInstanceOfMyTargetMediationAdapter() {
    assertThat(myTargetMediationAdapter is MyTargetMediationAdapter).isTrue()
  }
}
