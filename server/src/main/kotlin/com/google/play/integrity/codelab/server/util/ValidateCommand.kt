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
import com.google.play.integrity.codelab.server.models.ValidateResult
import com.google.play.integrity.codelab.server.models.randomStorage
import java.security.MessageDigest
import java.util.*
import java.util.logging.Logger

// Five minute timeout (in milliseconds)
const val NONCE_TIMEOUT = 1000 * 60 * 5

// Package name of the client application
const val APPLICATION_PACKAGE_IDENTIFIER = "your.package.identifier"
//const val APPLICATION_PACKAGE_IDENTIFIER = "com.googlenct.sample.nativegamepad"
//const val APPLICATION_PACKAGE_IDENTIFIER = "com.your.app.package"

// Values returned by the verdict that provide integrity signals
const val VERDICT_VAL_MEETS_BASIC_INTEGRITY = "MEETS_BASIC_INTEGRITY"
const val VERDICT_VAL_MEETS_DEVICE_INTEGRITY = "MEETS_DEVICE_INTEGRITY"
const val VERDICT_VAL_MEETS_STRONG_INTEGRITY = "MEETS_STRONG_INTEGRITY"
const val VERDICT_VAL_MEETS_VIRTUAL_INTEGRITY = "MEETS_VIRTUAL_INTEGRITY"
const val VERDICT_VAL_VERSION_UNRECOGNIZED = "UNRECOGNIZED_VERSION"
const val VERDICT_VAL_VERSION_RECOGNIZED = "PLAY_RECOGNIZED"
const val VERDICT_VAL_LICENSED = "LICENSED"
const val VERDICT_VAL_UNLICENSED = "UNLICENSED"

fun validateCommand(commandString: String,
                  integrityVerdict: IntegrityVerdict
): ValidateResult {
    if (integrityVerdict.requestDetails.nonce != null) {
        var nonceString: String = integrityVerdict.requestDetails.nonce!!
        // Server might re-pad base64 with unicode '=', trim any that exist to
        // match our web-safe original
        val utfEqualRegex = "\\u003d$".toRegex()
        nonceString = utfEqualRegex.replace(nonceString, "")
        val nonceBytes = Base64.getUrlDecoder().decode(nonceString)
        // The nonce string contains two parts, the random number previously generated,
        // and the SHA256 hash of the command string, we need to separate them
        // The values were written out as hex values, so they are base64 compatible, but
        // we don't actually base64 decode them.
        val randomString = nonceString.slice(IntRange(0, (RANDOM_BYTE_COUNT * 2) - 1))
        val hashString = nonceString.slice(IntRange(RANDOM_BYTE_COUNT * 2, nonceString.lastIndex))

        val log = Logger.getLogger("validateCommand")
        log.info("Raw nonce: $nonceString")
        log.info("Random nonce segment: $randomString")
        log.info("Hash nonce segment: $hashString")

        // Verify the random part of the nonce was a random number previously generated on
        // the server, and that it hasn't expired
        val matchingRandom = randomStorage.find { it.random == randomString }
        if (matchingRandom != null) {
            val currentTimestamp = System.currentTimeMillis()
            val timeDelta = currentTimestamp - matchingRandom.timestamp
            // Can only use once, remove from the server's random list after matching
            randomStorage.remove(matchingRandom)
            if (timeDelta < NONCE_TIMEOUT) {
                return if (validateHash(commandString, hashString)) {
                    if (validateVerdict(integrityVerdict)) {
                        ValidateResult.VALIDATE_SUCCESS
                    } else {
                        ValidateResult.VALIDATE_INTEGRITY_FAIL
                    }
                } else {
                    ValidateResult.VALIDATE_NONCE_MISMATCH
                }
            }
            return ValidateResult.VALIDATE_NONCE_EXPIRED
        }
    }
    return ValidateResult.VALIDATE_NONCE_NOT_FOUND
}

fun validateHash(commandString: String, hashString: String) : Boolean {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    val commandHashBytes = messageDigest.digest(commandString.toByteArray(Charsets.UTF_8))
    val commandHashString = commandHashBytes.toHexString()
    val hashMatch = hashString.contentEquals(commandHashString)

    val log = Logger.getLogger("validateHash")
    log.info("Command string: $commandString")
    log.info("token hash string: $hashString")
    log.info("command hash string: $commandHashString")
    log.info("hashMatch: $hashMatch")

    return hashMatch
}

fun validateVerdict(integrityVerdict: IntegrityVerdict) : Boolean {
    // Process the integrity verdict and 'validate' the command if the following positive
    // signals exist:
    // 1) Positive device integrity signal
    // 2) Recognized app version signal
    // 3) Licensed user signal
    // 4) Application package identifier match
    var metDeviceIntegrity = false
    for (deviceField in integrityVerdict.deviceIntegrity.deviceRecognitionVerdict!!) {
        when (deviceField) {
            VERDICT_VAL_MEETS_BASIC_INTEGRITY -> metDeviceIntegrity = true
            VERDICT_VAL_MEETS_DEVICE_INTEGRITY -> metDeviceIntegrity = true
            VERDICT_VAL_MEETS_STRONG_INTEGRITY -> metDeviceIntegrity = true
            VERDICT_VAL_MEETS_VIRTUAL_INTEGRITY -> metDeviceIntegrity = true
        }
    }

    if (metDeviceIntegrity) {
        val recognitionVerdict = integrityVerdict.appIntegrity.appRecognitionVerdict
        if (recognitionVerdict == VERDICT_VAL_VERSION_RECOGNIZED ||
            recognitionVerdict == VERDICT_VAL_VERSION_UNRECOGNIZED) {
            if (integrityVerdict.accountDetails.appLicensingVerdict ==
                    VERDICT_VAL_LICENSED) {
                if (integrityVerdict.requestDetails.requestPackageName ==
                    APPLICATION_PACKAGE_IDENTIFIER) {
                    return true
                }
            }
        }
    }
    return false
}