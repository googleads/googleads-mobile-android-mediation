// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.ads.mediation.mintegral

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.adaptertestkit.createMediationNativeAdConfiguration
import com.google.ads.mediation.mintegral.mediation.MintegralNativeAd
import com.google.ads.mediation.mintegral.mediation.MintegralNativeAdListener
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.MediationNativeAdConfiguration
import com.google.android.gms.ads.mediation.UnifiedNativeAdMapper
import com.google.android.gms.ads.nativead.MediaView
import com.google.common.truth.Truth.assertThat
import com.mbridge.msdk.out.Campaign
import com.mbridge.msdk.widget.MBAdChoice
import kotlin.test.assertIs
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric

/** Tests for [MintegralNativeAd]. */
@RunWith(AndroidJUnit4::class)
class MintegralNativeAdTest {

  private val context = Robolectric.buildActivity(Activity::class.java).get()
  private val mockNativeAdCallback: MediationNativeAdCallback = mock()
  private val mockAdLoadCallback:
    MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback> =
    mock {
      on { onSuccess(any()) } doReturn mockNativeAdCallback
    }
  private val adConfiguration = createMediationNativeAdConfiguration(context = context)

  private lateinit var mintegralNativeAd: TestMintegralNativeAd

  @Before
  fun setUp() {
    mintegralNativeAd = TestMintegralNativeAd(adConfiguration, mockAdLoadCallback)
  }

  @Test
  fun mapNativeAd_withAllCampaignProperties_mapsAllPropertiesCorrectly() {
    val mockCampaign =
      mock<Campaign> {
        on { getAppName() } doReturn TEST_APP_NAME
        on { getAppDesc() } doReturn TEST_APP_DESC
        on { getAdCall() } doReturn TEST_AD_CALL
        on { getRating() } doReturn TEST_RATING
        on { getIconUrl() } doReturn TEST_ICON_URL
      }

    mintegralNativeAd.mapNativeAd(mockCampaign, context)

    assertThat(mintegralNativeAd.headline).isEqualTo(TEST_APP_NAME)
    assertThat(mintegralNativeAd.body).isEqualTo(TEST_APP_DESC)
    assertThat(mintegralNativeAd.callToAction).isEqualTo(TEST_AD_CALL)
    assertThat(mintegralNativeAd.starRating).isEqualTo(TEST_RATING)

    val icon = mintegralNativeAd.icon
    assertIs<MintegralNativeAd.MBridgeNativeMappedImage>(icon)
    assertThat(icon.uri.toString()).isEqualTo(TEST_ICON_URL)
    assertThat(icon.scale).isEqualTo(1.0)

    val adChoicesContent = mintegralNativeAd.adChoicesContent
    assertIs<MBAdChoice>(adChoicesContent)

    assertThat(mintegralNativeAd.overrideClickHandling).isTrue()
  }

  @Test
  fun mapNativeAd_withNullProperties_doesNotSetNullFields() {
    val mockCampaign =
      mock<Campaign> {
        on { getAppName() } doReturn null
        on { getAppDesc() } doReturn null
        on { getAdCall() } doReturn null
        on { getIconUrl() } doReturn ""
      }

    mintegralNativeAd.mapNativeAd(mockCampaign, context)

    assertThat(mintegralNativeAd.headline).isNull()
    assertThat(mintegralNativeAd.body).isNull()
    assertThat(mintegralNativeAd.callToAction).isNull()
    assertThat(mintegralNativeAd.icon).isNull()
  }

  @Test
  fun traversalView_withNullView_returnsEmptyList() {
    val result = mintegralNativeAd.traversalView(null)

    assertThat(result).isEmpty()
  }

  @Test
  fun traversalView_withSingleView_returnsListContainingView() {
    val view = View(context)

    val result = mintegralNativeAd.traversalView(view)

    assertThat(result).containsExactly(view)
  }

  @Test
  fun traversalView_withMediaView_returnsListContainingMediaView() {
    val mediaView = MediaView(context)

    val result = mintegralNativeAd.traversalView(mediaView)

    assertThat(result).containsExactly(mediaView)
  }

