package com.auracode.hiposim.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AssuredWorkload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auracode.hiposim.R
import com.auracode.hiposim.core.designsystem.component.ErrorBanner
import com.auracode.hiposim.core.designsystem.component.HipoSimTextField
import com.auracode.hiposim.core.designsystem.component.PrimaryButton
import com.auracode.hiposim.core.designsystem.theme.Spacing
import com.auracode.hiposim.feature.auth.domain.LoginField
import com.auracode.hiposim.feature.auth.domain.LoginFormError

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }

    LoginContent(
        state = state,
        onBack = onBack,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onSubmit = viewModel::onSubmit,
        onNavigateToRegister = onNavigateToRegister,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginContent(
    state: LoginUiState,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AssuredWorkload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.register_back_to_results),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.login_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                HipoSimTextField(
                    value = state.form.email,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.field_email),
                    placeholder = stringResource(R.string.field_email_placeholder),
                    leadingIcon = Icons.Filled.Mail,
                    errorText = state.errors[LoginField.Email]?.let { errorMessage(it) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                HipoSimTextField(
                    value = state.form.password,
                    onValueChange = onPasswordChange,
                    label = stringResource(R.string.field_password),
                    leadingIcon = Icons.Filled.Lock,
                    errorText = state.errors[LoginField.Password]?.let { errorMessage(it) },
                    visualTransformation =
                        if (state.isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    trailingIcon = {
                        IconButton(onClick = onTogglePasswordVisibility) {
                            Icon(
                                imageVector =
                                    if (state.isPasswordVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                contentDescription =
                                    stringResource(
                                        if (state.isPasswordVisible) R.string.password_hide else R.string.password_show,
                                    ),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.failure?.let { ErrorBanner(failureMessage(it)) }

            PrimaryButton(
                text = stringResource(if (state.isSubmitting) R.string.login_submitting else R.string.login_submit),
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = onSubmit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            TextButton(
                onClick = onNavigateToRegister,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.minTouchTarget),
            ) {
                Text(text = stringResource(R.string.login_no_account), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun errorMessage(error: LoginFormError): String =
    stringResource(
        when (error) {
            LoginFormError.EmailInvalid -> R.string.error_email_invalid
            LoginFormError.PasswordRequired -> R.string.error_password_required
        },
    )

@Composable
private fun failureMessage(failure: LoginFailure): String =
    stringResource(
        when (failure) {
            LoginFailure.InvalidCredentials -> R.string.error_invalid_credentials
            LoginFailure.NotABuyer -> R.string.error_not_a_buyer
            LoginFailure.NoConnection -> R.string.error_no_connection
            LoginFailure.ServiceUnavailable -> R.string.error_service_unavailable
            LoginFailure.Unexpected -> R.string.error_unexpected
        },
    )
