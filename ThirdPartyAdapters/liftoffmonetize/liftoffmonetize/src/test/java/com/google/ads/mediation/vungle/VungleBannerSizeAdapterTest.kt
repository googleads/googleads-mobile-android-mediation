package com.google.ads.mediation.vungle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdSize
import com.google.common.truth.Truth.assertThat
import com.vungle.ads.BannerAdSize
import com.vungle.mediation.VungleInterstitialAdapter
import org.junit.Test
import org.junit.runner.RunWith

/** Unit tests for the utility function getVungleBannerAdSizeFromGoogleAdSize() */
@RunWith(AndroidJUnit4::class)
class VungleBannerSizeAdapterTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun getVungleBannerAdSize_forGoogleSize300By50_returnsLiftoffSizeBannerShort() {
    val liftoffBannerSize =
      VungleInterstitialAdapter.getVungleBannerAdSizeFromGoogleAdSize(context, AdSize(300, 50))

    assertThat(liftoffBannerSize).isEqualTo(BannerAdSize.BANNER_SHORT)
  }

  @Test
  fun getVungleBannerAdSize_forGoogleSizeRegularBanner_returnsLiftoffSizeRegularBanner() {
    val liftoffBannerSize =
      VungleInterstitialAdapter.getVungleBannerAdSizeFromGoogleAdSize(context, AdSize.BANNER)

    assertThat(liftoffBannerSize).isEqualTo(BannerAdSize.BANNER)
  }

  @Test
  fun getVungleBannerAdSize_forGoogleSizeLeaderboard_returnsLiftoffSizeLeaderboard() {
    val liftoffBannerSize =
      VungleInterstitialAdapter.getVungleBannerAdSizeFromGoogleAdSize(context, AdSize.LEADERBOARD)

    assertThat(liftoffBannerSize).isEqualTo(BannerAdSize.BANNER_LEADERBOARD)
  }

  @Test
  fun getVungleBannerAdSize_forGoogleSizeMediumRectangle_returnsLiftoffSizeMediumRectangle() {
    val liftoffBannerSize =
      VungleInterstitialAdapter.getVungleBannerAdSizeFromGoogleAdSize(
        context,
        AdSize.MEDIUM_RECTANGLE,
      )

    assertThat(liftoffBannerSize).isEqualTo(BannerAdSize.VUNGLE_MREC)
  }

  @Test
  fun getVungleBannerAdSize_forUnsupportedGoogleBannerSize_returnsNull() {
    val liftoffBannerSize =
      VungleInterstitialAdapter.getVungleBannerAdSizeFromGoogleAdSize(
        context,
        AdSize.WIDE_SKYSCRAPER,
      )

    assertThat(liftoffBannerSize).isNull()
  }
}