  @Test
  fun traversalView_withViewGroupHierarchy_returnsAllChildViews() {
    val parent = LinearLayout(context)
    val child1 = View(context)
    val childGroup = FrameLayout(context)
    val child2 = View(context)
    parent.addView(child1)
    parent.addView(childGroup)
    childGroup.addView(child2)

    val result = mintegralNativeAd.traversalView(parent)

    assertThat(result).containsExactly(child1, child2)
  }

  @Test
  fun onEnterFullscreen_withNativeCallback_invokesOnAdOpened() {
    loadAdSuccessfully()

    mintegralNativeAd.onEnterFullscreen()

    verify(mockNativeAdCallback).onAdOpened()
  }

  @Test
  fun onEnterFullscreen_withoutNativeCallback_throwsNoException() {
    mintegralNativeAd.onEnterFullscreen()
  }

  @Test
  fun onExitFullscreen_withNativeCallback_invokesOnAdClosed() {
    loadAdSuccessfully()

    mintegralNativeAd.onExitFullscreen()

    verify(mockNativeAdCallback).onAdClosed()
  }

  @Test
  fun onExitFullscreen_withoutNativeCallback_throwsNoException() {
    mintegralNativeAd.onExitFullscreen()
  }

  @Test
  fun onVideoAdClicked_withNativeCallback_invokesReportAdClicked() {
    loadAdSuccessfully()

    mintegralNativeAd.onVideoAdClicked(mock())

    verify(mockNativeAdCallback).reportAdClicked()
  }

  @Test
  fun onVideoAdClicked_withoutNativeCallback_throwsNoException() {
    mintegralNativeAd.onVideoAdClicked(mock())
  }

  @Test
  fun onVideoStart_withNativeCallback_invokesOnVideoPlay() {
    loadAdSuccessfully()

    mintegralNativeAd.onVideoStart()

    verify(mockNativeAdCallback).onVideoPlay()
  }

  @Test
  fun onVideoStart_withoutNativeCallback_throwsNoException() {
    mintegralNativeAd.onVideoStart()
  }

  @Test
  fun onStartRedirection_throwsNoException() {
    mintegralNativeAd.onStartRedirection(mock(), "url")
  }

  @Test
  fun onFinishRedirection_throwsNoException() {
    mintegralNativeAd.onFinishRedirection(mock(), "url")
  }

  @Test
  fun onRedirectionFailed_throwsNoException() {
    mintegralNativeAd.onRedirectionFailed(mock(), "url")
  }

  @Test
  fun mappedImage_getters_returnCorrectValues() {
    val mockDrawable = mock<Drawable>()
    val mockUri = mock<Uri>()
    val mappedImage = mintegralNativeAd.MBridgeNativeMappedImage(mockDrawable, mockUri, 2.5)

    assertThat(mappedImage.drawable).isEqualTo(mockDrawable)
    assertThat(mappedImage.uri).isEqualTo(mockUri)
    assertThat(mappedImage.scale).isEqualTo(2.5)
  }

  private fun loadAdSuccessfully() {
    val listener = MintegralNativeAdListener(mintegralNativeAd, context, mockAdLoadCallback)
    listener.onAdLoaded(listOf(mock()), 0)
  }

  private class TestMintegralNativeAd(
    adConfiguration: MediationNativeAdConfiguration,
    adLoadCallback: MediationAdLoadCallback<UnifiedNativeAdMapper, MediationNativeAdCallback>,
  ) : MintegralNativeAd(adConfiguration, adLoadCallback) {
    override fun loadAd(adConfiguration: MediationNativeAdConfiguration?) {}

    public override fun mapNativeAd(ad: Campaign, context: Context) {
      super.mapNativeAd(ad, context)
    }

    public override fun traversalView(view: View?): List<*> {
      return super.traversalView(view)
    }
  }

  private companion object {
    const val TEST_APP_NAME = "Test App Name"
    const val TEST_APP_DESC = "Test App Desc"
    const val TEST_AD_CALL = "Install"
    const val TEST_RATING = 4.5
    const val TEST_ICON_URL = "https://example.com/icon.png"
  }
}
