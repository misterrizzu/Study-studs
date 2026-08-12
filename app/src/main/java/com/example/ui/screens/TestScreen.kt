package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.data.local.QuestionEntity
import com.example.data.remote.QuestionMergeResult
import com.example.ui.components.AddQuestionsDialog
import com.example.ui.components.MergeSummaryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    chapterId: Long,
    chapterTitle: String,
    questions: List<QuestionEntity>,
    currentIndex: Int,
    userAnswers: Map<Int, Int>,
    lastQuestionMerge: QuestionMergeResult? = null,
    onBackClick: () -> Unit,
    onAnswerOption: (optionIndex: Int) -> Unit,
    onNextQuestion: () -> Unit,
    onFinishTest: () -> Unit,
    onAddManualQuestion: (QuestionEntity) -> Unit = {},
    onMergeQuestionsText: (String) -> Unit = {},
    onConfirmMerge: () -> Unit = {},
    onUndoMerge: () -> Unit = {}
) {
    var showAddQuestionsDialog by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    val question = questions.getOrNull(currentIndex)
    val selectedOption = userAnswers[currentIndex]
    val isAnswered = selectedOption != null
    val totalQuestions = questions.size
    val progressPct = if (totalQuestions > 0) (currentIndex + 1).toFloat() / totalQuestions.toFloat() else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Test: $chapterTitle",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("test_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddQuestionsDialog = true },
                        modifier = Modifier.testTag("add_questions_test_screen_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add or Merge Questions")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF000000) else MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (question != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF),
                    tonalElevation = 8.dp
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        val isLast = currentIndex == totalQuestions - 1
                        Button(
                            onClick = {
                                if (isLast) onFinishTest() else onNextQuestion()
                            },
                            enabled = isAnswered,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("next_question_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3),
                                disabledContainerColor = Color(0xFF2196F3).copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = if (isLast) "View Results →" else "Next Question →",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF))
        ) {
            if (question == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No questions in this test yet.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAddQuestionsDialog = true },
                            modifier = Modifier.testTag("add_first_question_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Questions")
                        }
                    }
                }
            } else {
                // Top Progress Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Q ${currentIndex + 1}/$totalQuestions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                        Text(
                            text = "${(progressPct * 100).toInt()}% Completed",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progressPct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF2196F3),
                        trackColor = Color(0xFF2196F3).copy(alpha = 0.2f)
                    )
                }

                // Scrollable Content Column (Question Card, Options, Explanation)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // QUESTION CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF2196F3).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Question ${currentIndex + 1} of $totalQuestions",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = question.questionText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 24.sp,
                                color = if (isDark) Color.White else Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // OPTIONS (4 stacked buttons, min height 56dp, rounded 8dp, margin 8dp)
                    val optionsList = listOf(
                        0 to question.optionA,
                        1 to question.optionB,
                        2 to question.optionC,
                        3 to question.optionD
                    )

                    optionsList.forEach { (optionIdx, optionText) ->
                        val letter = when (optionIdx) {
                            0 -> "A"
                            1 -> "B"
                            2 -> "C"
                            else -> "D"
                        }
                        val isThisSelected = selectedOption == optionIdx
                        val isCorrectAnswer = optionIdx == question.correctOption

                        val cardBg = when {
                            isAnswered && isCorrectAnswer -> if (isDark) Color(0xFF1B382B) else Color(0xFFE8F5E9)
                            isAnswered && isThisSelected && !isCorrectAnswer -> if (isDark) Color(0xFF3A1C1C) else Color(0xFFFFEBEE)
                            else -> if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                        }

                        val borderColor = when {
                            isAnswered && isCorrectAnswer -> Color(0xFF4CAF50)
                            isAnswered && isThisSelected && !isCorrectAnswer -> Color(0xFFF44336)
                            else -> if (isDark) Color(0xFF333333) else Color(0xFFE0E0E0)
                        }

                        val textColor = when {
                            isAnswered && isCorrectAnswer -> Color(0xFF4CAF50)
                            isAnswered && isThisSelected && !isCorrectAnswer -> Color(0xFFF44336)
                            else -> if (isDark) Color.White else Color.Black
                        }

                        val circleBg = when {
                            isAnswered && isCorrectAnswer -> Color(0xFF4CAF50)
                            isAnswered && isThisSelected && !isCorrectAnswer -> Color(0xFFF44336)
                            else -> Color(0xFF2196F3).copy(alpha = 0.2f)
                        }

                        val circleTextColor = when {
                            isAnswered && (isCorrectAnswer || isThisSelected) -> Color.White
                            else -> Color(0xFF2196F3)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(vertical = 4.dp)
                                .clickable(enabled = !isAnswered) { onAnswerOption(optionIdx) }
                                .testTag("question_option_${letter}_button"),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(circleBg, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isAnswered && isCorrectAnswer) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = "Correct", tint = Color.White, modifier = Modifier.size(20.dp))
                                    } else if (isAnswered && isThisSelected && !isCorrectAnswer) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Wrong", tint = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Text(text = letter, fontWeight = FontWeight.Bold, color = circleTextColor, fontSize = 14.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = optionText,
                                    fontSize = 14.sp,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // EXPLANATION CARD (Appears below options on answer selection)
                    AnimatedVisibility(
                        visible = isAnswered,
                        enter = fadeIn()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1A2332) else Color(0xFFE3F2FD)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Explanation", fontWeight = FontWeight.Bold, color = Color(0xFF2196F3), fontSize = 15.sp)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = question.explanation.ifBlank { "No detailed explanation provided for this question." },
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (showAddQuestionsDialog) {
                AddQuestionsDialog(
                    chapterId = chapterId,
                    onDismiss = { showAddQuestionsDialog = false },
                    onAddManualQuestion = { q ->
                        onAddManualQuestion(q)
                    },
                    onExtractAndMergeFromFileOrText = { rawText ->
                        onMergeQuestionsText(rawText)
                    }
                )
            }

            if (lastQuestionMerge != null) {
                MergeSummaryDialog(
                    questionResult = lastQuestionMerge,
                    onConfirm = onConfirmMerge,
                    onUndo = onUndoMerge
                )
            }
        }
    }
}

