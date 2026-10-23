package com.swan1127.repland.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.swan1127.repland.domain.model.Task
import com.swan1127.repland.domain.model.TimeBlockValidator
import com.swan1127.repland.domain.model.TimelineEntry
import com.swan1127.repland.ui.components.DesignIcons
import java.time.LocalDate

/** A short agenda for the home page. Track editing belongs in the day view. */
@Composable
fun DailyDesk(
    date: LocalDate,
    entries: List<TimelineEntry>,
    unplacedCount: Int,
    unplacedTasks: List<Task>,
    plannedMinutes: Int,
    onOpenEntry: (String) -> Unit,
    onOpenTask: (Task) -> Unit,
    onOpenPlan: () -> Unit,
    onAdd: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().testTag("daily-desk"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("今天的日程", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onOpenPlan, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("全部日程"); Spacer(Modifier.width(4.dp)); Icon(DesignIcons.ArrowRight, null, Modifier.size(16.dp))
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (entries.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                            Icon(DesignIcons.Calendar, null, Modifier.padding(12.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("今天还未安排时段", style = MaterialTheme.typography.titleSmall)
                            Text("在助手中为已有任务预览计划。", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    entries.sortedBy(TimelineEntry::startMinute).take(4).forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f))
                        Row(Modifier.fillMaxWidth().clickable { onOpenEntry(entry.id) }
                            .heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.width(54.dp)) {
                                Text(TimeBlockValidator.formatTime(entry.startMinute), style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary)
                                Text(TimeBlockValidator.formatTime(entry.endMinute), style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(Modifier.width(3.dp).height(30.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                            Text(entry.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (entries.size > 4) TextButton(onClick = onOpenPlan) { Text("查看全部 ${entries.size} 项日程") }
                }
            }
        }
        if (unplacedTasks.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("待安排", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                    Text("$unplacedCount 件", Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            unplacedTasks.take(3).forEach { task ->
                Surface(onClick = { onOpenTask(task) }, color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(DesignIcons.Tasks, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(task.displayName, style = MaterialTheme.typography.titleMedium,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(task.totalDurationMinutes?.let { "$it 分钟" } ?: "时长待补充",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(DesignIcons.ArrowRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
