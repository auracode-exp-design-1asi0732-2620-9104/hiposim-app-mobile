package com.auracode.hiposim.testing

/** Reads a file from `src/test/resources`, for example `contract/simulation-response-with-bonus.json`. */
fun readTestResource(path: String): String =
    requireNotNull(object {}.javaClass.getResource("/$path")) { "Missing test resource: $path" }.readText()
