package com.auracode.hiposim.feature.auth.domain

/** Shape check for emails typed in the forms. The API validates again. */
internal object EmailFormat {
    private val REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun isValid(email: String): Boolean = REGEX.matches(email.trim())
}
