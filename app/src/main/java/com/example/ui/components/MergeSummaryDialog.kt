package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ContentMergeResult
import com.example.data.remote.QuestionMergeResult

@Composable
fun MergeSummaryDialog(
    contentResult: ContentMergeResult? = null,
    questionResult: QuestionMergeResult? = null,
    onConfirm: () -> Unit,
    onUndo: () -> Unit
) {
    var showReviewDetails by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { },
        modifier = Modifier.testTag("merge_summary_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Merge & Update Summary",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (contentResult != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "📖 Chapter Content Update",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            SummaryBadgeRow(
                                label = "New paragraphs added:",
                                value = "${contentResult.newParagraphsCount}",
                                badgeColor = Color(0xFF2E7D32)
                            )
                            SummaryBadgeRow(
                                label = "Sections merged:",
                                value = "${contentResult.mergedSectionsCount}",
                                badgeColor = Color(0xFF1565C0)
                            )
                            SummaryBadgeRow(
                                label = "Sections kept unchanged:",
                                value = "${contentResult.unchangedSectionsCount}",
                                badgeColor = Color.Gray
                            )
                            SummaryBadgeRow(
                                label = "Total pages:",
                                value = "was ${contentResult.wasTotalPages}, now ${contentResult.nowTotalPages}",
                                badgeColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                if (questionResult != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "❓ Test Questions Update",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            SummaryBadgeRow(
                                label = "New questions added:",
                                value = "${questionResult.newQuestionsCount}",
                                badgeColor = Color(0xFF2E7D32)
                            )
                            SummaryBadgeRow(
                                label = "Questions merged:",
                                value = "${questionResult.mergedQuestionsCount}",
                                badgeColor = Color(0xFF1565C0)
                            )
                            SummaryBadgeRow(
                                label = "Duplicates skipped:",
                                value = "${questionResult.skippedDuplicatesCount}",
                                badgeColor = Color.Gray
                            )
                        }
                    }
                }

                // Review Changes Toggle Button
                OutlinedButton(
                    onClick = { showReviewDetails = !showReviewDetails },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("review_changes_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (showReviewDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (showReviewDetails) "Hide Detailed Changes" else "Review Changes")
                }

                if (showReviewDetails) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .background(
                                MaterialTheme.colorScheme.background,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        if (contentResult != null) {
                            items(contentResult.pageDiffs) { diff ->
                                DiffCardItem(
                                    title = "Page ${diff.pageIndex + 1}",
                                    snippet = diff.text.take(120) + if (diff.text.length > 120) "..." else "",
                                    status = diff.status
                                )
                            }
                        }

                        if (questionResult != null) {
                            items(questionResult.questions) { q ->
                                DiffCardItem(
                                    title = q.questionText,
                                    snippet = "Explanation: ${q.explanation}",
                                    status = q.status
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_save_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm & Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onUndo,
                modifier = Modifier.testTag("undo_merge_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(imageVector = Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Undo")
            }
        }
    )
}

@Composable
private fun SummaryBadgeRow(
    label: String,
    value: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = badgeColor
            )
        }
    }
}

@Composable
private fun DiffCardItem(
    title: String,
    snippet: String,
    status: String
) {
    val (bgColor, borderColor, statusLabel) = when (status) {
        "ADDED" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "ADDED")
        "MERGED" -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), "MERGED")
        "SKIPPED" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "SKIPPED")
        else -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outlineVariant, "UNCHANGED")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(borderColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (snippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = snippet,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
