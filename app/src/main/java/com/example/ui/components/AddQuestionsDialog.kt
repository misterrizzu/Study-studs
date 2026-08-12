package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.QuestionEntity
import com.example.util.FileParserUtil

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AddQuestionsDialog(
    chapterId: Long,
    onDismiss: () -> Unit,
    onAddManualQuestion: (QuestionEntity) -> Unit,
    onExtractAndMergeFromFileOrText: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Manual Question State
    var questionText by remember { mutableStateOf("") }
    var optionA by remember { mutableStateOf("") }
    var optionB by remember { mutableStateOf("") }
    var optionC by remember { mutableStateOf("") }
    var optionD by remember { mutableStateOf("") }
    var correctOption by remember { mutableStateOf(0) }
    var explanation by remember { mutableStateOf("") }

    // File / Text State
    var pastedText by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val content = FileParserUtil.readContentFromUri(context, uri)
                pastedText = content
                fileName = FileParserUtil.getFileName(context, uri)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("add_questions_dialog"),
        title = {
            Text(
                text = "Add / Merge Questions",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Manual Input") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Upload / Paste") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // Manual Single Question Form
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("Question Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_question_text_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Options (select correct answer):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val options = listOf("A" to optionA, "B" to optionB, "C" to optionC, "D" to optionD)
                    val setters = listOf<(String) -> Unit>({ optionA = it }, { optionB = it }, { optionC = it }, { optionD = it })

                    options.forEachIndexed { idx, (label, valStr) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RadioButton(
                                selected = correctOption == idx,
                                onClick = { correctOption = idx }
                            )
                            OutlinedTextField(
                                value = valStr,
                                onValueChange = setters[idx],
                                label = { Text("Option $label") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_option_${label.lowercase()}_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = explanation,
                        onValueChange = { explanation = it },
                        label = { Text("Explanation") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_explanation_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    // Upload or Paste Questions Text
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "✨ Gemini Smart Question Merge",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Upload PDF/TXT or paste text. Gemini will extract new questions, check for duplicates, and combine explanations without deleting existing questions.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_questions_file_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (fileName.isNotBlank()) "Selected: $fileName" else "Upload PDF or TXT File")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = { pastedText = it },
                        label = { Text("Or Paste Questions Raw Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("paste_questions_text_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                Button(
                    onClick = {
                        if (questionText.isNotBlank() && optionA.isNotBlank() && optionB.isNotBlank()) {
                            val newQ = QuestionEntity(
                                chapterId = chapterId,
                                questionText = questionText.trim(),
                                optionA = optionA.trim(),
                                optionB = optionB.trim(),
                                optionC = optionC.trim().ifEmpty { "N/A" },
                                optionD = optionD.trim().ifEmpty { "N/A" },
                                correctOption = correctOption,
                                explanation = explanation.trim().ifEmpty { "No explanation provided." }
                            )
                            onAddManualQuestion(newQ)
                            onDismiss()
                        }
                    },
                    enabled = questionText.isNotBlank() && optionA.isNotBlank() && optionB.isNotBlank(),
                    modifier = Modifier.testTag("add_manual_question_submit_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Question")
                }
            } else {
                Button(
                    onClick = {
                        if (pastedText.isNotBlank()) {
                            onExtractAndMergeFromFileOrText(pastedText)
                            onDismiss()
                        }
                    },
                    enabled = pastedText.isNotBlank(),
                    modifier = Modifier.testTag("extract_merge_questions_submit_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Extract & Merge")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
