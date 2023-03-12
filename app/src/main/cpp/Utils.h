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

#ifndef ANDROID_RENDERSCRIPT_TOOLKIT_UTILS_H
#define ANDROID_RENDERSCRIPT_TOOLKIT_UTILS_H

#include <android/log.h>
#include <stddef.h>

namespace renderscript {
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using uchar = unsigned char;
using uint = unsigned int;
using ushort = unsigned short;

using uint8_t = uchar;
using uint16_t = ushort;
using uint32_t = uint;

typedef float float4 __attribute__((ext_vector_type(4)));
typedef uchar uchar4 __attribute__((ext_vector_type(4)));

template <typename TO, typename TI>
inline TO convert(TI i) {
    // assert(i.x >= 0 && i.y >= 0 && i.z >= 0 && i.w >= 0);
    // assert(i.x <= 255 && i.y <= 255 && i.z <= 255 && i.w <= 255);
    return __builtin_convertvector(i, TO);
}

/**
 * Returns true if the processor we're running on supports the SIMD instructions that are
 * used in our assembly code.
 */
bool cpuSupportsSimd();

inline size_t divideRoundingUp(size_t a, size_t b) {
    return a / b + (a % b == 0 ? 0 : 1);
}

}  // namespace renderscript

#endif  // ANDROID_RENDERSCRIPT_TOOLKIT_UTILS_H
