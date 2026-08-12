package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.AddQuestionsDialog
import com.example.ui.components.MergeSummaryDialog
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.FileParserUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditChapterScreen(
    chapterId: Long,
    viewModel: StudyViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val selectedChapter by viewModel.selectedChapter.collectAsState()
    val chapterQuestions by viewModel.chapterQuestions.collectAsState()
    val isProcessing by viewModel.isProcessingContent.collectAsState()
    val statusMessage by viewModel.processingStatusMessage.collectAsState()

    val lastContentMerge by viewModel.lastContentMergeResult.collectAsState()
    val lastQuestionMerge by viewModel.lastQuestionMergeResult.collectAsState()

    var chapterTitle by remember { mutableStateOf("") }
    val topicsList = remember { mutableStateListOf<String>() }
    var newTopicInput by remember { mutableStateOf("") }

    var newContentRawText by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf("") }

    var showAddQuestionsDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val content = FileParserUtil.readContentFromUri(context, uri)
                newContentRawText = content
                selectedFileName = FileParserUtil.getFileName(context, uri)
            }
        }
    }

    LaunchedEffect(chapterId) {
        if (selectedChapter != null && selectedChapter?.id == chapterId) {
            chapterTitle = selectedChapter!!.title
            topicsList.clear()
            topicsList.addAll(viewModel.parseTopicsJson(selectedChapter!!.topicsJson))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Chapter: ${selectedChapter?.title ?: ""}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Edit Chapter Details
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "✏️ Chapter Details",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = chapterTitle,
                            onValueChange = { chapterTitle = it },
                            label = { Text("Chapter Title") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_chapter_title_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Topics & Tags:", style = MaterialTheme.typography.labelMedium)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            topicsList.forEach { topic ->
                                InputChip(
                                    selected = true,
                                    onClick = { },
                                    label = { Text(topic) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { topicsList.remove(topic) },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove")
                                        }
                                    }
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newTopicInput,
                                onValueChange = { newTopicInput = it },
                                label = { Text("Add topic") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_topic_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (newTopicInput.isNotBlank()) {
                                        topicsList.add(newTopicInput.trim())
                                        newTopicInput = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Topic")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.updateChapterDetails(chapterId, chapterTitle, topicsList.toList())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_chapter_details_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Title & Topics")
                        }
                    }
                }

                // Section 2: Merge New Source Material
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📚 Add / Merge New Content",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Upload new notes, PDF, or text. Gemini will merge new information with existing material without deleting or summarizing existing text.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("upload_edit_content_file_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (selectedFileName.isNotBlank()) "File: $selectedFileName" else "Upload PDF or TXT Source")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newContentRawText,
                            onValueChange = { newContentRawText = it },
                            label = { Text("Or Paste New Content Text") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .testTag("paste_edit_content_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (newContentRawText.isNotBlank()) {
                                    viewModel.mergeChapterContent(chapterId, newContentRawText) {
                                        newContentRawText = ""
                                        selectedFileName = ""
                                    }
                                }
                            },
                            enabled = newContentRawText.isNotBlank() && !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("process_merge_content_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Process & Merge Content")
                        }
                    }
                }

                // Section 3: Questions Management
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "❓ Test Questions (${chapterQuestions.size})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = { showAddQuestionsDialog = true },
                                modifier = Modifier.testTag("edit_add_questions_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add / Merge")
                            }
                        }
                    }
                }
            }

            // Processing Overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = statusMessage,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // Merge Summary & Review Changes Dialog
            if (lastContentMerge != null || lastQuestionMerge != null) {
                MergeSummaryDialog(
                    contentResult = lastContentMerge,
                    questionResult = lastQuestionMerge,
                    onConfirm = {
                        viewModel.clearMergeResults()
                    },
                    onUndo = {
                        viewModel.undoLastChapterUpdate(chapterId) {
                            viewModel.clearMergeResults()
                        }
                    }
                )
            }

            // Add Questions Dialog
            if (showAddQuestionsDialog) {
                AddQuestionsDialog(
                    chapterId = chapterId,
                    onDismiss = { showAddQuestionsDialog = false },
                    onAddManualQuestion = { q ->
                        viewModel.addSingleQuestion(chapterId, q)
                    },
                    onExtractAndMergeFromFileOrText = { rawText ->
                        viewModel.mergeQuestions(chapterId, rawText) { }
                    }
                )
            }
        }
    }
}
