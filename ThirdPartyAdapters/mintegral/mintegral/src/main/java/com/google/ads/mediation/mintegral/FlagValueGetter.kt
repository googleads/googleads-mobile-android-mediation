// Copyright 2025 Google LLC
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

import java.lang.reflect.InvocationTargetException

/** Gets the values of flags that modify the adapter's behavior. */
class FlagValueGetter {

  /**
   * Returns whether to restrict loading multiple full-screen ads for a single Mintegral slot ID at
   * the same time.
   *
   * If true, loading of a second ad for a full-screen slot will be prevented until the previously
   * loaded ad has been shown.
   */
  fun shouldRestrictMultipleAdLoads(): Boolean {
    try {
      val adapterSettingsClass =
        Class.forName("com.google.android.gms.ads.internal.adaptersettings.AdapterSettings")
      val getInstanceMethod = adapterSettingsClass.getDeclaredMethod("getInstance")
      getInstanceMethod.setAccessible(true)
      val settings = getInstanceMethod.invoke(null)
      val getBooleanMethod =
        settings.javaClass.getDeclaredMethod(
          "getBoolean",
          String::class.java,
          Boolean::class.javaPrimitiveType,
        )
      getBooleanMethod.setAccessible(true)
      return getBooleanMethod.invoke(
        settings,
        /* key= */ "adapter:mintegral_android_restrict_multiple_ads",
        /* defaultValue= */ false,
      ) as Boolean
    }
    // Default to false if there is an exception.
    catch (e: ClassNotFoundException) {
      return false
    } catch (e: NoSuchMethodException) {
      return false
    } catch (e: IllegalAccessException) {
      return false
    } catch (e: InvocationTargetException) {
      return false
    } catch (e: NullPointerException) {
      return false
    }
  }
}
