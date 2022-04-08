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

#pragma once

#include "util.hpp"

/*
 * Manages sending commands to the server and generating
 * Play Integrity tokens
 */
class ClientManager {
public:
    enum ServerOperationResult {
        // The server approved the operation
        SERVER_OPERATION_SUCCESS = 0,
        // No server operation has been performed yet
        SERVER_OPERATION_NONE = 1,
        // Server operation is pending
        SERVER_OPERATION_PENDING = 2,
        // Server operation failed due to network error
        SERVER_OPERATION_NETWORK_ERROR = -1,
        // Server operation failed due to invalid nonce returned from server
        SERVER_OPERATION_INVALID_RANDOM = -2,
        // Server operation failed due to invalid CommandResult returned from server
        SERVER_OPERATION_INVALID_RESULT = -3,
        // Server operation failed due server rejecting
        // from integrity check resulting in lack of positive verdict signals
        SERVER_OPERATION_REJECTED_VERDICT = -4
    };

    ClientManager();

    ~ClientManager();

    const std::string &GetCurrentExpressToken() const { return mCurrentExpressToken;}

    const std::string &GetCurrentRandomString() const { return mCurrentRandom; }

    const std::string &GetCurrentSummary() const { return mCurrentSummary; }

    void RequestRandom();

    void StartCommandIntegrity();

    void StartCommandExpress();

    ServerOperationResult GetOperationResult() const { return mResult; }

    void Update();

private:
    void CleanupRequest();

    void SendCommandToServer(const std::string &token);

    bool ParseRandom(const std::string &randomJson);

    void GenerateNonce();

    void ParseResult(const std::string &resultJson);

    enum ClientManagerStatus {
        CLIENT_MANAGER_IDLE = 0,
        CLIENT_MANAGER_REQUEST_TOKEN,
        CLIENT_MANAGER_RESPONSE_AVAILABLE
    };

    ServerOperationResult mResult;
    ClientManagerStatus mStatus;
    std::string mCurrentExpressToken;
    std::string mCurrentNonce;
    std::string mCurrentRandom;
    std::string mCurrentSummary;
    bool mInitialized;
    bool mValidRandom;
    bool mValidExpressToken;
};
