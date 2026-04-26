package com.decimal128.decimal


actual fun loadTestResourceAsString(fileName: String): String? =
    object {}.javaClass.getResourceAsStream(fileName)?.bufferedReader()?.readText()