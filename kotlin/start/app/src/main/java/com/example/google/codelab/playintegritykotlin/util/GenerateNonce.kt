/*
 * Copyright 2022 Google LLC
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
package com.example.google.codelab.playintegritykotlin.util

import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.util.Base64.getUrlEncoder

fun ByteArray.toHexString(): String = joinToString(separator = "") {
    currentByte -> "%02x".format(currentByte)
}

object GenerateNonce {
    private val TAG = "PlayIntegrityCodelab"

    // Generate a nonce for Play Integrity using the following steps:
    // 1. Generate a SHA-256 hash of the command string
    // 2. Convert the hash value to a hex string
    // 3. Take the random number string from the server and append the hash
    // hex string to it to create the nonce string
    // Play Integrity expects a URL encoded, non-padded Base64 string,
    // our hex string is a valid Base64 string, even though we don't actually
    // need to encode/decode it.
    fun GenerateNonceString(commandString: String, randomString: String) : String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val commandHashBytes = messageDigest.digest(
            commandString.toByteArray(Charsets.UTF_8))
        val commandHashString = commandHashBytes.toHexString()
        val nonceString = randomString + commandHashString
        Log.d(TAG, "nonce: " + nonceString)
        return nonceString
    }
}