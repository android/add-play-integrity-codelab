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
package com.example.google.codelab.playintegritykotlin.ui.main.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.google.codelab.playintegritykotlin.data.ServerState
import com.example.google.codelab.playintegritykotlin.data.ServerStatus
import com.example.google.codelab.playintegritykotlin.integrity.IntegrityServer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val integrityServer = IntegrityServer()
    val state: StateFlow<MainViewState> = integrityServer.serverState.mapLatest {
        MainViewState(it)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = MainViewState(ServerState())
    )

    init {
        requestRandom()
    }

    public fun requestRandom() {
        viewModelScope.launch {
            integrityServer.requestRandom()
        }
    }

    public fun integrityCommand(context: Context) {
        viewModelScope.launch {
            integrityServer.integrityCommand(context)
        }
    }

    public fun expressCommand() {
        viewModelScope.launch {
            integrityServer.expressCommand()
        }
    }
}