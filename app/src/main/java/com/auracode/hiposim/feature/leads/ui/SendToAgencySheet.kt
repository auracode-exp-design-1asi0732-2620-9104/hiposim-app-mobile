package com.auracode.hiposim.feature.leads.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auracode.hiposim.R
import com.auracode.hiposim.core.designsystem.component.AccentButton
import com.auracode.hiposim.core.designsystem.component.ConsentCheckbox
import com.auracode.hiposim.core.designsystem.component.ErrorBanner
import com.auracode.hiposim.core.designsystem.component.OutlinedActionButton
import com.auracode.hiposim.core.designsystem.component.PrimaryButton
import com.auracode.hiposim.core.designsystem.component.SuccessBanner
import com.auracode.hiposim.core.designsystem.theme.Spacing
import com.auracode.hiposim.feature.leads.domain.LeadFailure

/**
 * Bottom sheet where a signed in buyer chooses an agency and authorizes sharing the simulation with it.
 * Nothing is sent without the explicit consent (Ley N° 29733).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendToAgencySheet(
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: SendToAgencyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onOpen() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SheetHeader(onDismiss)
            val agenciesFailure = state.agenciesFailure
            when {
                state.isSent -> SentContent(agencyName = state.sentToAgency.orEmpty(), onDone = onDismiss)
                state.isSessionExpired -> SessionExpiredContent(onSignIn)
                state.isLoadingAgencies -> LoadingAgencies()
                agenciesFailure != null ->
                    AgenciesFailureContent(agenciesFailure, onRetry = viewModel::onRetryAgencies)
                state.agencies.isEmpty() ->
                    Text(
                        text = stringResource(R.string.send_sheet_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                else ->
                    SendForm(
                        state = state,
                        onAgencySelected = viewModel::onAgencySelected,
                        onConsentChange = viewModel::onConsentChange,
                        onSend = viewModel::onSend,
                    )
            }
        }
    }
}

@Composable
private fun SheetHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(R.string.send_sheet_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.send_sheet_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
        }
    }
}

@Composable
private fun LoadingAgencies() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.minTouchTarget),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        Text(
            text = stringResource(R.string.send_sheet_loading),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AgenciesFailureContent(
    failure: LeadFailure,
    onRetry: () -> Unit,
) {
    ErrorBanner(stringResource(failure.messageRes()))
    OutlinedActionButton(
        text = stringResource(R.string.action_retry),
        icon = Icons.Filled.Refresh,
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SendForm(
    state: SendToAgencyUiState,
    onAgencySelected: (String) -> Unit,
    onConsentChange: (Boolean) -> Unit,
    onSend: () -> Unit,
) {
    Column(modifier = Modifier.selectableGroup()) {
        state.agencies.forEach { agency ->
            AgencyOption(
                agency = agency,
                selected = agency.id == state.selectedAgencyId,
                onSelect = { onAgencySelected(agency.id) },
            )
        }
        if (state.showAgencyError) {
            Text(
                text = stringResource(R.string.error_agency_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    ConsentCheckbox(
        text = stringResource(R.string.send_consent_text),
        checked = state.consentAccepted,
        onCheckedChange = onConsentChange,
        error = if (state.showConsentError) stringResource(R.string.error_send_consent_required) else null,
    )

    state.sendFailure?.let { ErrorBanner(stringResource(it.messageRes())) }

    AccentButton(
        text = stringResource(if (state.isSending) R.string.send_submitting else R.string.send_submit),
        icon = Icons.AutoMirrored.Filled.Send,
        onClick = onSend,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AgencyOption(
    agency: AgencyUi,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.minTouchTarget)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // The whole row is the touch target, so the radio button itself does not handle clicks.
        RadioButton(selected = selected, onClick = null)
        Text(
            text = agency.name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SentContent(
    agencyName: String,
    onDone: () -> Unit,
) {
    SuccessBanner(stringResource(R.string.send_success, agencyName))
    PrimaryButton(text = stringResource(R.string.send_done), onClick = onDone, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SessionExpiredContent(onSignIn: () -> Unit) {
    ErrorBanner(stringResource(R.string.error_session_expired))
    OutlinedActionButton(
        text = stringResource(R.string.login_submit),
        icon = Icons.AutoMirrored.Filled.Login,
        onClick = onSignIn,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun LeadFailure.messageRes(): Int =
    when (this) {
        LeadFailure.ConsentRequired -> R.string.error_send_consent_required
        LeadFailure.SimulationMissing, LeadFailure.NotFound -> R.string.error_send_not_found
        LeadFailure.Rejected -> R.string.error_send_rejected
        LeadFailure.SessionExpired -> R.string.error_session_expired
        LeadFailure.NoConnection -> R.string.error_no_connection
        LeadFailure.ServiceUnavailable -> R.string.error_service_unavailable
        LeadFailure.Unexpected -> R.string.error_unexpected
    }
