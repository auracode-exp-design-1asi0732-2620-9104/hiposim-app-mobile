package com.auracode.hiposim.feature.auth.domain

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveSessionUseCase
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        operator fun invoke(): StateFlow<SessionState> = repository.session
    }

class RegisterUserUseCase
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        suspend operator fun invoke(form: RegistrationForm): RegisterResult = repository.register(form)
    }

class SignOutUseCase
    @Inject
    constructor(
        private val repository: AuthRepository,
    ) {
        suspend operator fun invoke() = repository.signOut()
    }
