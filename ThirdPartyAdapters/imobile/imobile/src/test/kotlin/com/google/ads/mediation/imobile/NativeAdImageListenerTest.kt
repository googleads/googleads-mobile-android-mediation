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

package com.google.ads.mediation.imobile

import android.app.Activity
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.mediation.MediationAdLoadCallback
import com.google.android.gms.ads.mediation.MediationNativeAdCallback
import com.google.android.gms.ads.mediation.NativeAdMapper
import com.google.common.truth.Truth.assertThat
import jp.co.imobile.sdkads.android.ImobileSdkAdsNativeAdData
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.Robolectric

/** Tests for [NativeAdImageListener]. */
@RunWith(AndroidJUnit4::class)
class NativeAdImageListenerTest {

  private val adLoadCallback: MediationAdLoadCallback<NativeAdMapper, MediationNativeAdCallback> =
    mock()
  private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()
  private val clickEvent: Runnable = mock()
  private val adData: ImobileSdkAdsNativeAdData = mock {
    on { sponsored } doReturn SPONSORED_TEXT
    on { description } doReturn DESCRIPTION
    on { title } doReturn TITLE
    on { clickEvent } doReturn clickEvent
  }

  private val nativeAdImageListener = NativeAdImageListener(adLoadCallback, activity, adData)

  @Test
  fun onNativeAdImageReciveCompleted_invokesLoadSuccessWithNativeAdMapper() {
    nativeAdImageListener.onNativeAdImageReciveCompleted(mock())

    val nativeAdMapperCaptor = argumentCaptor<NativeAdMapper>()
    verify(adLoadCallback).onSuccess(nativeAdMapperCaptor.capture())
    val nativeAdMapper = nativeAdMapperCaptor.firstValue
    assertThat(nativeAdMapper.advertiser).isEqualTo(SPONSORED_TEXT)
    assertThat(nativeAdMapper.body).isEqualTo(DESCRIPTION)
    assertThat(nativeAdMapper.callToAction).isEqualTo(Constants.CALL_TO_ACTION)
    assertThat(nativeAdMapper.headline).isEqualTo(TITLE)
    assertThat(nativeAdMapper.images.size).isEqualTo(1)
    assertThat(nativeAdMapper.icon).isNotNull()
    nativeAdMapper.handleClick(View(activity))
    verify(clickEvent).run()
  }

  private companion object {
    const val SPONSORED_TEXT = "the native ad's sponsored text"
    const val DESCRIPTION = "the native ad's description"
    const val TITLE = "the native ad's title"
  }
}
