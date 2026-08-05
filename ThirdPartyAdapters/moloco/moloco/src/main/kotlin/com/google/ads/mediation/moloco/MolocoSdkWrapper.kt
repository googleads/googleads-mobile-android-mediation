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

package com.google.ads.mediation.moloco

import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoInitializationListener
import com.moloco.sdk.publisher.init.MolocoInitParams
import com.moloco.sdk.publisher.privacy.MolocoPrivacy

/**
 * Wrapper singleton to enable mocking of Moloco SDK calls for unit testing.
 *
 * **Note:** It is used as a layer between the Moloco Adapter and the Moloco SDK. It is required to
 * use this class instead of calling the Moloco SDK methods directly.
 */
object MolocoSdkWrapper {
  /** Delegate used in unit tests to help mock calls to the third party SDK. */
  internal var delegate: SdkWrapper =
    object : SdkWrapper {
      override fun getSdkVersion(): String = com.moloco.sdk.BuildConfig.SDK_VERSION_NAME

      override fun isInitialized(): Boolean = Moloco.isInitialized

      override fun initialize(
        initParams: MolocoInitParams,
        listener: MolocoInitializationListener,
      ) {
        Moloco.initialize(initParams, listener)
      }

      override fun setAgeRestricted(isAgeRestricted: Boolean) {
        MolocoPrivacy.setPrivacy(
          MolocoPrivacy.PrivacySettings(
            isAgeRestrictedUser = isAgeRestricted,
            isDoNotSell = MolocoPrivacy.privacySettings.isDoNotSell,
            isUserConsent = MolocoPrivacy.privacySettings.isUserConsent,
          )
        )
      }

      override fun setUserConsent(isUserConsent: Boolean) {
        MolocoPrivacy.setPrivacy(
          MolocoPrivacy.PrivacySettings(
            isAgeRestrictedUser = MolocoPrivacy.privacySettings.isAgeRestrictedUser,
            isDoNotSell = MolocoPrivacy.privacySettings.isDoNotSell,
            isUserConsent = isUserConsent,
          )
        )
      }

      override fun setDoNotSell(isDoNotSell: Boolean) {
        MolocoPrivacy.setPrivacy(
          MolocoPrivacy.PrivacySettings(
            isAgeRestrictedUser = MolocoPrivacy.privacySettings.isAgeRestrictedUser,
            isDoNotSell = isDoNotSell,
            isUserConsent = MolocoPrivacy.privacySettings.isUserConsent,
          )
        )
      }
    }
}

/** Declares the methods that will invoke the Moloco SDK. */
interface SdkWrapper {
  fun getSdkVersion(): String

  fun isInitialized(): Boolean

  fun initialize(initParams: MolocoInitParams, listener: MolocoInitializationListener)

  fun setAgeRestricted(isAgeRestricted: Boolean)

  fun setUserConsent(isUserConsent: Boolean)

  fun setDoNotSell(isDoNotSell: Boolean)
}
