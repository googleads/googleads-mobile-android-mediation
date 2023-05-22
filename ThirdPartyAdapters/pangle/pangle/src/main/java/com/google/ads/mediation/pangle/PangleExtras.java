// Copyright 2022 Google LLC
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

package com.google.ads.mediation.pangle;

import android.os.Bundle;

public class PangleExtras {

  /**
   * Class containing keys for the Pangle extras {@link Bundle}.
   */
  static class Keys {

    static final String USER_DATA = "user_data";
  }

  /**
   * Convenience class used to build the Pangle network extras {@link Bundle}.
   */
  public static class Builder {

    private String userData;

    /**
     * Use this to set user data.
     */
    public Builder setUserData(String userData) {
      this.userData = userData;
      return this;
    }

    /**
     * Builds a {@link Bundle} object with the given inputs.
     */
    public Bundle build() {
      final Bundle extras = new Bundle();
      extras.putString(Keys.USER_DATA, userData);
      return extras;
    }
  }
}
