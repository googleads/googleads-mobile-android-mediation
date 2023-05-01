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

package com.vungle.mediation;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The {@link AdapterParametersParser} class helps in creating a Vungle network-specific
 * parameters.
 */
public class AdapterParametersParser {

  public static class Config {

    private String appId;
    private String requestUniqueId;

    public String getAppId() {
      return appId;
    }

    public String getRequestUniqueId() {
      return requestUniqueId;
    }
  }

  @NonNull
  public static Config parse(@NonNull String appId, @Nullable Bundle networkExtras) {
    String uuid = null;
    if (networkExtras != null && networkExtras.containsKey(VungleExtrasBuilder.UUID_KEY)) {
      uuid = networkExtras.getString(VungleExtrasBuilder.UUID_KEY);
    }

    Config ret = new Config();
    ret.appId = appId;
    ret.requestUniqueId = uuid;
    return ret;
  }
}
