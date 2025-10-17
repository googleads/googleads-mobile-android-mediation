package com.google.ads.mediation.applovin

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.applovin.mediation.AppLovinExtras
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppLovinExtrasTest {

  @Test
  fun build_withMuteAudioAsTrue_returnedBundleWithSetMuteAudioValue() {
    val extras = AppLovinExtras.Builder().setMuteAudio(true).build()

    assertThat(extras.getBoolean(AppLovinExtras.Keys.MUTE_AUDIO)).isTrue()
  }

  @Test
  fun build_withMuteAudioAsFalse_returnedBundleWithSetMuteAudioValue() {
    val extras = AppLovinExtras.Builder().setMuteAudio(false).build()

    assertThat(extras.getBoolean(AppLovinExtras.Keys.MUTE_AUDIO)).isFalse()
  }
}
