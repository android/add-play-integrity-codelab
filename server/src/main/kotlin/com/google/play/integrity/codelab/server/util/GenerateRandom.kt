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

import java.security.SecureRandom

const val RANDOM_BYTE_COUNT = 16

fun ByteArray.toHexString(): String = joinToString(separator = "") {
        currentByte -> "%02x".format(currentByte) }

/*
 * Generates 16 bytes of random data using SecureRandom, converts
 * to a hex string and returns it.
 */
fun generateRandom(): String {
    val nonceBytes = ByteArray(RANDOM_BYTE_COUNT)
    SecureRandom().nextBytes(nonceBytes)
    return nonceBytes.toHexString()
}