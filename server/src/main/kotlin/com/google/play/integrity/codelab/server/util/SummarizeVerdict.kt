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

package com.google.play.integrity.codelab.server.util

import com.google.play.integrity.codelab.server.models.IntegrityVerdict

// Build a summary string of integrity verdict information. This is used for
// informational purposes, you would not normally pass this information back
// to the client.
fun summarizeVerdict( integrityVerdict: IntegrityVerdict): String {
    var verdictString = "Device integrity: "
    var foundDeviceIntegritySignal = false
    for (deviceField in integrityVerdict.deviceIntegrity.deviceRecognitionVerdict!!) {
        when (deviceField) {
            VERDICT_VAL_MEETS_BASIC_INTEGRITY -> {
                foundDeviceIntegritySignal = true
                verdictString += "Basic "
            }
            VERDICT_VAL_MEETS_DEVICE_INTEGRITY -> {
                foundDeviceIntegritySignal = true
                verdictString += "Device "
            }
            VERDICT_VAL_MEETS_STRONG_INTEGRITY -> {
                foundDeviceIntegritySignal = true
                verdictString += "Strong "
            }
            VERDICT_VAL_MEETS_VIRTUAL_INTEGRITY -> {
                foundDeviceIntegritySignal = true
                verdictString += "Virtual "
            }
        }
    }
    if (!foundDeviceIntegritySignal) {
        verdictString = "Not found"
    }

    when (integrityVerdict.appIntegrity.appRecognitionVerdict) {
        VERDICT_VAL_VERSION_RECOGNIZED -> verdictString += "\nApp version recognized"
        VERDICT_VAL_VERSION_UNRECOGNIZED -> verdictString += "\nApp version unrecognized"
        else -> verdictString += "\nApp version unevaluated"
    }

    when (integrityVerdict.accountDetails.appLicensingVerdict) {
        VERDICT_VAL_LICENSED -> verdictString += "\nApp licensed"
        VERDICT_VAL_UNLICENSED -> verdictString += "\nApp unlicensed"
        else -> verdictString += "\nApp license unevaluated"
    }

    if (integrityVerdict.requestDetails.requestPackageName ==
        APPLICATION_PACKAGE_IDENTIFIER) {
        verdictString += "\nPackage name match"
    } else {
        verdictString += "\nPackage name mismatch"
    }

    return verdictString
}
