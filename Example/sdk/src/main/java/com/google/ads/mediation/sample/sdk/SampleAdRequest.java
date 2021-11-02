/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ads.mediation.sample.sdk;

import java.util.Set;

/**
 * A sample ad request used to load an ad. This is an example of some targeting options an ad
 * network may provide.
 */
public class SampleAdRequest {

  /**
   * Creates a new {@link SampleAdRequest}.
   */
  public SampleAdRequest() {
  }

  /**
   * Sets keywords for targeting purposes.
   *
   * @param keywords A set of keywords.
   */
  public void setKeywords(Set<String> keywords) {
    // Normally we'd save the keywords. But since this is a sample network, we'll do nothing.
  }

  /**
   * Designates a request for test mode.
   *
   * @param useTesting {@code true} to enable test mode.
   */
  public void setTestMode(boolean useTesting) {
    // Normally we'd save this flag. But since this is a sample network, we'll do nothing.
  }

  public void setShouldAddAwesomeSauce(boolean shouldAddAwesomeSauce) {
    // Normally we'd save this flag but since this is a sample network, we'll do nothing.
  }

  public void setIncome(int income) {
    // Normally we'd save this value but since this is a sample network, we'll do nothing.
  }

  public static String getSDKVersion() {
    return BuildConfig.SDK_VERSION;
  }
}
