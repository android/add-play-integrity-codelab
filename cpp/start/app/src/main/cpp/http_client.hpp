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

#include <optional>
#include <string>

/**
 * An HTTP client backed by curl.
 */
class HTTPClient {
public:
    /**
     * Constructs an HTTP client.
     */
    explicit HTTPClient();

    HTTPClient(const HTTPClient &) = delete;

    ~HTTPClient();

    void operator=(const HTTPClient &) = delete;

    /**
     * Performs an HTTP GET request.
     *
     * @param url The URL to GET.
     * @param error An out parameter for an error string, if one occurs.
     * @return A non-empty value containing the body of the response on success,
     * or an empty result on failure.
     */
    std::optional<std::string> Get(const std::string &url,
                                   std::string *error) const;

    /**
     * Performs an HTTP POST request.
     *
     * @param url The URL to POST.
     * @param body The data sent by the POST.
     * @param error An out parameter for an error string, if one occurs.
     * @return A non-empty value containing the body of the response on success,
     * or an empty result on failure.
     */
    std::optional<std::string> Post(const std::string &url, const std::string &body,
                                    std::string *error) const;

    /**
     * Sets the path to the CACert file for curl
     * @param cacert_path Absolute path to the cacert.pem file
     */
    static void SetCACertPath(const std::string cacert_path);

private:
    static std::string cacert_path;
};
