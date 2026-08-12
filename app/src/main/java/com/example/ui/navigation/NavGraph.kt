package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AddChapterScreen
import com.example.ui.screens.ApiKeySetupScreen
import com.example.ui.screens.EditChapterScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubjectScreen
import com.example.ui.screens.TestResultsScreen
import com.example.ui.screens.TestScreen
import com.example.ui.theme.StudyStudsTheme
import com.example.ui.viewmodel.StudyViewModel

object Routes {
    const val API_KEY_SETUP = "api_key_setup"
    const val HOME = "home"
    const val SUBJECT = "subject"
    const val ADD_CHAPTER = "add_chapter"
    const val READER = "reader"
    const val TEST = "test"
    const val EDIT_CHAPTER = "edit_chapter/{chapterId}"
    const val TEST_RESULTS = "test_results/{resultId}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun editChapterRoute(chapterId: Long) = "edit_chapter/$chapterId"
    fun testResultsRoute(resultId: Long) = "test_results/$resultId"
}

@Composable
fun StudyStudsApp(
    viewModel: StudyViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isSetupCompleted by viewModel.isSetupCompleted.collectAsState()

    val subjects by viewModel.allSubjects.collectAsState()
    val allChapters by viewModel.allChapters.collectAsState()
    val testResults by viewModel.allTestResults.collectAsState()

    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val subjectChapters by viewModel.subjectChapters.collectAsState()

    val selectedChapter by viewModel.selectedChapter.collectAsState()
    val chapterPages by viewModel.chapterPages.collectAsState()
    val chapterQuestions by viewModel.chapterQuestions.collectAsState()

    val isProcessingContent by viewModel.isProcessingContent.collectAsState()
    val processingStatusMessage by viewModel.processingStatusMessage.collectAsState()

    val activeTestQuestions by viewModel.activeTestQuestions.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val userAnswers by viewModel.userAnswers.collectAsState()
    val latestSavedTestResult by viewModel.latestSavedTestResult.collectAsState()

    StudyStudsTheme(themeMode = themeMode) {
        val startDestination = if (isSetupCompleted) Routes.HOME else Routes.API_KEY_SETUP

        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(Routes.API_KEY_SETUP) {
                ApiKeySetupScreen(
                    currentKey = apiKey,
                    currentModel = selectedModel,
                    onSaveKeyAndModel = { key, model ->
                        viewModel.saveApiKey(key)
                        viewModel.saveSelectedModel(model)
                        viewModel.markSetupCompleted()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.API_KEY_SETUP) { inclusive = true }
                        }
                    },
                    onSkip = {
                        viewModel.markSetupCompleted()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.API_KEY_SETUP) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                // Compute Chapter Counts & Progresses
                val chapterCounts = subjects.associate { s ->
                    s.id to subjectChapters.count { it.subjectId == s.id }
                }
                val progresses = subjects.associate { s ->
                    val sChapters = subjectChapters.filter { it.subjectId == s.id }
                    val totalPages = sChapters.sumOf { it.totalPages }
                    val readPages = sChapters.sumOf { if (it.totalPages > 0) (it.lastReadPage + 1).coerceAtMost(it.totalPages) else 0 }
                    val pct = if (totalPages > 0) readPages.toFloat() / totalPages.toFloat() else 0f
                    s.id to pct
                }

                HomeScreen(
                    subjects = subjects,
                    subjectChapterCounts = chapterCounts,
                    subjectProgresses = progresses,
                    currentTheme = themeMode,
                    onToggleTheme = {
                        val next = when (themeMode) {
                            "DARK" -> "LIGHT"
                            "LIGHT" -> "SYSTEM"
                            else -> "DARK"
                        }
                        viewModel.saveThemeMode(next)
                    },
                    onSubjectClick = { subject ->
                        viewModel.selectSubject(subject)
                        navController.navigate(Routes.SUBJECT)
                    },
                    onAddSubject = { name ->
                        viewModel.addSubject(name)
                    },
                    onNavigateHistory = {
                        navController.navigate(Routes.HISTORY)
                    },
                    onNavigateSettings = {
                        navController.navigate(Routes.SETTINGS)
                    }
                )
            }

            composable(Routes.SUBJECT) {
                val subject = selectedSubject
                if (subject != null) {
                    val latestScores = subjectChapters.associate { c ->
                        c.id to testResults.filter { it.chapterId == c.id }.maxByOrNull { it.timestamp }
                    }

                    SubjectScreen(
                        subject = subject,
                        chapters = subjectChapters,
                        latestScores = latestScores,
                        onBackClick = { navController.popBackStack() },
                        onChapterClick = { chapter ->
                            viewModel.selectChapter(chapter)
                            navController.navigate(Routes.READER)
                        },
                        onAddChapterClick = {
                            navController.navigate(Routes.ADD_CHAPTER)
                        },
                        onDeleteSubject = {
                            viewModel.deleteSubject(subject.id)
                            navController.popBackStack()
                        },
                        onDeleteChapter = { chapterId ->
                            viewModel.deleteChapter(chapterId)
                        },
                        onEditChapter = { chapterId ->
                            navController.navigate(Routes.editChapterRoute(chapterId))
                        },
                        onTakeTestClick = { chapter ->
                            viewModel.selectChapter(chapter)
                            viewModel.startTest(chapter)
                            navController.navigate(Routes.TEST)
                        }
                    )
                }
            }

            composable(Routes.ADD_CHAPTER) {
                val subject = selectedSubject
                if (subject != null) {
                    AddChapterScreen(
                        subjectName = subject.name,
                        isProcessing = isProcessingContent,
                        processingStatus = processingStatusMessage,
                        onBackClick = { navController.popBackStack() },
                        onParseFileUri = { uri -> viewModel.parseFileUri(uri) },
                        onProcessAndSave = { title, topics, rawContent, extractAuto, manualQuestions, parserMode, rawStylePreset ->
                            viewModel.processAndSaveChapter(
                                title = title,
                                topics = topics,
                                rawContent = rawContent,
                                extractQuestionsAuto = extractAuto,
                                manualQuestions = manualQuestions,
                                parserMode = parserMode,
                                rawStylePreset = rawStylePreset,
                                onSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    )
                }
            }

            composable(Routes.READER) {
                val chapter = selectedChapter
                val activeBgMap by viewModel.activeBackgroundProcessing.collectAsState()
                if (chapter != null) {
                    ReaderScreen(
                        chapter = chapter,
                        pages = chapterPages,
                        hasQuestions = chapterQuestions.isNotEmpty(),
                        onBackClick = { navController.popBackStack() },
                        onProgressUpdate = { pageIndex, totalPages ->
                            viewModel.updateChapterProgress(chapter.id, pageIndex, totalPages)
                        },
                        onParsePageContent = { json -> viewModel.parsePageContent(json) },
                        onTakeTestClick = {
                            viewModel.startTest(chapter)
                            navController.navigate(Routes.TEST)
                        },
                        onUpdatePageText = { chapterId, pageIndex, newText ->
                            viewModel.updatePageText(chapterId, pageIndex, newText)
                        },
                        onUpdateElementText = { chapterId, globalIndex, oldText, newText ->
                            viewModel.updatePageElementText(chapterId, globalIndex, oldText, newText)
                        },
                        onInsertElementAfter = { chapterId, globalIndex, newElem ->
                            viewModel.insertElementAfter(chapterId, globalIndex, newElem)
                        },
                        onReplaceElementAt = { chapterId, globalIndex, newElem ->
                            viewModel.replaceElementAt(chapterId, globalIndex, newElem)
                        },
                        onReplaceElementRange = { chapterId, startIdx, endIdx, newElem ->
                            viewModel.replaceElementRangeAt(chapterId, startIdx, endIdx, newElem)
                        },
                        onDeleteElementRange = { chapterId, startIdx, endIdx ->
                            viewModel.deleteElementRangeAt(chapterId, startIdx, endIdx)
                        },
                        onReplaceDocumentSelection = { chapterId, selection, replacement ->
                            viewModel.replaceDocumentSelection(chapterId, selection, replacement)
                        },
                        onFormatDocumentSelection = { chapterId, selection, prefix, suffix ->
                            viewModel.formatDocumentSelection(chapterId, selection, prefix, suffix)
                        },
                        onRestoreChapterPages = { chapterId, pageSnapshots ->
                            viewModel.restoreChapterPages(chapterId, pageSnapshots)
                        },
                        onAppendCallout = { chapterId, pageIndex, title, text ->
                            viewModel.appendCalloutToPage(chapterId, pageIndex, title, text)
                        },
                        onRunAiAction = { instruction, pageText, onResult ->
                            viewModel.runAiAssistantAction(instruction, pageText, onResult)
                        },
                        backgroundProcessingStatus = activeBgMap[chapter.id]
                    )
                }
            }

            composable(Routes.TEST) {
                val chapter = selectedChapter
                val lastQMerge by viewModel.lastQuestionMergeResult.collectAsState()
                if (chapter != null) {
                    TestScreen(
                        chapterId = chapter.id,
                        chapterTitle = chapter.title,
                        questions = if (activeTestQuestions.isNotEmpty()) activeTestQuestions else chapterQuestions,
                        currentIndex = currentQuestionIndex,
                        userAnswers = userAnswers,
                        lastQuestionMerge = lastQMerge,
                        onBackClick = { navController.popBackStack() },
                        onAnswerOption = { optionIdx ->
                            viewModel.answerCurrentQuestion(optionIdx)
                        },
                        onNextQuestion = {
                            viewModel.goToNextQuestion()
                        },
                        onFinishTest = {
                            viewModel.finishTestAndSaveResult { resultId ->
                                navController.navigate(Routes.testResultsRoute(resultId)) {
                                    popUpTo(Routes.TEST) { inclusive = true }
                                }
                            }
                        },
                        onAddManualQuestion = { q ->
                            viewModel.addSingleQuestion(chapter.id, q)
                        },
                        onMergeQuestionsText = { rawText ->
                            viewModel.mergeQuestions(chapter.id, rawText) { }
                        },
                        onConfirmMerge = {
                            viewModel.clearMergeResults()
                        },
                        onUndoMerge = {
                            viewModel.undoLastChapterUpdate(chapter.id) {
                                viewModel.clearMergeResults()
                            }
                        }
                    )
                }
            }

            composable(
                route = Routes.EDIT_CHAPTER,
                arguments = listOf(navArgument("chapterId") { type = NavType.LongType })
            ) { backStackEntry ->
                val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: 0L
                EditChapterScreen(
                    chapterId = chapterId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.TEST_RESULTS,
                arguments = listOf(navArgument("resultId") { type = NavType.LongType })
            ) { backStackEntry ->
                val resultId = backStackEntry.arguments?.getLong("resultId") ?: 0L
                val result = latestSavedTestResult
                if (result != null) {
                    val records = viewModel.parseUserAnswerRecords(result.userAnswersJson)
                    TestResultsScreen(
                        testResult = result,
                        userAnswerRecords = records,
                        onBackToChapter = {
                            navController.popBackStack(Routes.SUBJECT, false)
                        },
                        onRetryTest = {
                            val chapter = selectedChapter
                            if (chapter != null) {
                                viewModel.startTest(chapter)
                                navController.navigate(Routes.TEST) {
                                    popUpTo(Routes.TEST_RESULTS) { inclusive = true }
                                }
                            }
                        },
                        onReviewWeakQuestions = {
                            val chapter = selectedChapter
                            if (chapter != null) {
                                val wrongRecords = records.filter { !it.isCorrect }
                                val wrongQuestionIds = wrongRecords.map { it.questionId }.toSet()
                                val wrongQuestions = chapterQuestions.filter { wrongQuestionIds.contains(it.id) }
                                if (wrongQuestions.isNotEmpty()) {
                                    viewModel.startTest(chapter, wrongQuestions)
                                    navController.navigate(Routes.TEST) {
                                        popUpTo(Routes.TEST_RESULTS) { inclusive = true }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            composable(Routes.HISTORY) {
                HistoryScreen(
                    testResults = testResults,
                    subjects = subjects,
                    allChapters = allChapters,
                    onBackClick = { navController.popBackStack() },
                    onTestResultClick = { result ->
                        viewModel.loadTestResult(result.id)
                        navController.navigate(Routes.testResultsRoute(result.id))
                    },
                    onStartTestForChapter = { chapter ->
                        viewModel.selectChapter(chapter)
                        viewModel.startTest(chapter)
                        navController.navigate(Routes.TEST)
                    }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    currentApiKey = apiKey,
                    currentModel = selectedModel,
                    currentTheme = themeMode,
                    onBackClick = { navController.popBackStack() },
                    onSaveApiKey = { key -> viewModel.saveApiKey(key) },
                    onSaveSelectedModel = { model -> viewModel.saveSelectedModel(model) },
                    onSaveThemeMode = { theme -> viewModel.saveThemeMode(theme) },
                    onClearAllData = { viewModel.clearAllData() }
                )
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
