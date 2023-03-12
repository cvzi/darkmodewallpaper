/*
 * Copyright (C) 2021 The Android Open Source Project
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
 *
 * File changed by cvzi (cuzi-android@openmail.cc).
 *
 */

#include "Utils.h"

#include <cpu-features.h>

#include "RenderScriptToolkit.h"

namespace renderscript {

#define LOG_TAG "renderscript.toolkit.Utils"

bool cpuSupportsSimd() {
    AndroidCpuFamily family = android_getCpuFamily();
    uint64_t features = android_getCpuFeatures();

    if (family == ANDROID_CPU_FAMILY_ARM && (features & ANDROID_CPU_ARM_FEATURE_NEON)) {
        // ALOGI("Arm with Neon");
        return true;
    } else if (family == ANDROID_CPU_FAMILY_ARM64 && (features & ANDROID_CPU_ARM64_FEATURE_ASIMD)) {
        // ALOGI("Arm64 with ASIMD");
        return true;
    } else if ((family == ANDROID_CPU_FAMILY_X86 || family == ANDROID_CPU_FAMILY_X86_64) &&
               (features & ANDROID_CPU_X86_FEATURE_SSSE3)) {
        // ALOGI("x86* with SSE3");
        return true;
    }
    // ALOGI("Not simd");
    return false;
}

}  // namespace renderscript
