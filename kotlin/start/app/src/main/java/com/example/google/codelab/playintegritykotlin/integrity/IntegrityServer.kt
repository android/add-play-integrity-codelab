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
package com.example.google.codelab.playintegritykotlin.integrity

import android.content.Context
import android.util.Log
import com.example.google.codelab.playintegritykotlin.data.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class IntegrityServer() {
    private val TAG = "PlayIntegrityCodelab"
    private val SERVER_URL: String = "https://your-play-integrity-server.com"
    private val TEST_COMMAND: String = "TRANSFER FROM alice TO bob CURRENCY gems QUANTITY 1000"

    private val httpClient: HttpClient = HttpClient(CIO) {
        install(Logging) {
            level = LogLevel.ALL
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 20000
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer()
            acceptContentTypes = acceptContentTypes + ContentType.Any
        }
    }

    private val _serverState: MutableStateFlow<ServerState> =
        MutableStateFlow(ServerState(ServerStatus.SERVER_STATUS_INITIALIZING))
    val serverState = _serverState.asStateFlow()

    suspend fun requestRandom() {
        _serverState.emit(ServerState(
            ServerStatus.SERVER_STATUS_WORKING))
        try {
            val returnedRandom = httpClient.get<IntegrityRandom>(
                SERVER_URL + "/getRandom")
            _serverState.emit(ServerState(ServerStatus.SERVER_STATUS_REACHABLE, returnedRandom))
        } catch (t: Throwable) {
            Log.d(TAG, "requestRandom exception " + t.message)
            _serverState.emit(ServerState(ServerStatus.SERVER_STATUS_UNREACHABLE,
                IntegrityRandom("", 0U)))
        }
    }

    suspend fun expressCommand() {
        val expressToken = _serverState.value.expressToken
        // Set our state to working to trigger a switch to the waiting UI
        _serverState.emit(ServerState(
            ServerStatus.SERVER_STATUS_WORKING))
        try {
            val commandResult = httpClient.post<CommandResult>(
                SERVER_URL + "/performCommand") {
                contentType(ContentType.Application.Json)
                body = ServerCommand(TEST_COMMAND, expressToken)
            }
            _serverState.emit(ServerState(ServerStatus.SERVER_STATUS_REACHABLE,
                // Show the express token as the new random
                IntegrityRandom(commandResult.expressToken, 0U),
                commandResult.diagnosticMessage,
                commandResult.commandSuccess,
                commandResult.expressToken))
        } catch (t: Throwable) {
            Log.d(TAG, "performCommand exception " + t.message)
            _serverState.emit(ServerState(ServerStatus.SERVER_STATUS_UNREACHABLE))
        }
    }

    // Requests a token from the Play Integrity API and sends it
    // to our server, along with our 'command', receive
    // a CommandResult object in response
    suspend fun integrityCommand(context: Context) {
    }

    // Issue a POST command to the server    
    suspend fun postCommand(tokenString: String, integrityRandom: IntegrityRandom) {
    }
}