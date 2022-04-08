/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.play.integrity.codelab.server.models

data class RequestDetails(
    var requestPackageName: String? = null,
    var timestampMillis: Long = 0,
    var nonce: String? = null
)

data class AppIntegrity(
    var appRecognitionVerdict: String? = null,
    var packageName: String? = null,
    var certificateSha256Digest: List<String>? = null,
    var versionCode: Int = 0
)

data class DeviceIntegrity(
    var deviceRecognitionVerdict: List<String>? = null
)

data class AccountDetails(
    var appLicensingVerdict: String? = null
)

data class IntegrityVerdict(
    var requestDetails: RequestDetails,
    var appIntegrity: AppIntegrity,
    var deviceIntegrity: DeviceIntegrity,
    var accountDetails: AccountDetails
)

data class IntegrityVerdictPayload(
    var tokenPayloadExternal: IntegrityVerdict
)

