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

#include <string>
#include <optional>

namespace Json {
    class Reader;
    class Value;
}

// Minimal utility class for parsing a JSON object with JsonCpp
class JsonLookup {
public:
    JsonLookup();

    ~JsonLookup();

    bool ParseJson(const std::string &jsonString);

    const Json::Value &GetObjectForKey(const std::string &keyString, bool &foundObject) const;

    std::optional<bool> GetBoolValueForKey(const std::string &keyString) const;

    std::optional<std::string> GetStringValueForKey(const std::string &keyString) const;

    static const Json::Value &GetArrayForKeyFromObject(const std::string &keyString,
                                                       bool &foundObject,
                                                       const Json::Value &jsonObject);

    static const Json::Value &GetObjectForKeyFromObject(const std::string &keyString,
                                                        bool &foundObject,
                                                        const Json::Value &jsonObject);

    static const Json::Value &GetTypeForKeyFromObject(const std::string &keyString,
                                                      bool &foundObject,
                                                      const Json::Value &jsonObject,
                                                      const int valueType);

    static std::optional<std::string> GetValueForKeyFromObject(const std::string &keyString,
                                                               const Json::Value &jsonObject);

    static bool FindMatchingStringValueInArray(const std::string &valueString,
                                               const Json::Value &jsonObject);

    static bool CompareInsensitive(const std::string &a, const std::string &b);

private:
    std::unique_ptr<Json::Reader> mReader;
    std::unique_ptr<Json::Value> mValue;
    bool mIsValid;
};