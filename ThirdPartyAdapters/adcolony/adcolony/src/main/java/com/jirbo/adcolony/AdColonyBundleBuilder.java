// Copyright 2017 Google LLC
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

package com.jirbo.adcolony;

import android.os.Bundle;

/**
 * This is a helper class that helps publishers in creating a AdColony network-specific parameters
 * that can be used by the adapter to customize requests.
 */
public class AdColonyBundleBuilder {

  private static boolean _showPreAdPopup;
  private static boolean _showPostAdPopup;

  public static void setShowPrePopup(boolean showPrePopupValue) {
    _showPreAdPopup = showPrePopupValue;
  }

  public static void setShowPostPopup(boolean showPostPopupValue) {
    _showPostAdPopup = showPostPopupValue;
  }

  public static Bundle build() {
    Bundle bundle = new Bundle();
    bundle.putBoolean("show_pre_popup", _showPreAdPopup);
    bundle.putBoolean("show_post_popup", _showPostAdPopup);
    return bundle;
  }
}
