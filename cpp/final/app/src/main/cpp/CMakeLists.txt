#
# Copyright 2022 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

cmake_minimum_required(VERSION 3.6)
project(integrity_cpp_demo VERSION 1.0.0 LANGUAGES CXX)

find_package(curl REQUIRED CONFIG)
find_package(jsoncpp REQUIRED CONFIG)
find_package(game-activity REQUIRED CONFIG)

include("${PLAYCORE_LOCATION}/playcore.cmake")
add_playcore_static_library()

# Export GameActivity_onCreate(),
# Refer to: https://github.com/android-ndk/ndk/issues/381.
set(CMAKE_SHARED_LINKER_FLAGS
        "${CMAKE_SHARED_LINKER_FLAGS} -u GameActivity_onCreate")

# Set common compiler options
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17 -Wall")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")
add_definitions("-DIMGUI_IMPL_OPENGL_ES2")

# build Dear ImGui as a static lib
set(IMGUI_BASE_DIR "../../../../third-party/imgui")

add_library(imgui STATIC
        ${IMGUI_BASE_DIR}/imgui_demo.cpp
        ${IMGUI_BASE_DIR}/imgui_draw.cpp
        ${IMGUI_BASE_DIR}/imgui_tables.cpp
        ${IMGUI_BASE_DIR}/imgui_widgets.cpp
        ${IMGUI_BASE_DIR}/imgui.cpp
        ${IMGUI_BASE_DIR}/backends/imgui_impl_opengl3.cpp)

target_include_directories(imgui PRIVATE
        ${IMGUI_BASE_DIR})

# now build app's shared lib
add_library(game SHARED
        android_main.cpp
        demo_scene.cpp
        game_activity_included.cpp
        game_input_included.cpp
        http_client.cpp
        imgui_manager.cpp
        input_util.cpp
        client_manager.cpp
        jni_util.cpp
        json_util.cpp
        native_app_glue_included.cpp
        native_engine.cpp
        scene.cpp
        scene_manager.cpp
        util.cpp)

target_include_directories(game PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${PLAYCORE_LOCATION}/include
        ${IMGUI_BASE_DIR})

target_compile_options(game
        PRIVATE
        -std=c++17
        -Wall
        -Wextra-semi
        -Wshadow
        -Wshadow-field
        -O0)

# add lib dependencies
target_link_libraries(game
        android
        imgui
        playcore
        curl::curl
        jsoncpp::jsoncpp
        game-activity::game-activity
        atomic
        EGL
        GLESv2
        jnigraphics
        log
        z)
