package com.decimal128.decimal

// FIXME: decide how to handle invalid syntax
//  libbid ignores all NaN payloads
//  decNumber keeps valid payloads
//  in both cases, invalid syntax becomes payload==0
//  that seems wrong to me ...
//  ... the whole idea of the payload for diagnostic purposes
//  is to know the origin of the NaN
internal const val NAN_INVALID_SYNTAX = 0
internal const val NAN_DIV_BY_ZERO = 2
internal const val NAN_DOWNGRADED_FROM_SNAN = 3
