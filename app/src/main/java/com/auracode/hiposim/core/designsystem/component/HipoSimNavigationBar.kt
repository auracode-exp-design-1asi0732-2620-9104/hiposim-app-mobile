package com.auracode.hiposim.core.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.auracode.hiposim.R
import com.auracode.hiposim.core.designsystem.theme.HipoSimTheme

/** An entry of [HipoSimNavigationBar]. A [locked] item shows a padlock and still reports clicks. */
data class NavBarItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val locked: Boolean,
)

@Composable
fun HipoSimNavigationBar(
    items: List<NavBarItem>,
    selectedId: String,
    onItemClick: (NavBarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            val lockedDescription = stringResource(R.string.nav_locked_description, item.label)
            NavigationBarItem(
                selected = item.id == selectedId,
                onClick = { onItemClick(item) },
                modifier = if (item.locked) Modifier.semantics { contentDescription = lockedDescription } else Modifier,
                icon = {
                    if (item.locked) {
                        BadgedBox(
                            badge = {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = HipoSimTheme.extraColors.amount,
                                    modifier = Modifier.size(12.dp),
                                )
                            },
                        ) {
                            Icon(item.icon, contentDescription = null)
                        }
                    } else {
                        Icon(item.icon, contentDescription = null)
                    }
                },
                label = { Text(item.label) },
            )
        }
    }
}
