// file: jna_bid_dpd_shim.c
#include <stdint.h>
#include <stddef.h>
#include <string.h>

/* ========================= DPD (decNumber) ========================= */
#ifndef DECNUMDIGITS
#define DECNUMDIGITS 34   /* satisfies decimal128.h (>=34) and decimal64.h (>=16) */
#endif
#include "decNumber.h"
#include "decimal64.h"
#include "decimal128.h"

/* ========================= BID (Intel libbid) ====================== */
/* Match archive variant: 000 => by-value; no globals for rounding/flags */
#ifndef DECIMAL_CALL_BY_REFERENCE
#define DECIMAL_CALL_BY_REFERENCE 0
#endif
#ifndef DECIMAL_GLOBAL_ROUNDING
#define DECIMAL_GLOBAL_ROUNDING 0
#endif
#ifndef DECIMAL_GLOBAL_EXCEPTION_FLAGS
#define DECIMAL_GLOBAL_EXCEPTION_FLAGS 0
#endif

#include "bid_conf.h"      /* must come before bid_functions.h */
#include "bid_functions.h"

#if defined(__APPLE__)
#  define API __attribute__((visibility("default")))
#else
#  define API
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* ------------------------ decimal128 (DPD) ------------------------- */
API int d128_dpd_le_from_string(const char* s, uint8_t out16[16]) {
    if (!s || !out16) return -1;
    decContext ctx;
    decContextDefault(&ctx, DEC_INIT_DECIMAL128);
    decimal128 d;
    decimal128FromString(&d, s, &ctx);
    memcpy(out16, &d, 16);
    return 0;
}

API int d128_dpd_le_to_string(const uint8_t in16[16], char* out_str, int outCap) {
    if (!in16 || !out_str || outCap <= 0) return -1;
    decimal128 d;
    memcpy(&d, in16, 16);
    char buf[128];
    decimal128ToString(&d, buf);
    size_t n = strlen(buf);
    if ((int)n >= outCap) n = (size_t)(outCap - 1);
    memcpy(out_str, buf, n);
    out_str[n] = '\0';
    return 0;
}

/* ------------------------ decimal128 (BID) ------------------------- */
API int d128_bid_le_from_string(const char* s, uint8_t out16[16]) {
    if (!s || !out16) return -1;
    unsigned flags = 0;
    /* U3 "000" proto: BID_UINT128 bid128_from_string(char*, int rnd, unsigned* flags) */
    BID_UINT128 b = bid128_from_string((char*)s, BID_ROUNDING_TO_NEAREST, &flags);
    memcpy(out16, &b, 16);
    return 0;
}

API int d128_bid_le_to_string(const uint8_t in16[16], char* out_str, int outCap) {
    if (!in16 || !out_str || outCap <= 0) return -1;
    BID_UINT128 b;
    memcpy(&b, in16, 16);
    char buf[128];
    unsigned flags = 0;
    /* U3 "000" proto: void bid128_to_string(char* out, BID_UINT128 x, unsigned* flags) */
    bid128_to_string(buf, b, &flags);
    size_t n = strlen(buf);
    if ((int)n >= outCap) n = (size_t)(outCap - 1);
    memcpy(out_str, buf, n);
    out_str[n] = '\0';
    return 0;
}

/* ------------------------- decimal64 (DPD) ------------------------- */
API int d64_dpd_le_from_string(const char* s, uint8_t out8[8]) {
    if (!s || !out8) return -1;
    decContext ctx;
    decContextDefault(&ctx, DEC_INIT_DECIMAL64);
    decimal64 d;
    decimal64FromString(&d, s, &ctx);
    memcpy(out8, &d, 8);
    return 0;
}

API int d64_dpd_le_to_string(const uint8_t in8[8], char* out_str, int outCap) {
    if (!in8 || !out_str || outCap <= 0) return -1;
    decimal64 d;
    memcpy(&d, in8, 8);
    char buf[64];
    decimal64ToString(&d, buf);
    size_t n = strlen(buf);
    if ((int)n >= outCap) n = (size_t)(outCap - 1);
    memcpy(out_str, buf, n);
    out_str[n] = '\0';
    return 0;
}

/* ------------------------- decimal64 (BID) ------------------------- */
API int d64_bid_le_from_string(const char* s, uint8_t out8[8]) {
    if (!s || !out8) return -1;
    unsigned flags = 0;
    /* U3 "000" proto: BID_UINT64 bid64_from_string(char*, int rnd, unsigned* flags) */
    BID_UINT64 b = bid64_from_string((char*)s, BID_ROUNDING_TO_NEAREST, &flags);
    memcpy(out8, &b, 8);
    return 0;
}

API int d64_bid_le_to_string(const uint8_t in8[8], char* out_str, int outCap) {
    if (!in8 || !out_str || outCap <= 0) return -1;
    BID_UINT64 b;
    memcpy(&b, in8, 8);
    char buf[64];
    unsigned flags = 0;
    /* U3 "000" proto: void bid64_to_string(char* out, BID_UINT64 x, unsigned* flags) */
    bid64_to_string(buf, b, &flags);
    size_t n = strlen(buf);
    if ((int)n >= outCap) n = (size_t)(outCap - 1);
    memcpy(out_str, buf, n);
    out_str[n] = '\0';
    return 0;
}

#ifdef __cplusplus
} /* extern "C" */
#endif
