package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.QuestionEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChapterScreen(
    subjectName: String,
    isProcessing: Boolean,
    processingStatus: String,
    onBackClick: () -> Unit,
    onParseFileUri: suspend (Uri) -> String,
    onProcessAndSave: (title: String, topics: List<String>, rawContent: String, extractAuto: Boolean, manualQuestions: List<QuestionEntity>, parserMode: Int, rawStylePreset: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var topicInput by remember { mutableStateOf("") }
    val topicsList = remember { mutableStateListOf<String>() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Paste Text, 1 = Upload File
    var pastedText by remember { mutableStateOf("") }
    var uploadedFileName by remember { mutableStateOf("") }

    var parserMode by remember { mutableIntStateOf(0) } // 0 = Gemini AI, 1 = Basic Local, 2 = No Parser (Raw)
    var rawStylePreset by remember { mutableStateOf("samsung") } // samsung, monospace, standard

    var extractQuestionsAuto by remember { mutableStateOf(true) }
    val manualQuestions = remember { mutableStateListOf<QuestionEntity>() }

    // Dialog for adding manual question
    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var newQText by remember { mutableStateOf("") }
    var newOptA by remember { mutableStateOf("") }
    var newOptB by remember { mutableStateOf("") }
    var newOptC by remember { mutableStateOf("") }
    var newOptD by remember { mutableStateOf("") }
    var newCorrectOpt by remember { mutableIntStateOf(0) }
    var newExpl by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadedFileName = uri.lastPathSegment ?: "Document"
            coroutineScope.launch {
                val fileText = onParseFileUri(uri)
                pastedText = fileText
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Chapter - $subjectName", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("add_chapter_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(56.dp),
                        strokeWidth = 5.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Processing Study Material...",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = processingStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Formatting pages, headings, tables, diagrams & questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Chapter Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Chapter Title") },
                    placeholder = { Text("e.g. Chapter 1: Thermodynamics") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chapter_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Topics list input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        label = { Text("Add Topic Tag") },
                        placeholder = { Text("e.g. Laws, Formulas") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("topic_tag_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (topicInput.isNotBlank()) {
                                topicsList.add(topicInput.trim())
                                topicInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            .testTag("add_topic_tag_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Topic", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                if (topicsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topicsList.forEach { topic ->
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = topic,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { topicsList.remove(topic) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Content Input Section
                Text(
                    text = "Content Input",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Paste Text")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload File")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = { pastedText = it },
                        placeholder = { Text("Paste study content or leave blank to generate notes with AI in Reader...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("paste_content_text_area"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "✨ AI Empty Topic Support: You can save this topic without a source file. Then open it in Reader mode to ask AI to generate study notes or give it your entire syllabus!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clickable {
                                filePickerLauncher.launch("*/*")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uploadedFileName.isBlank()) "Tap to select PDF or TXT File" else "Selected: $uploadedFileName",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (pastedText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Extracted Text Preview (${pastedText.split("\\s+".toRegex()).size} words):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = pastedText,
                            onValueChange = { pastedText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Parsing Options Section
                Text(
                    text = "Parsing Options",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Option 1: Gemini AI Parser
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { parserMode = 0 }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = parserMode == 0, onClick = { parserMode = 0 })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Option 1 — Gemini AI Parser", fontWeight = FontWeight.Bold)
                                Text("Formats headings, tables, diagrams, callouts & auto questions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Option 2: Basic Local Parser
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { parserMode = 1 }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = parserMode == 1, onClick = { parserMode = 1 })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Option 2 — Basic Local Parser", fontWeight = FontWeight.Bold)
                                Text("Offline markdown parser for lists, ASCII tables & headings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Option 3: Direct Raw Text (No Parsing)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { parserMode = 2 }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = parserMode == 2, onClick = { parserMode = 2 })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Option 3 — Direct Raw Text (No Parsing)", fontWeight = FontWeight.Bold)
                                Text("Keeps exact original text as clean plain paragraphs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Questions Section
                Text(
                    text = "Questions & Test Prep",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { extractQuestionsAuto = !extractQuestionsAuto }
                        ) {
                            Checkbox(
                                checked = extractQuestionsAuto,
                                onCheckedChange = { extractQuestionsAuto = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Option A — Auto-extract Questions", fontWeight = FontWeight.Bold)
                                Text("AI finds MCQs automatically from content", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showAddQuestionDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_manual_question_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (manualQuestions.isEmpty()) "Option B — Add Manually" else "Option B — Add Manually (${manualQuestions.size} added)",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (manualQuestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            manualQuestions.forEachIndexed { idx, q ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${idx + 1}. ${q.questionText}", maxLines = 1, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { manualQuestions.removeAt(idx) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onProcessAndSave(
                                title.trim(),
                                topicsList.toList(),
                                pastedText.trim().ifBlank { "No content available for this chapter." },
                                extractQuestionsAuto,
                                manualQuestions.toList(),
                                parserMode,
                                rawStylePreset
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("process_and_save_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Process & Save Chapter", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Add Manual Question Dialog
        if (showAddQuestionDialog) {
            AlertDialog(
                onDismissRequest = { showAddQuestionDialog = false },
                title = { Text("Add Question Manually") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(
                            value = newQText,
                            onValueChange = { newQText = it },
                            label = { Text("Question Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newOptA,
                            onValueChange = { newOptA = it },
                            label = { Text("Option A") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = newOptB,
                            onValueChange = { newOptB = it },
                            label = { Text("Option B") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = newOptC,
                            onValueChange = { newOptC = it },
                            label = { Text("Option C") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = newOptD,
                            onValueChange = { newOptD = it },
                            label = { Text("Option D") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Correct Option:", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf("A", "B", "C", "D").forEachIndexed { index, label ->
                                RadioButton(
                                    selected = newCorrectOpt == index,
                                    onClick = { newCorrectOpt = index }
                                )
                                Text(label)
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newExpl,
                            onValueChange = { newExpl = it },
                            label = { Text("Explanation Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newQText.isNotBlank() && newOptA.isNotBlank() && newOptB.isNotBlank()) {
                            manualQuestions.add(
                                QuestionEntity(
                                    chapterId = 0,
                                    questionText = newQText.trim(),
                                    optionA = newOptA.trim(),
                                    optionB = newOptB.trim(),
                                    optionC = if (newOptC.isBlank()) "-" else newOptC.trim(),
                                    optionD = if (newOptD.isBlank()) "-" else newOptD.trim(),
                                    correctOption = newCorrectOpt,
                                    explanation = newExpl.trim()
                                )
                            )
                            showAddQuestionDialog = false
                            newQText = ""; newOptA = ""; newOptB = ""; newOptC = ""; newOptD = ""; newExpl = ""
                        }
                    }) {
                        Text("Add Question", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddQuestionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
