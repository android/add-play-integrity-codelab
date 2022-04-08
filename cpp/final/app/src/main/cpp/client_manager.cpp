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

#include "http_client.hpp"
#include "client_manager.hpp"
#include "json_util.hpp"
#include "native_engine.hpp"
#include "server_urls.hpp"

#include <algorithm>
#include <inttypes.h>
#include <openssl/sha.h>
#include <optional>
#include <string>

namespace {
    // Key for random number in JSON returned by /getRandom endpoint
    constexpr char RANDOM_KEY[] = "random";
    // Keys for values returned in JSON returned by /performCommand endpoint
    constexpr char COMMANDSUCCESS_KEY[] = "commandSuccess";
    constexpr char DIAGNOSTICMESSAGE_KEY[] = "diagnosticMessage";
    constexpr char EXPRESSTOKEN_KEY[] = "expressToken";
    // Constants to construct the command JSON payload for the POST to the
    // /performCommand endpoint
    constexpr char COMMAND_JSON_PREFIX[] = "{ \"commandString\" : \"";
    constexpr char COMMAND_JSON_TOKEN[] = "\", \"tokenString\" : \"";
    constexpr char COMMAND_JSON_SUFFIX[] = "\" }";
    // Test 'command'
    constexpr char TEST_COMMAND[] = "TRANSFER FROM alice TO bob CURRENCY gems QUANTITY 1000";
    // Hex conversion table
    constexpr char HEX_TABLE[] = "0123456789abcdef";
}

ClientManager::ClientManager() {
    mResult = SERVER_OPERATION_NONE;
    mStatus = CLIENT_MANAGER_IDLE;
    mCurrentExpressToken = "";
    mCurrentNonce = "";
    mCurrentRandom = "";
    mCurrentSummary = "";
    mValidExpressToken = false;
    mValidRandom = false;
    mTokenRequest = nullptr;
    mTokenResponse = nullptr;

    const android_app *app = NativeEngine::GetInstance()->GetAndroidApp();
    const IntegrityErrorCode errorCode = IntegrityManager_init(app->activity->vm,
                                                               app->activity->javaGameActivity);
    if (errorCode == INTEGRITY_NO_ERROR) {
        mInitialized = true;
    } else {
        mInitialized = false;
        ALOGE("Play Integrity returned error: %d", errorCode);
    }

}

ClientManager::~ClientManager() {
    if (mInitialized) {
        IntegrityManager_destroy();
        mInitialized = false;
    }
}

void ClientManager::RequestRandom() {
    // Reset internal state
    mValidRandom = false;
    mCurrentRandom = "";

    // HTTP GET request to the server for a random number
	// Note that for simplicity, we are doing HTTP operations as
	// synchronous blocking instead of managing them from a
	// separate network thread
    HTTPClient client;
    std::string errorString;
    auto result = client.Get(GET_RANDOM_URL, &errorString);

    if (!result) {
        ALOGE("Curl Error: %s", errorString.c_str());
        mResult = SERVER_OPERATION_NETWORK_ERROR;
    } else {
        ALOGI("RequestRandom Result: %s", (*result).c_str());
        JsonLookup jsonLookup;
        if (jsonLookup.ParseJson(*result)) {
            // Check for a success value of true
            auto resultValue = jsonLookup.GetStringValueForKey(RANDOM_KEY);
            if (resultValue) {
                mCurrentRandom = *resultValue;
                mValidRandom = true;
            }
        }

        if (!mValidRandom) {
            ALOGE("getRandom returned invalid json object");
            mResult = SERVER_OPERATION_INVALID_RANDOM;
        }
    }
}

void ClientManager::StartCommandIntegrity() {
    // Only one request can be in-flight at a time
    if (mStatus != CLIENT_MANAGER_REQUEST_TOKEN) {
        mResult = SERVER_OPERATION_PENDING;
        // Request a fresh random
        RequestRandom();
        if (mValidRandom) {
            GenerateNonce();
            IntegrityTokenRequest_create(&mTokenRequest);
            IntegrityTokenRequest_setNonce(mTokenRequest, mCurrentNonce.c_str());

            const IntegrityErrorCode errorCode =
                    IntegrityManager_requestIntegrityToken(mTokenRequest, &mTokenResponse);
            if (errorCode != INTEGRITY_NO_ERROR) {
                ALOGE("Play Integrity returned error: %d", errorCode);
                CleanupRequest();
                mStatus = CLIENT_MANAGER_IDLE;
            } else {
                mStatus = CLIENT_MANAGER_REQUEST_TOKEN;
            }
        }
    }
}

void ClientManager::StartCommandExpress() {
    // Only one request can be in-flight at a time
    if (mStatus != CLIENT_MANAGER_REQUEST_TOKEN) {
        if (mValidExpressToken) {
            SendCommandToServer(mCurrentExpressToken);
            mStatus = CLIENT_MANAGER_RESPONSE_AVAILABLE;
        }
    }
}

