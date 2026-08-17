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

package com.google.ads.mediation.unity.eventadapters

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.ads.mediation.unity.UnityAdsAdapterUtils.AdEvent
import com.google.android.gms.ads.mediation.MediationBannerAdapter
import com.google.android.gms.ads.mediation.MediationBannerListener
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

/** Unit tests for [UnityBannerEventAdapter]. */
@RunWith(AndroidJUnit4::class)
class UnityBannerEventAdapterTest {

  private val listener: MediationBannerListener = mock()
  private val adapter: MediationBannerAdapter = mock()

  private lateinit var eventAdapter: UnityBannerEventAdapter

  @Before
  fun setUp() {
    eventAdapter = UnityBannerEventAdapter(listener, adapter)
  }

  @Test
  fun sendAdEvent_withLoadedEvent_callsOnAdLoaded() {
    eventAdapter.sendAdEvent(AdEvent.LOADED)

    verify(listener).onAdLoaded(adapter)
  }

  @Test
  fun sendAdEvent_withOpenedEvent_callsOnAdOpened() {
    eventAdapter.sendAdEvent(AdEvent.OPENED)

    verify(listener).onAdOpened(adapter)
  }

  @Test
  fun sendAdEvent_withClickedEvent_callsOnAdClicked() {
    eventAdapter.sendAdEvent(AdEvent.CLICKED)

    verify(listener).onAdClicked(adapter)
  }

  @Test
  fun sendAdEvent_withClosedEvent_callsOnAdClosed() {
    eventAdapter.sendAdEvent(AdEvent.CLOSED)

    verify(listener).onAdClosed(adapter)
  }

  @Test
  fun sendAdEvent_withLeftApplicationEvent_callsOnAdLeftApplication() {
    eventAdapter.sendAdEvent(AdEvent.LEFT_APPLICATION)

    verify(listener).onAdLeftApplication(adapter)
  }

  @Test
  fun sendAdEvent_withUnhandledEvent_doesNotCallListener() {
    val unhandledEvents =
      listOf(AdEvent.IMPRESSION, AdEvent.VIDEO_START, AdEvent.REWARD, AdEvent.VIDEO_COMPLETE)

    for (event in unhandledEvents) {
      eventAdapter.sendAdEvent(event)
    }

    verifyNoInteractions(listener)
  }
}
