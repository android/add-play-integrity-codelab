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
package com.example.google.codelab.playintegritykotlin

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.google.codelab.playintegritykotlin.R
import com.example.google.codelab.playintegritykotlin.data.ServerState
import com.example.google.codelab.playintegritykotlin.data.ServerStatus
import com.example.google.codelab.playintegritykotlin.ui.WaitingView
import com.example.google.codelab.playintegritykotlin.ui.main.vm.MainViewModel
import com.example.google.codelab.playintegritykotlin.ui.main.vm.MainViewState
import com.example.google.codelab.playintegritykotlin.ui.theme.Green300
import com.example.google.codelab.playintegritykotlin.ui.theme.Green900
import com.example.google.codelab.playintegritykotlin.ui.theme.PlayIntegrityCodelabTheme
import com.example.google.codelab.playintegritykotlin.util.ErrorMessages.getErrorMessageForStatus


/**
 * Root view of the application. Handles navigation.
 */
@Composable
fun MainView(viewModel: MainViewModel = MainViewModel()) {
    val state by viewModel.state.collectAsState()
    var isErrorDialogOpen by rememberSaveable { mutableStateOf(true) }
    PlayIntegrityCodelabTheme {
        if (state.serverState.serverStatus == ServerStatus.SERVER_STATUS_INITIALIZING ||
            state.serverState.serverStatus == ServerStatus.SERVER_STATUS_WORKING) {
                WaitingView()
        } else if (state.serverState.serverStatus == ServerStatus.SERVER_STATUS_UNREACHABLE ||
            state.serverState.serverStatus == ServerStatus.SERVER_STATUS_FAILED_TO_GET_TOKEN) {
            if (isErrorDialogOpen) {
                ErrorDialog(getErrorMessageForStatus(state.serverState.serverStatus), {
                    isErrorDialogOpen = false
                })
            }
        } else {
            MainUI(state, viewModel)
        }
    }
}

@Composable
fun ErrorDialog(@StringRes textId: Int, onClose: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onClose,
        title = { Text(text = stringResource(id = R.string.error_title)) },
        text = { Text(text = stringResource(id = textId)) },
        confirmButton = {
            Button(onClick = onClose) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        }
    )
}

@Composable
fun getSummaryColor(state: MainViewState) : Color {
    if (state.serverState.commandSuccess) {
        if (isSystemInDarkTheme()) {
            return Green300
        } else {
            return Green900
        }
    }
    return MaterialTheme.colors.error
}

@Composable
fun MainUI(state: MainViewState, viewModel: MainViewModel) {
    // Play Integrity API requires a context when requesting an
    // integrity token
    val context = LocalContext.current
    val summaryColor = getSummaryColor(state)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = MaterialTheme.colors.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.align(Alignment.Center)
            ) {
                // App name as text header
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.h5,
                    textAlign = TextAlign.Center
                )

                // Button to request a random from the server
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.requestRandom() }
                ) {
                    Text(text = stringResource(id = R.string.button_request_nonce))
                }

                // Descriptor and text of most recent random returned by the server
                Text(
                    text = stringResource(id = R.string.current_nonce),
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Left
                )

                Text(
                    text = state.serverState.serverRandom.random,
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Left
                )

                // Button to request an integrity check
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.integrityCommand(context) }
                ) {
                    Text(text = stringResource(id = R.string.button_integrity_check))
                }

                if (state.serverState.expressToken.isNotEmpty()) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.expressCommand()}
                    ) {
                        Text(text = stringResource(id = R.string.button_express))
                    }
                }
                // Descriptor and text of the most recent integrity
                // verdict returned by the server
                Text(
                    text = stringResource(id = R.string.verdict_results),
                    style = MaterialTheme.typography.h6,
                    textAlign = TextAlign.Left
                )

                Text(
                    text = state.serverState.serverVerdict,
                    color = summaryColor,
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Left
                )
            }
        }
    }
}
