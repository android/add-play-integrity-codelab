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

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.playintegrity.v1.PlayIntegrity
import com.google.api.services.playintegrity.v1.model.DecodeIntegrityTokenRequest
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.google.common.collect.Lists
import java.util.logging.Logger

fun decryptToken(tokenString: String): String {
    val googleCredentials = GoogleCredentials.getApplicationDefault()
        .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/playintegrity"))
    val requestInitializer = HttpCredentialsAdapter(googleCredentials)
    val playIntegrity = PlayIntegrity.Builder(
        NetHttpTransport(),
        GsonFactory.getDefaultInstance(),
        requestInitializer)
        .setApplicationName("application")
        .build()

    val decodeTokenRequest = DecodeIntegrityTokenRequest().setIntegrityToken(tokenString)
    val returnString = playIntegrity.v1()
        .decodeIntegrityToken(APPLICATION_PACKAGE_IDENTIFIER, decodeTokenRequest)
        .execute()
        .toPrettyString()

    val log = Logger.getLogger("decryptToken")
    log.info("Decrypted token: $returnString")

    return returnString
}
