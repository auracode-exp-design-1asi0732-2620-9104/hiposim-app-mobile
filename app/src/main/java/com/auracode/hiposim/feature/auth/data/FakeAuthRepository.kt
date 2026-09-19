package com.auracode.hiposim.feature.auth.data

import com.auracode.hiposim.feature.auth.domain.AuthRepository
import com.auracode.hiposim.feature.auth.domain.AuthenticatedUser
import com.auracode.hiposim.feature.auth.domain.RegisterResult
import com.auracode.hiposim.feature.auth.domain.RegistrationForm
import com.auracode.hiposim.feature.auth.domain.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject

/**
 * Provisional in-memory accounts, until the backend exists.
 *
 * Nothing is persisted: accounts and the session are lost when the process dies. The password is
 * never stored because there is no sign in yet.
 *
 * TODO: replace with an API-backed repository (registration, sign in and token storage).
 */
class FakeAuthRepository
    @Inject
    constructor() : AuthRepository {
        private val lock = Any()
        private val registeredEmails = mutableSetOf<String>()
        private val _session = MutableStateFlow<SessionState>(SessionState.Guest)

        override val session: StateFlow<SessionState> = _session.asStateFlow()

        override suspend fun register(form: RegistrationForm): RegisterResult {
            val email = form.email.trim().lowercase()
            val isNew = synchronized(lock) { registeredEmails.add(email) }
            if (!isNew) return RegisterResult.EmailAlreadyRegistered

            val user =
                AuthenticatedUser(
                    id = UUID.randomUUID().toString(),
                    fullName = form.fullName.trim(),
                    email = email,
                    phone = form.phone.filterNot { it.isWhitespace() || it == '-' },
                )
            _session.value = SessionState.Authenticated(user)
            return RegisterResult.Success(user)
        }

        override suspend fun signOut() {
            _session.value = SessionState.Guest
        }
    }
