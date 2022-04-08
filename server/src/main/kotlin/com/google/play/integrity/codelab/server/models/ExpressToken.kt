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

import com.google.play.integrity.codelab.server.util.generateRandom
import kotlinx.serialization.Serializable
import java.util.logging.Logger;

// Eight hour timeout (in milliseconds)
const val EXPRESS_TOKEN_TIMEOUT = 1000 * 60 * 60 * 8

// Char length of express token
const val EXPRESS_TOKEN_LENGTH = 32

val expressTokenStorage = mutableListOf<ExpressToken>()

fun generateExpressToken(): ExpressToken {
    val expressToken = ExpressToken(
        generateRandom(),
        System.currentTimeMillis()
    )
    val log = Logger.getLogger("generateExpressToken")
    log.info("Generated random for express token: " + expressToken.random)
    expressTokenStorage.add(expressToken)
    return expressToken
}

fun lookupExpressToken(expressToken: String): LookupResult {
    val matchingToken = expressTokenStorage.find { it.random == expressToken }
    if (matchingToken != null) {
        val currentTimestamp = System.currentTimeMillis()
        val timeDelta = currentTimestamp - matchingToken.timestamp
        // Single-use, so remove. A new one will be generated
        // and returned with the CommandResult
        expressTokenStorage.remove(matchingToken)
        if (timeDelta < EXPRESS_TOKEN_TIMEOUT) {
            return LookupResult.LOOKUP_FOUND
        }
        return LookupResult.LOOKUP_EXPIRED
    }
    return LookupResult.LOOKUP_NOT_FOUND
}

@Serializable
data class ExpressToken(val random: String, val timestamp: Long)