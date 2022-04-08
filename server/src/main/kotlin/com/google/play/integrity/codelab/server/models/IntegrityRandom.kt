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
import java.util.logging.Logger

val randomStorage = mutableListOf<IntegrityRandom>()

fun generateIntegrityRandom(): IntegrityRandom {
    val integrityRandom = IntegrityRandom(
        generateRandom(),
        System.currentTimeMillis()
    )
	val log = Logger.getLogger("generateIntegrityRandom")
	log.info("Generated random for nonce: " + integrityRandom.random)
    randomStorage.add(integrityRandom)
    return integrityRandom
}

@Serializable
data class IntegrityRandom(val random: String, val timestamp: Long)