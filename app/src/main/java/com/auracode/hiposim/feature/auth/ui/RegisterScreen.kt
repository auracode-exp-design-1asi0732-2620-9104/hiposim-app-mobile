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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import com.auracode.hiposim.core.designsystem.component.ConsentCheckbox
import com.auracode.hiposim.core.designsystem.component.ErrorBanner
import com.auracode.hiposim.core.designsystem.component.HipoSimCard
import com.auracode.hiposim.core.designsystem.component.HipoSimTextField
import com.auracode.hiposim.core.designsystem.component.LegalLink
import com.auracode.hiposim.core.designsystem.component.PrimaryButton
import com.auracode.hiposim.core.designsystem.theme.Spacing
import com.auracode.hiposim.core.legal.LegalLinks
import com.auracode.hiposim.feature.auth.domain.RegistrationError
import com.auracode.hiposim.feature.auth.domain.RegistrationField

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isRegistered) {
        if (state.isRegistered) onRegistered()
    }

    RegisterContent(
        state = state,
        onBack = onBack,
        onFullNameChange = viewModel::onFullNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPhoneChange = viewModel::onPhoneChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onTermsChange = viewModel::onTermsChange,
        onConsentChange = viewModel::onConsentChange,
        onSubmit = viewModel::onSubmit,
        onNavigateToLogin = onNavigateToLogin,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterContent(
    state: RegisterUiState,
    onBack: () -> Unit,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToLogin: () -> Unit,
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
            state.quote?.let { QuoteCard(it) }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(R.string.register_headline),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.register_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            RegistrationForm(
                state = state,
                onFullNameChange = onFullNameChange,
                onEmailChange = onEmailChange,
                onPhoneChange = onPhoneChange,
                onPasswordChange = onPasswordChange,
                onTogglePasswordVisibility = onTogglePasswordVisibility,
                onTermsChange = onTermsChange,
                onConsentChange = onConsentChange,
                onSubmit = onSubmit,
                onNavigateToLogin = onNavigateToLogin,
            )

            Text(
                text = stringResource(R.string.register_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun QuoteCard(quote: QuoteSummaryUi) {
    HipoSimCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.register_quote_ready),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text =
                        stringResource(R.string.register_quote_payment, quote.monthlyPayment) +
                            " " + stringResource(R.string.per_month),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = stringResource(R.string.tcea_label) + " " + quote.tcea,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.register_quote_property),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = quote.propertyPrice,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RegistrationForm(
    state: RegisterUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val form = state.form
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        HipoSimTextField(
            value = form.fullName,
            onValueChange = onFullNameChange,
            label = stringResource(R.string.field_name),
            placeholder = stringResource(R.string.field_name_placeholder),
            leadingIcon = Icons.Filled.Person,
            errorText = fieldError(state, RegistrationField.FullName),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        HipoSimTextField(
            value = form.email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.field_email),
            placeholder = stringResource(R.string.field_email_placeholder),
            leadingIcon = Icons.Filled.Mail,
            errorText = fieldError(state, RegistrationField.Email),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        HipoSimTextField(
            value = form.phone,
            onValueChange = onPhoneChange,
            label = stringResource(R.string.field_phone) + " (" + stringResource(R.string.field_phone_hint) + ")",
            placeholder = stringResource(R.string.field_phone_placeholder),
            prefix = stringResource(R.string.field_phone_prefix),
            leadingIcon = Icons.Filled.Phone,
            supportingText = stringResource(R.string.field_phone_rule),
            errorText = fieldError(state, RegistrationField.Phone),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        HipoSimTextField(
            value = form.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.field_password),
            placeholder = stringResource(R.string.field_password_placeholder),
            leadingIcon = Icons.Filled.Lock,
            supportingText = stringResource(R.string.field_password_rule),
            errorText = fieldError(state, RegistrationField.Password),
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

        // Two separate acceptances, as the API asks for them. Neither starts checked.
        ConsentCheckbox(
            text = stringResource(R.string.terms_text),
            checked = form.termsAccepted,
            onCheckedChange = onTermsChange,
            error = fieldError(state, RegistrationField.Terms),
        )
        // The checkbox row toggles the acceptance, so the documents are separate links right below it.
        LegalLink(
            text = stringResource(R.string.legal_terms_read),
            description = stringResource(R.string.legal_terms_description),
            url = LegalLinks.termsUrl(),
        )
        ConsentCheckbox(
            text = stringResource(R.string.consent_text),
            checked = form.consentAccepted,
            onCheckedChange = onConsentChange,
            error = fieldError(state, RegistrationField.Consent),
        )
        LegalLink(
            text = stringResource(R.string.legal_privacy_read),
            description = stringResource(R.string.legal_privacy_description),
            url = LegalLinks.privacyUrl(),
        )

        state.failure?.let { ErrorBanner(stringResource(it.messageRes())) }

        PrimaryButton(
            text = stringResource(R.string.register_submit),
            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            onClick = onSubmit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )

        TextButton(
            onClick = onNavigateToLogin,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.minTouchTarget),
        ) {
            Text(text = stringResource(R.string.register_have_account), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** The client message when there is one, otherwise what the API said about the field. */
@Composable
private fun fieldError(
    state: RegisterUiState,
    field: RegistrationField,
): String? = state.errors[field]?.let { errorMessage(it) } ?: state.serverErrors[field]

@Composable
private fun errorMessage(error: RegistrationError): String =
    stringResource(
        when (error) {
            RegistrationError.NameRequired -> R.string.error_name_required
            RegistrationError.EmailInvalid -> R.string.error_email_invalid
            RegistrationError.PhoneInvalid -> R.string.error_phone_invalid
            RegistrationError.PasswordTooShort -> R.string.error_password_short
            RegistrationError.PasswordTooWeak -> R.string.error_password_weak
            RegistrationError.TermsRequired -> R.string.error_terms_required
            RegistrationError.ConsentRequired -> R.string.error_consent_required
            RegistrationError.EmailAlreadyRegistered -> R.string.error_email_taken
        },
    )
