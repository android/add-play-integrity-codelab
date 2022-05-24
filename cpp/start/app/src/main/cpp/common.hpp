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

#include <android/log.h>
#include <android/sensor.h>
#include <errno.h>
#include <cstring>
#include <EGL/egl.h>
#include <GLES3/gl3.h>
#include <jni.h>
#include <unistd.h>
#include <stdlib.h>
#include "game-activity/native_app_glue/android_native_app_glue.h"

#define LOG_TAG "PlayIntegritySample"

#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);
#define ALOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__);
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);
#ifdef NDEBUG
#define ALOGV(...)
#else
#define ALOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__);
#endif

#define ABORT_GAME { ALOGE("*** GAME ABORTING."); *((volatile char*)0) = 'a'; }
#define DEBUG_BLIP ALOGI("[ BLIP ]: %s:%d", __FILE__, __LINE__)

#define MY_ASSERT(cond) { if (!(cond)) { ALOGE("ASSERTION FAILED: %s", #cond); \
   ABORT_GAME; } }

#define BUFFER_OFFSET(i) ((char*)NULL + (i))

#include "our_key_codes.hpp"
