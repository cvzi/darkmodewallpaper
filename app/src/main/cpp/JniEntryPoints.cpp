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

#include <android/bitmap.h>
#include <cassert>
#include <jni.h>

#include "RenderScriptToolkit.h"
#include "Utils.h"

#define LOG_TAG "renderscript.toolkit.JniEntryPoints"

using namespace renderscript;

class ByteArrayGuard {
private:
    JNIEnv *env;
    jbyteArray array;
    jbyte *data;

public:
    ByteArrayGuard(JNIEnv *env, jbyteArray array) : env{env}, array{array} {
        data = env->GetByteArrayElements(array, nullptr);
    }

    ~ByteArrayGuard() {
        env->ReleaseByteArrayElements(array, data, 0);
    }

    uint8_t *get() { return reinterpret_cast<uint8_t *>(data); }
};

class BitmapGuard {
private:
    JNIEnv *env;
    jobject bitmap;
    AndroidBitmapInfo info;
    int bytesPerPixel;
    void *bytes;
    bool valid;

public:
    BitmapGuard(JNIEnv *env, jobject jBitmap) : env{env}, bitmap{jBitmap}, bytes{nullptr} {
        valid = false;
        if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
            ALOGE("AndroidBitmap_getInfo failed");
            return;
        }
        if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 &&
            info.format != ANDROID_BITMAP_FORMAT_A_8) {
            ALOGE("AndroidBitmap in the wrong format");
            return;
        }
        bytesPerPixel = info.stride / info.width;
        if (bytesPerPixel != 1 && bytesPerPixel != 4) {
            ALOGE("Expected a vector size of 1 or 4. Got %d. Extra padding per line not currently "
                  "supported",
                  bytesPerPixel);
            return;
        }
        if (AndroidBitmap_lockPixels(env, bitmap, &bytes) != ANDROID_BITMAP_RESULT_SUCCESS) {
            ALOGE("AndroidBitmap_lockPixels failed");
            return;
        }
        valid = true;
    }

    ~BitmapGuard() {
        if (valid) {
            AndroidBitmap_unlockPixels(env, bitmap);
        }
    }

    uint8_t *get() const {
        assert(valid);
        return reinterpret_cast<uint8_t *>(bytes);
    }

    int width() const { return info.width; }

    int height() const { return info.height; }

    int vectorSize() const { return bytesPerPixel; }
};

/**
 * Copies the content of Kotlin Range2d object into the equivalent C++ struct.
 */
class RestrictionParameter {
private:
    bool isNull;
    Restriction restriction;

public:
    RestrictionParameter(JNIEnv *env, jobject jRestriction) : isNull{jRestriction == nullptr} {
        if (isNull) {
            return;
        }
        /* TODO Measure how long FindClass and related functions take. Consider passing the
         * four values instead. This would also require setting the default when Range2D is null.
         */
        jclass restrictionClass = env->FindClass("com/google/android/renderscript/Range2d");
        if (restrictionClass == nullptr) {
            ALOGE("RenderScriptToolit. Internal error. Could not find the Kotlin Range2d class.");
            isNull = true;
            return;
        }
        jfieldID startXId = env->GetFieldID(restrictionClass, "startX", "I");
        jfieldID startYId = env->GetFieldID(restrictionClass, "startY", "I");
        jfieldID endXId = env->GetFieldID(restrictionClass, "endX", "I");
        jfieldID endYId = env->GetFieldID(restrictionClass, "endY", "I");
        restriction.startX = env->GetIntField(jRestriction, startXId);
        restriction.startY = env->GetIntField(jRestriction, startYId);
        restriction.endX = env->GetIntField(jRestriction, endXId);
        restriction.endY = env->GetIntField(jRestriction, endYId);
    }

    Restriction *get() { return isNull ? nullptr : &restriction; }
};

extern "C" JNIEXPORT jlong JNICALL
Java_com_google_android_renderscript_Toolkit_createNative(JNIEnv * /*env*/, jobject /*thiz*/) {
    return reinterpret_cast<jlong>(new RenderScriptToolkit());
}

extern "C" JNIEXPORT void JNICALL Java_com_google_android_renderscript_Toolkit_destroyNative(
        JNIEnv * /*env*/, jobject /*thiz*/, jlong native_handle) {
    RenderScriptToolkit *toolkit = reinterpret_cast<RenderScriptToolkit *>(native_handle);
    delete toolkit;
}

extern "C" JNIEXPORT void JNICALL Java_com_google_android_renderscript_Toolkit_nativeBlur(
        JNIEnv *env, jobject /*thiz*/, jlong native_handle, jbyteArray input_array, jint vectorSize,
        jint size_x, jint size_y, jint radius, jbyteArray output_array, jobject restriction) {
    RenderScriptToolkit *toolkit = reinterpret_cast<RenderScriptToolkit *>(native_handle);
    RestrictionParameter restrict{env, restriction};
    ByteArrayGuard input{env, input_array};
    ByteArrayGuard output{env, output_array};

    toolkit->blur(input.get(), output.get(), size_x, size_y, vectorSize, radius, restrict.get());
}

extern "C" JNIEXPORT void JNICALL Java_com_google_android_renderscript_Toolkit_nativeBlurBitmap(
        JNIEnv *env, jobject /*thiz*/, jlong native_handle, jobject input_bitmap,
        jobject output_bitmap, jint radius, jobject restriction) {
    RenderScriptToolkit *toolkit = reinterpret_cast<RenderScriptToolkit *>(native_handle);
    RestrictionParameter restrict{env, restriction};
    BitmapGuard input{env, input_bitmap};
    BitmapGuard output{env, output_bitmap};

    toolkit->blur(input.get(), output.get(), input.width(), input.height(), input.vectorSize(),
                  radius, restrict.get());
}