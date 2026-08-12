package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChapterEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.TestResultEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    testResults: List<TestResultEntity>,
    subjects: List<SubjectEntity>,
    allChapters: List<ChapterEntity> = emptyList(),
    onBackClick: () -> Unit,
    onTestResultClick: (TestResultEntity) -> Unit,
    onStartTestForChapter: (ChapterEntity) -> Unit = {}
) {
    var selectedSubjectFilter by remember { mutableStateOf<String?>(null) }
    var isFilterMenuExpanded by remember { mutableStateOf(false) }

    var showStartTestDialog by remember { mutableStateOf(false) }
    var selectedSubjectForTest by remember { mutableStateOf<SubjectEntity?>(null) }
    var selectedChapterForTest by remember { mutableStateOf<ChapterEntity?>(null) }

    // Compute Overall Stats
    val totalQuestionsAttempted = testResults.sumOf { it.totalQuestions }
    val totalCorrectAnswers = testResults.sumOf { it.score }
    val overallAccuracyPct = if (totalQuestionsAttempted > 0) {
        (totalCorrectAnswers * 100) / totalQuestionsAttempted
    } else 0

    val subjectCounts = testResults.groupingBy { it.subjectName }.eachCount()
    val mostAttemptedSubject = subjectCounts.maxByOrNull { it.value }?.key ?: "N/A"

    val chapterAccuracies = testResults.groupBy { "${it.subjectName} - ${it.chapterTitle}" }
        .mapValues { entry ->
            val qCount = entry.value.sumOf { it.totalQuestions }
            val cCount = entry.value.sumOf { it.score }
            if (qCount > 0) (cCount * 100) / qCount else 100
        }
    val weakestChapter = chapterAccuracies.minByOrNull { it.value }?.key ?: "N/A"

    val filteredResults = testResults.filter { result ->
        if (selectedSubjectFilter == null) true else result.subjectName == selectedSubjectFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test History & Stats", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("history_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { isFilterMenuExpanded = true }) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(
                            expanded = isFilterMenuExpanded,
                            onDismissRequest = { isFilterMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Subjects", fontWeight = if (selectedSubjectFilter == null) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedSubjectFilter = null
                                    isFilterMenuExpanded = false
                                }
                            )
                            subjects.forEach { subject ->
                                DropdownMenuItem(
                                    text = { Text(subject.name, fontWeight = if (selectedSubjectFilter == subject.name) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedSubjectFilter = subject.name
                                        isFilterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Overall Stats Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Overall Performance",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Attempted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text("$totalQuestionsAttempted Qs", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

                        Column {
                            Text("Overall Accuracy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text("$overallAccuracyPct%", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Most Attempted", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(mostAttemptedSubject, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Weakest Topic", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            Text(weakestChapter, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            selectedSubjectForTest = subjects.firstOrNull()
                            selectedChapterForTest = allChapters.firstOrNull { it.subjectId == selectedSubjectForTest?.id }
                            showStartTestDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Take a Practice Test", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (filteredResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Test History Recorded",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select a subject and chapter to take a test now!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                selectedSubjectForTest = subjects.firstOrNull()
                                selectedChapterForTest = allChapters.firstOrNull { it.subjectId == selectedSubjectForTest?.id }
                                showStartTestDialog = true
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Take Test Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredResults, key = { it.id }) { result ->
                        val matchingChapter = allChapters.find { it.title == result.chapterTitle }
                        TestResultHistoryCard(
                            result = result,
                            onClick = { onTestResultClick(result) },
                            onRetakeTest = if (matchingChapter != null) {
                                { onStartTestForChapter(matchingChapter) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    if (showStartTestDialog) {
        AlertDialog(
            onDismissRequest = { showStartTestDialog = false },
            title = {
                Text("Select Test Subject & Topic", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Choose Subject:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (subjects.isEmpty()) {
                        Text("No subjects created yet. Add a subject first to take a test.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else {
                        LazyColumn(modifier = Modifier.height(120.dp)) {
                            items(subjects) { subj ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable {
                                            selectedSubjectForTest = subj
                                            selectedChapterForTest = allChapters.firstOrNull { it.subjectId == subj.id }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedSubjectForTest?.id == subj.id)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = subj.name,
                                        modifier = Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selectedSubjectForTest?.id == subj.id) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Choose Chapter/Topic:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))

                        val availableChapters = allChapters.filter { it.subjectId == selectedSubjectForTest?.id }
                        if (availableChapters.isEmpty()) {
                            Text("No chapters found in this subject.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(modifier = Modifier.height(140.dp)) {
                                items(availableChapters) { ch ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable {
                                                selectedChapterForTest = ch
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (selectedChapterForTest?.id == ch.id)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = ch.title,
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (selectedChapterForTest?.id == ch.id) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStartTestDialog = false
                        selectedChapterForTest?.let { ch ->
                            onStartTestForChapter(ch)
                        }
                    },
                    enabled = selectedChapterForTest != null
                ) {
                    Text("Start Test")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTestDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TestResultHistoryCard(
    result: TestResultEntity,
    onClick: () -> Unit,
    onRetakeTest: (() -> Unit)? = null
) {
    val accuracyPct = if (result.totalQuestions > 0) (result.score * 100) / result.totalQuestions else 0
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(result.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("test_history_item_${result.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${result.subjectName} — ${result.chapterTitle}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$accuracyPct%",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${result.score}/${result.totalQuestions} Correct",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            if (onRetakeTest != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onRetakeTest,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retake Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
