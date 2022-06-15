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

#include "common.hpp"
#include "http_client.hpp"

#include <memory>
#include <optional>
#include <string>
#include <cassert>

#include "curl/curl.h"

using namespace std::string_literals;

namespace {
    constexpr char ACCEPT_STRING[] = "Accept: application/json";
    constexpr char CONTENT_TYPE_STRING[] = "Content-Type: application/json";
    constexpr char CHARSET_STRING[] = "charset: utf-8";

    size_t write_fn(char *data, size_t size, size_t nmemb, void *user_data) {
        assert(user_data != nullptr);
        std::string *buffer = reinterpret_cast<std::string *>(user_data);
        buffer->append(data, size * nmemb);
        return size * nmemb;
    }

    bool request_init(const std::string &cacert, const std::string &url,
                      std::string *error, std::string *buffer, CURL *curl) {

        if (curl == nullptr) {
            *error = "Failed to create CURL object";
            return false;
        }

        ALOGI("CURL url: %s", url.c_str());
        ALOGI("CURL cacart path: %s", cacert.c_str());
        CURLcode res = curl_easy_setopt(curl, CURLOPT_URL, url.c_str());
        if (res != CURLE_OK) {
            *error = "CURLOPT_URL failed: "s + curl_easy_strerror(res);
            return false;
        }

        res = curl_easy_setopt(curl, CURLOPT_VERBOSE, 1L);
        if (res != CURLE_OK) {
            *error = "CURLOPT_VERBOSE failed: "s + curl_easy_strerror(res);
            return false;
        }

        res = curl_easy_setopt(curl, CURLOPT_CAINFO, cacert.c_str());
        if (res != CURLE_OK) {
            *error = "CURLOPT_VERBOSE failed: "s + curl_easy_strerror(res);
            return false;
        }

        res = curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_fn);
        if (res != CURLE_OK) {
            *error = "CURLOPT_WRITEFUNCTION failed: "s + curl_easy_strerror(res);
            return false;
        }

        res = curl_easy_setopt(curl, CURLOPT_WRITEDATA,
                               reinterpret_cast<void *>(buffer));
        if (res != CURLE_OK) {
            *error = "CURLOPT_WRITEDATA failed: "s + curl_easy_strerror(res);
            return false;
        }

        res = curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1L);
        if (res != CURLE_OK) {
            *error = "CURLOPT_FOLLOWLOCATION failed: "s + curl_easy_strerror(res);
            return false;
        }

        return true;
    }

}  // namespace

std::string HTTPClient::cacert_path;

void HTTPClient::SetCACertPath(const std::string path) {
    HTTPClient::cacert_path = path;
}

HTTPClient::HTTPClient() {
    curl_global_init(CURL_GLOBAL_DEFAULT);
}

HTTPClient::~HTTPClient() { curl_global_cleanup(); }

std::optional<std::string> HTTPClient::Get(const std::string &url,
                                           std::string *error) const {
    std::string placeholder;
    if (error == nullptr) {
        error = &placeholder;
    }

    std::unique_ptr<CURL, decltype(&curl_easy_cleanup)> curl(curl_easy_init(),
                                                             curl_easy_cleanup);

    std::string buffer;

    if (!request_init(cacert_path, url, error, &buffer, curl.get())) {
        return std::nullopt;
    }

    CURLcode res = curl_easy_perform(curl.get());
    if (res != CURLE_OK) {
        *error = "easy_perform failed: "s + curl_easy_strerror(res);
        return std::nullopt;
    }

    return buffer;
}

std::optional<std::string> HTTPClient::Post(const std::string &url, const std::string &body,
                                            std::string *error) const {
    std::string placeholder;
    if (error == nullptr) {
        error = &placeholder;
    }

    std::unique_ptr<CURL, decltype(&curl_easy_cleanup)> curl(curl_easy_init(),
                                                             curl_easy_cleanup);
    std::string buffer;

    if (!request_init(cacert_path, url, error, &buffer, curl.get())) {
        return std::nullopt;
    }

    curl_slist *headerlist = curl_slist_append(NULL, ACCEPT_STRING);
    headerlist = curl_slist_append(headerlist, CONTENT_TYPE_STRING);
    headerlist = curl_slist_append(headerlist, CHARSET_STRING);
    CURLcode res = curl_easy_setopt(curl.get(), CURLOPT_HTTPHEADER, headerlist);
    if (res != CURLE_OK) {
        *error = "CURLOPT_HTTPHEADER failed: "s + curl_easy_strerror(res);
        return std::nullopt;
    }

    const size_t bodysize = body.length();
    res = curl_easy_setopt(curl.get(), CURLOPT_POSTFIELDSIZE, bodysize);
    if (res != CURLE_OK) {
        *error = "CURLOPT_POSTFIELDSIZE failed: "s + curl_easy_strerror(res);
        return std::nullopt;
    }

    res = curl_easy_setopt(curl.get(), CURLOPT_POSTFIELDS, body.c_str());
    if (res != CURLE_OK) {
        *error = "CURLOPT_POSTFIELDS failed: "s + curl_easy_strerror(res);
        return std::nullopt;
    }

    res = curl_easy_perform(curl.get());
    curl_slist_free_all(headerlist);
    ALOGI("buffer %d %s", (int) buffer.size(), buffer.c_str());
    if (res != CURLE_OK) {
        *error = "easy_perform failed: "s + curl_easy_strerror(res);
        return std::nullopt;
    }

    return buffer;
}