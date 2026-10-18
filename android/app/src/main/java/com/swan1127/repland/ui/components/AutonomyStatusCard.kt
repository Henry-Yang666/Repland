package com.swan1127.repland.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swan1127.repland.ui.theme.LocalReducedMotion

/** A quiet receipt for real planning state. This component owns no planning or undo state. */
@Composable
fun AutonomyStatusCard(
    enabled: Boolean,
    isBusy: Boolean,
    summary: String?,
    error: String?,
    canUndo: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val visibleError = error?.takeIf(String::isNotBlank)
    val visibleSummary = summary?.takeIf(String::isNotBlank)
    val title = when {
        isBusy -> "正在调整安排"
        !enabled -> "自动安排已暂停"
        visibleError != null -> "自动安排需要留意"
        else -> "自动安排已开启"
    }
    Surface(
        modifier = modifier.fillMaxWidth().testTag("autonomy-status-card"),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(color = colors.secondaryContainer, shape = RoundedCornerShape(10.dp)) {
                    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        when {
                            isBusy && !LocalReducedMotion.current -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.primary,
                            )
                            isBusy -> Icon(DesignIcons.Clock, null, Modifier.size(20.dp), tint = colors.primary)
                            else -> Icon(DesignIcons.Calendar, null, Modifier.size(18.dp),
                                tint = if (visibleError != null) colors.error else colors.primary)
                        }
                    }
                }
                Text(title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite })
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("autonomy-enabled-switch").semantics {
                        contentDescription = "自动安排"
                        stateDescription = if (enabled) "已开启" else "已暂停"
                    },
                )
            }
            if (visibleError != null) {
                Surface(color = colors.errorContainer, shape = MaterialTheme.shapes.small) {
                    Text(visibleError, Modifier.fillMaxWidth().padding(12.dp).testTag("autonomy-error")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium, color = colors.onErrorContainer)
                }
            }
            if (visibleSummary != null || canUndo) {
                HorizontalDivider(color = colors.outlineVariant)
                if (visibleSummary != null) Text(
                    visibleSummary,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("autonomy-receipt")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                if (canUndo) TextButton(
                    onClick = onUndo,
                    enabled = !isBusy,
                    modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp).testTag("autonomy-undo"),
                ) { Text("撤销上次调整") }
            }
        }
    }
}
