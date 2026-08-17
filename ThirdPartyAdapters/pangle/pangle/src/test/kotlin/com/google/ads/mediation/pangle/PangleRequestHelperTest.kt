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

package com.google.ads.mediation.pangle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bytedance.sdk.openadsdk.api.PAGRequest
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_BID_RESPONSE
import com.google.ads.mediation.adaptertestkit.AdapterTestKitConstants.TEST_WATERMARK
import com.google.ads.mediation.adaptertestkit.createMediationBannerAdConfiguration
import com.google.ads.mediation.pangle.PangleRequestHelper.ADMOB_WATERMARK_KEY
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Unit tests for [PangleRequestHelper]. */
@RunWith(AndroidJUnit4::class)
class PangleRequestHelperTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val pagRequest: PAGRequest = mock()
  private val extraInfoCaptor = argumentCaptor<Map<String, Any>>()

  @Test
  fun setWatermarkString_whenBidResponseAndWatermarkPresent_setsWatermarkInExtraInfo() {
    val adConfiguration =
      createMediationBannerAdConfiguration(context = context, watermark = TEST_WATERMARK)

    PangleRequestHelper.setWatermarkString(pagRequest, TEST_BID_RESPONSE, adConfiguration)

    verify(pagRequest).setExtraInfo(extraInfoCaptor.capture())
    val extraInfo = extraInfoCaptor.firstValue
    assertThat(extraInfo).containsEntry(ADMOB_WATERMARK_KEY, TEST_WATERMARK)
  }

  @Test
  fun setWatermarkString_whenExtraInfoAlreadyExists_preservesExistingInfoAndAddsWatermark() {
    val existingExtraInfo = mutableMapOf<String, Any>("existing_key" to "existing_value")
    whenever(pagRequest.extraInfo) doReturn existingExtraInfo
    val adConfiguration =
      createMediationBannerAdConfiguration(context = context, watermark = TEST_WATERMARK)

    PangleRequestHelper.setWatermarkString(pagRequest, TEST_BID_RESPONSE, adConfiguration)

    verify(pagRequest).setExtraInfo(extraInfoCaptor.capture())
    val extraInfo = extraInfoCaptor.firstValue
    assertThat(extraInfo).containsEntry("existing_key", "existing_value")
    assertThat(extraInfo).containsEntry(ADMOB_WATERMARK_KEY, TEST_WATERMARK)
  }

  @Test
  fun setWatermarkString_whenBidResponseNull_doesNotSetWatermark() {
    val adConfiguration =
      createMediationBannerAdConfiguration(context = context, watermark = TEST_WATERMARK)

    PangleRequestHelper.setWatermarkString(pagRequest, null, adConfiguration)

    verify(pagRequest, never()).setExtraInfo(any())
  }

  @Test
  fun setWatermarkString_whenBidResponseEmpty_doesNotSetWatermark() {
    val adConfiguration =
      createMediationBannerAdConfiguration(context = context, watermark = TEST_WATERMARK)

    PangleRequestHelper.setWatermarkString(pagRequest, "", adConfiguration)

    verify(pagRequest, never()).setExtraInfo(any())
  }

  @Test
  fun setWatermarkString_whenWatermarkEmpty_doesNotSetWatermark() {
    val adConfiguration = createMediationBannerAdConfiguration(context = context, watermark = "")

    PangleRequestHelper.setWatermarkString(pagRequest, TEST_BID_RESPONSE, adConfiguration)

    verify(pagRequest, never()).setExtraInfo(any())
  }
}
