/*
 * Copyright (C) 2011 The Android Open Source Project
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

#include <stdint.h>
#include <x86intrin.h>

namespace renderscript {

/* Unsigned extend packed 8-bit integer (in LBS) into packed 32-bit integer */
static inline __m128i cvtepu8_epi32(__m128i x) {
#if defined(__SSE4_1__)
    return _mm_cvtepu8_epi32(x);
#elif defined(__SSSE3__)
    const __m128i M8to32 = _mm_set_epi32(0xffffff03, 0xffffff02, 0xffffff01, 0xffffff00);
    x = _mm_shuffle_epi8(x, M8to32);
    return x;
#else
#   error "Require at least SSSE3"
#endif
}

extern "C"

    void rsdIntrinsicBlurVFU4_K(void *dst,
                          const void *pin, int stride, const void *gptr,
                          int rct, int x1, int x2) {
    const char *pi;
    __m128i pi0, pi1;
    __m128 pf0, pf1;
    __m128 bp0, bp1;
    __m128 x;
    int r;

    for (; x1 < x2; x1 += 2) {
        pi = (const char *)pin + (x1 << 2);
        bp0 = _mm_setzero_ps();
        bp1 = _mm_setzero_ps();

        for (r = 0; r < rct; ++r) {
            x = _mm_load_ss((const float *)gptr + r);
            x = _mm_shuffle_ps(x, x, _MM_SHUFFLE(0, 0, 0, 0));

            pi0 = _mm_cvtsi32_si128(*(const int *)pi);
            pi1 = _mm_cvtsi32_si128(*((const int *)pi + 1));

            pf0 = _mm_cvtepi32_ps(cvtepu8_epi32(pi0));
            pf1 = _mm_cvtepi32_ps(cvtepu8_epi32(pi1));

            bp0 = _mm_add_ps(bp0, _mm_mul_ps(pf0, x));
            bp1 = _mm_add_ps(bp1, _mm_mul_ps(pf1, x));

            pi += stride;
        }

        _mm_storeu_ps((float *)dst, bp0);
        _mm_storeu_ps((float *)dst + 4, bp1);
        dst = (char *)dst + 32;
    }
}

void rsdIntrinsicBlurHFU4_K(void *dst,
                          const void *pin, const void *gptr,
                          int rct, int x1, int x2) {
    const __m128i Mu8 = _mm_set_epi32(0xffffffff, 0xffffffff, 0xffffffff, 0x0c080400);
    const float *pi;
    __m128 pf, x, y;
    __m128i o;
    int r;

    for (; x1 < x2; ++x1) {
        /* rct is define as 2*r+1 by the caller */
        x = _mm_load_ss((const float *)gptr);
        x = _mm_shuffle_ps(x, x, _MM_SHUFFLE(0, 0, 0, 0));

        pi = (const float *)pin + (x1 << 2);
        pf = _mm_mul_ps(x, _mm_load_ps(pi));

        for (r = 1; r < rct; r += 2) {
            x = _mm_load_ss((const float *)gptr + r);
            y = _mm_load_ss((const float *)gptr + r + 1);
            x = _mm_shuffle_ps(x, x, _MM_SHUFFLE(0, 0, 0, 0));
            y = _mm_shuffle_ps(y, y, _MM_SHUFFLE(0, 0, 0, 0));

            pf = _mm_add_ps(pf, _mm_mul_ps(x, _mm_load_ps(pi + (r << 2))));
            pf = _mm_add_ps(pf, _mm_mul_ps(y, _mm_load_ps(pi + (r << 2) + 4)));
        }

        o = _mm_cvtps_epi32(pf);
        *(int *)dst = _mm_cvtsi128_si32(_mm_shuffle_epi8(o, Mu8));
        dst = (char *)dst + 4;
    }
}

void rsdIntrinsicBlurHFU1_K(void *dst,
                          const void *pin, const void *gptr,
                          int rct, int x1, int x2) {
    const __m128i Mu8 = _mm_set_epi32(0xffffffff, 0xffffffff, 0xffffffff, 0x0c080400);
    const float *pi;
    __m128 pf, g0, g1, g2, g3, gx, p0, p1;
    __m128i o;
    int r;

    for (; x1 < x2; x1+=4) {
        g0 = _mm_load_ss((const float *)gptr);
        g0 = _mm_shuffle_ps(g0, g0, _MM_SHUFFLE(0, 0, 0, 0));

        pi = (const float *)pin + x1;
        pf = _mm_mul_ps(g0, _mm_loadu_ps(pi));

        for (r = 1; r < rct; r += 4) {
            gx = _mm_loadu_ps((const float *)gptr + r);
            p0 = _mm_loadu_ps(pi + r);
            p1 = _mm_loadu_ps(pi + r + 4);

            g0 = _mm_shuffle_ps(gx, gx, _MM_SHUFFLE(0, 0, 0, 0));
            pf = _mm_add_ps(pf, _mm_mul_ps(g0, p0));
            g1 = _mm_shuffle_ps(gx, gx, _MM_SHUFFLE(1, 1, 1, 1));
            pf = _mm_add_ps(pf, _mm_mul_ps(g1, _mm_alignr_epi8(p1, p0, 4)));
            g2 = _mm_shuffle_ps(gx, gx, _MM_SHUFFLE(2, 2, 2, 2));
            pf = _mm_add_ps(pf, _mm_mul_ps(g2, _mm_alignr_epi8(p1, p0, 8)));
            g3 = _mm_shuffle_ps(gx, gx, _MM_SHUFFLE(3, 3, 3, 3));
            pf = _mm_add_ps(pf, _mm_mul_ps(g3, _mm_alignr_epi8(p1, p0, 12)));
        }

        o = _mm_cvtps_epi32(pf);
        *(int *)dst = _mm_cvtsi128_si32(_mm_shuffle_epi8(o, Mu8));
        dst = (char *)dst + 4;
    }
}

    extern "C"

}  // namespace renderscript
