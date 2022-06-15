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

#include "json_util.hpp"
#include "json/json.h"
#include <algorithm>

JsonLookup::JsonLookup() {
    mIsValid = false;
    mReader = std::unique_ptr<Json::Reader>(new Json::Reader());
    mValue = std::unique_ptr<Json::Value>(new Json::Value());
}

JsonLookup::~JsonLookup() {
}

bool JsonLookup::CompareInsensitive(const std::string &a, const std::string &b) {
    return std::equal(a.begin(), a.end(), b.begin(), b.end(), [](char a, char b) {
        return tolower(a) == tolower(b);
    });
}

bool JsonLookup::ParseJson(const std::string &jsonString) {
    mIsValid = mReader.get()->parse(jsonString, *mValue.get());
    return mIsValid;
}

const Json::Value &JsonLookup::GetObjectForKey(const std::string &keyString,
                                               bool &foundObject) const {
    const Json::Value &jsonObject = *mValue.get();
    return JsonLookup::GetObjectForKeyFromObject(keyString, foundObject, jsonObject);
}

std::optional<bool> JsonLookup::GetBoolValueForKey(const std::string &keyString) const {
    if (mIsValid) {
        const Json::Value &jsonObject = *mValue.get();
        if (jsonObject.type() == Json::objectValue) {
            auto keys = jsonObject.getMemberNames();
            for (size_t i = 0; i < keys.size(); ++i) {
                const std::string &key = keys[i];
                if (JsonLookup::CompareInsensitive(key, keyString)) {
                    const Json::Value value = jsonObject[key];
                    const Json::ValueType valueType = value.type();
                    if (valueType == Json::booleanValue) {
                        return value.asBool();
                    }
                }
            }
        }
    }
    return std::nullopt;
}

std::optional<std::string> JsonLookup::GetStringValueForKey(const std::string &keyString) const {
    if (mIsValid) {
        return JsonLookup::GetValueForKeyFromObject(keyString, *mValue.get());
    }
    return std::nullopt;
}

std::optional<std::string> JsonLookup::GetValueForKeyFromObject(const std::string &keyString,
                                                                const Json::Value &jsonObject) {
    if (jsonObject.type() == Json::objectValue) {
        auto keys = jsonObject.getMemberNames();
        for (size_t i = 0; i < keys.size(); ++i) {
            const std::string &key = keys[i];
            if (JsonLookup::CompareInsensitive(key, keyString)) {
                const Json::Value value = jsonObject[key];
                const Json::ValueType valueType = value.type();
                if (!(valueType == Json::arrayValue || valueType == Json::objectValue)) {
                    return value.asString();
                }
            }
        }
    }
    return std::nullopt;
}

const Json::Value &JsonLookup::GetArrayForKeyFromObject(const std::string &keyString,
                                                        bool &foundObject,
                                                        const Json::Value &jsonObject) {
    return JsonLookup::GetTypeForKeyFromObject(keyString, foundObject, jsonObject,
                                               Json::arrayValue);
}

const Json::Value &JsonLookup::GetObjectForKeyFromObject(const std::string &keyString,
                                                         bool &foundObject,
                                                         const Json::Value &jsonObject) {
    return JsonLookup::GetTypeForKeyFromObject(keyString, foundObject, jsonObject,
                                               Json::objectValue);
}

const Json::Value &JsonLookup::GetTypeForKeyFromObject(const std::string &keyString,
                                                       bool &foundObject,
                                                       const Json::Value &jsonObject,
                                                       const int valueType) {
    if (jsonObject.type() == Json::objectValue) {
        auto keys = jsonObject.getMemberNames();
        for (size_t i = 0; i < keys.size(); ++i) {
            const std::string &key = keys[i];
            if (JsonLookup::CompareInsensitive(key, keyString)) {
                const Json::Value &value = jsonObject[key];
                if (valueType == value.type()) {
                    foundObject = true;
                    return value;
                }
            }
        }
    }

    // Return the root object if there is no match
    foundObject = false;
    return jsonObject;
}

bool JsonLookup::FindMatchingStringValueInArray(const std::string &valueString,
                                                const Json::Value &jsonObject) {
    bool foundMatch = false;
    if (jsonObject.type() == Json::arrayValue) {
        for (Json::Value::ArrayIndex i = 0; i != jsonObject.size(); i++) {
            const Json::Value &arrayValue = jsonObject[i];
            if (arrayValue.type() == Json::stringValue) {
                const std::string &arrayString = arrayValue.asString();
                if (arrayString == valueString) {
                    foundMatch = true;
                    break;
                }
            }
        }
    }
    return foundMatch;
}