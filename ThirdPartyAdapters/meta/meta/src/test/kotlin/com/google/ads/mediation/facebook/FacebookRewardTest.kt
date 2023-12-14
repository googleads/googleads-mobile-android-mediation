package com.google.ads.mediation.facebook

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for [FacebookReward]. */
@RunWith(AndroidJUnit4::class)
class FacebookRewardTest {

  /** The unit under test. */
  private lateinit var facebookReward: FacebookReward

  @Before
  fun setUp() {
    facebookReward = FacebookReward()
  }

  @Test
  fun getType_returnsEmptyString() {
    assertThat(facebookReward.type).isEqualTo("")
  }

  @Test
  fun getAmount_returnsOne() {
    assertThat(facebookReward.amount).isEqualTo(1)
  }
}
