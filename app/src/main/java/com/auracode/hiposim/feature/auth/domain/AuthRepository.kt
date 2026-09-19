package com.auracode.hiposim.feature.auth.domain

import kotlinx.coroutines.flow.StateFlow

/** Accounts and session. The UI depends on this interface, never on an implementation. */
interface AuthRepository {
    val session: StateFlow<SessionState>

    /** Creates the account and, on success, signs the user in. */
    suspend fun register(form: RegistrationForm): RegisterResult

    suspend fun signOut()
}
