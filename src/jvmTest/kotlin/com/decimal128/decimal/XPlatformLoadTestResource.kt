package com.decimal128.decimal


fun loadTestResourceAsString(fileName: String): String? =
    object {}.javaClass.getResourceAsStream("/dectest/$fileName")?.bufferedReader()?.readText()