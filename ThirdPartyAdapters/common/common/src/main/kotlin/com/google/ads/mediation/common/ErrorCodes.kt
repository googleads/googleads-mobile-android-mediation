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

package com.google.ads.mediation.common

/**
 * Mediation Adapter error code definitions.
 *
 * All adapters are expected to use the error codes defined here and not define their own error
 * codes for these error types.
 *
 * See go/unified-adapter-error-codes for the design.
 */
object ErrorCodes {

  /** Missing/invalid account key. */
  const val ERROR_CODE_INVALID_ACCOUNT_KEY = 130

  /** Missing/invalid application key. */
  const val ERROR_CODE_INVALID_APP_KEY = 131

  /**
   * The request had age-restricted treatment, but the 3P SDK cannot receive age-restricted signals.
   */
  const val ERROR_CODE_AGE_RESTRICTED = 132

  /** The domain for error codes defined in the common library. */
  const val COMMON_MEDIATION_ERROR_DOMAIN = "com.google.ads.mediation.common"
}
