package com.auracode.hiposim.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import com.auracode.hiposim.R
import com.auracode.hiposim.core.designsystem.theme.Spacing
import com.auracode.hiposim.core.legal.LegalLinks

const val LEGAL_FOOTER_TAG = "legal_footer"

/**
 * Footer with the Terms and Conditions and the Privacy Policy (US17). Each link opens its section of the
 * Landing Page in the browser.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LegalFooter(
    modifier: Modifier = Modifier,
    termsUrl: String = LegalLinks.termsUrl(),
    privacyUrl: String = LegalLinks.privacyUrl(),
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().testTag(LEGAL_FOOTER_TAG),
        horizontalArrangement =
            Arrangement.spacedBy(
                Spacing.sm,
                alignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ),
    ) {
        LegalLink(
            text = stringResource(R.string.legal_terms),
            description = stringResource(R.string.legal_terms_description),
            url = termsUrl,
        )
        LegalLink(
            text = stringResource(R.string.legal_privacy),
            description = stringResource(R.string.legal_privacy_description),
            url = privacyUrl,
        )
    }
}

/** A text link that opens [url] in the browser. The touch target is at least 48 dp high. */
@Composable
fun LegalLink(
    text: String,
    description: String,
    url: String,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    TextButton(
        onClick = { uriHandler.openUri(url) },
        modifier =
            modifier
                .heightIn(min = Spacing.minTouchTarget)
                .semantics { contentDescription = description },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            textDecoration = TextDecoration.Underline,
        )
    }
}