void ClientManager::Update() {
    if (mStatus == CLIENT_MANAGER_REQUEST_TOKEN) {
        IntegrityResponseStatus responseStatus = INTEGRITY_RESPONSE_UNKNOWN;
        const IntegrityErrorCode errorCode =
                IntegrityTokenResponse_getStatus(mTokenResponse, &responseStatus);
        if (errorCode != INTEGRITY_NO_ERROR) {
            ALOGE("Play Integrity returned error: %d", errorCode);
            CleanupRequest();
            mStatus = CLIENT_MANAGER_IDLE;
        } else if (responseStatus == INTEGRITY_RESPONSE_COMPLETED) {
            std::string tokenString = IntegrityTokenResponse_getToken(mTokenResponse);
            SendCommandToServer(tokenString);
            CleanupRequest();
            mStatus = CLIENT_MANAGER_RESPONSE_AVAILABLE;
        }
    }
}

void ClientManager::CleanupRequest() {
    if (mTokenResponse != nullptr) {
        IntegrityTokenResponse_destroy(mTokenResponse);
        mTokenResponse = nullptr;
    }
    if (mTokenRequest != nullptr) {
        IntegrityTokenRequest_destroy(mTokenRequest);
        mTokenRequest = nullptr;
    }
}

void ClientManager::SendCommandToServer(const std::string &token) {
    // Note that for simplicity, we are doing HTTP operations as
    // synchronous blocking instead of managing them from a
    // separate network thread
    HTTPClient client;
    std::string errorString;

    // Manually construct the json payload
    std::string payloadString = COMMAND_JSON_PREFIX;
    payloadString += TEST_COMMAND;
    payloadString += COMMAND_JSON_TOKEN;
    payloadString += token;
    payloadString += COMMAND_JSON_SUFFIX;

    auto result = client.Post(PERFORM_COMMAND_URL, payloadString, &errorString);
    if (!result) {
        ALOGE("SendCommandToServer Curl reported error: %s", errorString.c_str());
        mResult = SERVER_OPERATION_NETWORK_ERROR;
    } else {
        ALOGI("SendCommandToServer result: %s", (*result).c_str())
        // Preset to success, ParseResult will set a failure result if the parsing
        // errors.
        mResult = SERVER_OPERATION_SUCCESS;
        ParseResult(*result);
    }
}

void ClientManager::GenerateNonce() {
    // To generate the nonce we do the following:
    // 1. Generate a SHA-256 hash of the command string
    // 2. Convert the bytes of the hash into a hex string
    // 3. Create the nonce string by taking the random string and appending the hash string to it

    // Generate the SHA-256 hash
    unsigned char hashBuffer[SHA256_DIGEST_LENGTH];
    char hashHexString[(SHA256_DIGEST_LENGTH * 2) + 1];
    SHA256_CTX sha256Ctx;
    SHA256_Init(&sha256Ctx);
    SHA256_Update(&sha256Ctx, TEST_COMMAND, strlen(TEST_COMMAND));
    SHA256_Final(hashBuffer, &sha256Ctx);
    char *hexOut = hashHexString;
    for (size_t i = 0; i < SHA256_DIGEST_LENGTH; ++i) {
        *hexOut++ = HEX_TABLE[((hashBuffer[i] >> 4) & 0xF)];
        *hexOut++ = HEX_TABLE[(hashBuffer[i] & 0xF)];
    }
    // Terminate string
    *hexOut = '\0';

    mCurrentNonce = mCurrentRandom + hashHexString;
}

void ClientManager::ParseResult(const std::string &resultJson) {
    bool validJson = false;
    JsonLookup jsonLookup;
    if (jsonLookup.ParseJson(resultJson)) {
        // Look for all of our needed fields in the returned json
        auto commandSuccess = jsonLookup.GetBoolValueForKey(COMMANDSUCCESS_KEY);
        if (commandSuccess) {
            auto diagnosticString = jsonLookup.GetStringValueForKey(DIAGNOSTICMESSAGE_KEY);
            if (diagnosticString) {
                auto expressString = jsonLookup.GetStringValueForKey(EXPRESSTOKEN_KEY);
                if (expressString) {
                    if (*commandSuccess) {
                        // Express token only valid if the server reports the command succeeded
                        mValidExpressToken = true;
                    } else {
                        mValidExpressToken = false;
                        mResult = SERVER_OPERATION_REJECTED_VERDICT;
                    }
                    mCurrentSummary = *diagnosticString;
                    mCurrentExpressToken = *expressString;
                    validJson = true;
                }
            }
        }
    }
    if (!validJson) {
        mResult = SERVER_OPERATION_INVALID_RESULT;
    }
}

