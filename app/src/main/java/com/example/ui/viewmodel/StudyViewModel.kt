package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChapterEntity
import com.example.data.local.PageEntity
import com.example.data.local.QuestionEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.TestResultEntity
import com.example.data.model.MoshiProvider
import com.example.data.model.DocumentSelection
import com.example.data.model.PageContent
import com.example.data.model.PageElement
import com.example.data.model.UserAnswerRecord
import com.example.data.preferences.PreferencesManager
import com.example.data.repository.StudyRepository
import com.example.util.FileParserUtil
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository
    private val moshi = MoshiProvider.moshi

    val apiKey: StateFlow<String?>
    val selectedModel: StateFlow<String>
    val selectedImageModel: StateFlow<String>
    val themeMode: StateFlow<String>
    val isSetupCompleted: StateFlow<Boolean>
    val allSubjects: StateFlow<List<SubjectEntity>>
    val allChapters: StateFlow<List<ChapterEntity>>
    val allTestResults: StateFlow<List<TestResultEntity>>

    // Active Selection State
    private val _selectedSubject = MutableStateFlow<SubjectEntity?>(null)
    val selectedSubject: StateFlow<SubjectEntity?> = _selectedSubject.asStateFlow()

    val subjectChapters: StateFlow<List<ChapterEntity>>

    private val _selectedChapter = MutableStateFlow<ChapterEntity?>(null)
    val selectedChapter: StateFlow<ChapterEntity?> = _selectedChapter.asStateFlow()

    private val _chapterPages = MutableStateFlow<List<PageEntity>>(emptyList())
    val chapterPages: StateFlow<List<PageEntity>> = _chapterPages.asStateFlow()

    private val _chapterQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val chapterQuestions: StateFlow<List<QuestionEntity>> = _chapterQuestions.asStateFlow()

    // Loading & Operation State
    private val _isProcessingContent = MutableStateFlow(false)
    val isProcessingContent: StateFlow<Boolean> = _isProcessingContent.asStateFlow()

    private val _processingStatusMessage = MutableStateFlow("")
    val processingStatusMessage: StateFlow<String> = _processingStatusMessage.asStateFlow()

    // Map of chapterId -> status string for background batch processing
    private val _activeBackgroundProcessing = MutableStateFlow<Map<Long, String>>(emptyMap())
    val activeBackgroundProcessing: StateFlow<Map<Long, String>> = _activeBackgroundProcessing.asStateFlow()

    // Active Test Execution State
    private val _activeTestQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val activeTestQuestions: StateFlow<List<QuestionEntity>> = _activeTestQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap()) // QuestionIndex -> OptionSelected
    val userAnswers: StateFlow<Map<Int, Int>> = _userAnswers.asStateFlow()

    private val _testStartTime = MutableStateFlow(0L)
    private val _latestSavedTestResult = MutableStateFlow<TestResultEntity?>(null)
    val latestSavedTestResult: StateFlow<TestResultEntity?> = _latestSavedTestResult.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        val prefs = PreferencesManager(application)
        repository = StudyRepository(database.studyDao(), prefs)

        apiKey = repository.apiKey.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), prefs.getApiKey()
        )
        selectedModel = repository.selectedModel.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), prefs.getSelectedModel()
        )
        selectedImageModel = prefs.selectedImageModel.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), prefs.getSelectedImageModel()
        )
        themeMode = repository.themeMode.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), prefs.getThemeMode()
        )
        isSetupCompleted = repository.isSetupCompleted.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), prefs.isSetupCompleted()
        )
        allSubjects = repository.allSubjects.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allChapters = repository.allChapters.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allTestResults = repository.allTestResults.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        subjectChapters = combine(allChapters, _selectedSubject) { chapters, selected ->
            if (selected == null) emptyList() else chapters.filter { it.subjectId == selected.id }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun saveApiKey(key: String) = repository.saveApiKey(key)
    fun saveSelectedModel(model: String) = repository.saveSelectedModel(model)
    fun saveSelectedImageModel(model: String) {
        val prefs = PreferencesManager(getApplication())
        prefs.saveSelectedImageModel(model)
    }
    fun saveThemeMode(theme: String) = repository.saveThemeMode(theme)
    fun markSetupCompleted() = repository.markSetupCompleted()

    fun addSubject(name: String) {
        viewModelScope.launch {
            repository.addSubject(name)
        }
    }

    fun deleteSubject(id: Long) {
        viewModelScope.launch {
            repository.deleteSubject(id)
            if (_selectedSubject.value?.id == id) {
                _selectedSubject.value = null
            }
        }
    }

    fun selectSubject(subject: SubjectEntity) {
        _selectedSubject.value = subject
    }

    fun generateAndReplaceDiagramWithImage(
        chapterId: Long,
        globalIndex: Int,
        prompt: String
    ) {
        viewModelScope.launch {
            _isProcessingContent.value = true
            _processingStatusMessage.value = "Generating AI Diagram image..."
            try {
                val imageBase64OrUri = repository.generateImageForDiagram(prompt)
                if (imageBase64OrUri.isNotBlank()) {
                    val imageData = PageElement.ImageData(
                        caption = prompt.take(100),
                        imageUriOrBase64 = imageBase64OrUri,
                        isAiGenerated = true
                    )
                    repository.replaceElementAtGlobalIndex(chapterId, globalIndex, imageData)
                    _selectedChapter.value?.let { ch ->
                        if (ch.id == chapterId) selectChapter(ch)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isProcessingContent.value = false
                _processingStatusMessage.value = ""
            }
        }
    }

    fun selectChapter(chapter: ChapterEntity) {
        _selectedChapter.value = chapter
        viewModelScope.launch {
            repository.getPagesForChapter(chapter.id).collect { pages ->
                _chapterPages.value = pages
            }
        }
        viewModelScope.launch {
            repository.getQuestionsForChapter(chapter.id).collect { questions ->
                _chapterQuestions.value = questions
            }
        }
    }

    fun updateChapterProgress(chapterId: Long, pageIndex: Int, totalPages: Int) {
        viewModelScope.launch {
            repository.updateChapterProgress(chapterId, pageIndex, totalPages)
            val updated = repository.getChapterById(chapterId)
            if (updated != null && _selectedChapter.value?.id == chapterId) {
                _selectedChapter.value = updated
            }
        }
    }

    fun deleteChapter(id: Long) {
        viewModelScope.launch {
            repository.deleteChapter(id)
            _selectedSubject.value?.let { subject ->
                selectSubject(subject)
            }
        }
    }

    suspend fun parseFileUri(uri: Uri): String {
        return FileParserUtil.readContentFromUri(getApplication(), uri)
    }

    fun processAndSaveChapter(
        title: String,
        topics: List<String>,
        rawContent: String,
        extractQuestionsAuto: Boolean,
        manualQuestions: List<QuestionEntity>,
        parserMode: Int = 0,
        rawStylePreset: String = "samsung",
        onSuccess: () -> Unit
    ) {
        val subject = _selectedSubject.value ?: return
        viewModelScope.launch {
            _isProcessingContent.value = true
            _processingStatusMessage.value = "Processing and formatting study material..."

            try {
                val newChapterId = repository.processAndSaveChapter(
                    subjectId = subject.id,
                    title = title,
                    topics = topics,
                    rawContent = rawContent,
                    extractQuestionsAuto = extractQuestionsAuto,
                    manualQuestions = manualQuestions,
                    parserMode = parserMode,
                    rawStylePreset = rawStylePreset
                )

                _processingStatusMessage.value = "Saved successfully!"
                val chapter = repository.getChapterById(newChapterId)
                if (chapter != null) {
                    selectChapter(chapter)
                }
                selectSubject(subject)
                _isProcessingContent.value = false
                onSuccess()
            } catch (e: Exception) {
                _processingStatusMessage.value = "Error: ${e.localizedMessage}"
                _isProcessingContent.value = false
            }
        }
    }

    fun updatePageElementText(chapterId: Long, globalIndex: Int, oldText: String, newText: String) {
        viewModelScope.launch {
            repository.updatePageElementText(chapterId, globalIndex, oldText, newText)
            _selectedChapter.value?.let { chapter ->
                if (chapter.id == chapterId) {
                    selectChapter(chapter)
                }
            }
        }
    }

    fun insertElementAfter(chapterId: Long, globalIndex: Int, newElement: PageElement) {
        viewModelScope.launch {
            repository.insertElementAfterGlobalIndex(chapterId, globalIndex, newElement)
            _selectedChapter.value?.let { chapter ->
                if (chapter.id == chapterId) {
                    selectChapter(chapter)
                }
            }
        }
    }

    fun replaceElementAt(chapterId: Long, globalIndex: Int, newElement: PageElement) {
        viewModelScope.launch {
            repository.replaceElementAtGlobalIndex(chapterId, globalIndex, newElement)
            _selectedChapter.value?.let { chapter ->
                if (chapter.id == chapterId) {
                    selectChapter(chapter)
                }
            }
        }
    }

    fun replaceElementRangeAt(chapterId: Long, startIndex: Int, endIndex: Int, newElement: PageElement) {
        viewModelScope.launch {
            repository.replaceElementRangeAtGlobalIndex(chapterId, startIndex, endIndex, newElement)
            _selectedChapter.value?.let { chapter ->
                if (chapter.id == chapterId) {
                    selectChapter(chapter)
                }
            }
        }
    }

    fun deleteElementRangeAt(chapterId: Long, startIndex: Int, endIndex: Int) {
        viewModelScope.launch {
            repository.replaceElementRangeAtGlobalIndex(chapterId, startIndex, endIndex, null)
            _selectedChapter.value?.let { chapter ->
                if (chapter.id == chapterId) {
                    selectChapter(chapter)
                }
            }
        }
    }

    fun replaceDocumentSelection(chapterId: Long, selection: DocumentSelection, replacement: String) {
        viewModelScope.launch {
            repository.replaceDocumentSelection(chapterId, selection, replacement)
            _selectedChapter.value?.let { chapter ->
                if (chapter.id == chapterId) {
                    selectChapter(chapter)
                }
            }
        }
    }

    fun formatDocumentSelection(chapterId: Long, selection: DocumentSelection, prefix: String, suffix: String) {
        viewModelScope.launch {
            repository.formatDocumentSelection(chapterId, selection, prefix, suffix)
            _selectedChapter.value?.let { chapter ->
                if (chapter.id == chapterId) {
                    selectChapter(chapter)
                }
            }
        }
    }

    fun restoreChapterPages(chapterId: Long, pageSnapshots: List<PageEntity>) {
        viewModelScope.launch {
            repository.restoreChapterPages(chapterId, pageSnapshots)
            _selectedChapter.value?.let { chapter ->
                if (chapter.id == chapterId) {
                    selectChapter(chapter)
                }
            }
        }
    }

    fun updatePageText(chapterId: Long, pageIndex: Int, newText: String) {
        viewModelScope.launch {
            repository.updatePageText(chapterId, pageIndex, newText)
            _selectedChapter.value?.let { chapter ->
                if (chapter.id == chapterId) {
                    selectChapter(chapter)
                }
            }
        }
    }

    fun appendCalloutToPage(chapterId: Long, pageIndex: Int, title: String, calloutText: String) {
        viewModelScope.launch {
            repository.appendCalloutToPage(chapterId, pageIndex, title, calloutText)
            _selectedChapter.value?.let { chapter ->
                if (chapter.id == chapterId) {
                    selectChapter(chapter)
                }
            }
        }
    }

    fun runAiAssistantAction(instruction: String, targetText: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.runAiAssistantAction(instruction, targetText)
            onResult(result)
        }
    }

    // --- Test Execution Methods ---
    fun startTest(chapter: ChapterEntity, filterWrongOnly: List<QuestionEntity>? = null) {
        viewModelScope.launch {
            val questions = filterWrongOnly ?: repository.getQuestionsListForChapter(chapter.id).shuffled()
            _activeTestQuestions.value = questions
            _currentQuestionIndex.value = 0
            _userAnswers.value = emptyMap()
            _testStartTime.value = System.currentTimeMillis()
        }
    }

    fun answerCurrentQuestion(optionIndex: Int) {
        val currentIdx = _currentQuestionIndex.value
        if (!_userAnswers.value.containsKey(currentIdx)) {
            val updated = _userAnswers.value.toMutableMap()
            updated[currentIdx] = optionIndex
            _userAnswers.value = updated
        }
    }

    fun goToNextQuestion(): Boolean {
        val nextIdx = _currentQuestionIndex.value + 1
        return if (nextIdx < _activeTestQuestions.value.size) {
            _currentQuestionIndex.value = nextIdx
            true
        } else {
            false // Finished
        }
    }

    fun finishTestAndSaveResult(onResultSaved: (Long) -> Unit) {
        val chapter = _selectedChapter.value ?: return
        val subject = _selectedSubject.value
        val questions = _activeTestQuestions.value
        val answersMap = _userAnswers.value

        var score = 0
        val records = mutableListOf<UserAnswerRecord>()

        questions.forEachIndexed { index, q ->
            val userChoice = answersMap[index] ?: -1
            val isCorrect = userChoice == q.correctOption
            if (isCorrect) score++

            records.add(
                UserAnswerRecord(
                    questionId = q.id,
                    questionText = q.questionText,
                    selectedOption = userChoice,
                    correctOption = q.correctOption,
                    isCorrect = isCorrect,
                    explanation = q.explanation
                )
            )
        }

        val durationSec = (System.currentTimeMillis() - _testStartTime.value) / 1000
        val listType = Types.newParameterizedType(List::class.java, UserAnswerRecord::class.java)
        val adapter = moshi.adapter<List<UserAnswerRecord>>(listType)
        val userAnswersJson = adapter.toJson(records) ?: "[]"

        viewModelScope.launch {
            val resultId = repository.saveTestResult(
                chapterId = chapter.id,
                subjectId = chapter.subjectId,
                subjectName = subject?.name ?: "Subject",
                chapterTitle = chapter.title,
                score = score,
                totalQuestions = questions.size,
                timeTakenSeconds = durationSec,
                userAnswersJson = userAnswersJson
            )

            val savedResult = repository.getTestResultById(resultId)
            _latestSavedTestResult.value = savedResult
            onResultSaved(resultId)
        }
    }

    fun loadTestResult(resultId: Long) {
        viewModelScope.launch {
            _latestSavedTestResult.value = repository.getTestResultById(resultId)
        }
    }

    private val _lastContentMergeResult = MutableStateFlow<com.example.data.remote.ContentMergeResult?>(null)
    val lastContentMergeResult: StateFlow<com.example.data.remote.ContentMergeResult?> = _lastContentMergeResult.asStateFlow()

    private val _lastQuestionMergeResult = MutableStateFlow<com.example.data.remote.QuestionMergeResult?>(null)
    val lastQuestionMergeResult: StateFlow<com.example.data.remote.QuestionMergeResult?> = _lastQuestionMergeResult.asStateFlow()

    fun clearMergeResults() {
        _lastContentMergeResult.value = null
        _lastQuestionMergeResult.value = null
    }

    fun mergeChapterContent(
        chapterId: Long,
        newRawContent: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isProcessingContent.value = true
            _processingStatusMessage.value = "Gemini is comparing and merging chapter content..."
            try {
                val result = repository.mergeAndUpdateChapterContent(chapterId, newRawContent)
                _lastContentMergeResult.value = result
                // Refresh active chapter pages
                repository.getPagesForChapter(chapterId).collect { pages ->
                    _chapterPages.value = pages
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isProcessingContent.value = false
                _processingStatusMessage.value = ""
                onComplete()
            }
        }
    }

    fun mergeQuestions(
        chapterId: Long,
        newRawQuestionsText: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            _isProcessingContent.value = true
            _processingStatusMessage.value = "Gemini is extracting and merging questions..."
            try {
                val result = repository.mergeQuestionsForChapter(chapterId, newRawQuestionsText)
                _lastQuestionMergeResult.value = result
                // Refresh active questions
                _chapterQuestions.value = repository.getQuestionsListForChapter(chapterId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isProcessingContent.value = false
                _processingStatusMessage.value = ""
                onComplete()
            }
        }
    }

    fun addSingleQuestion(chapterId: Long, question: QuestionEntity) {
        viewModelScope.launch {
            repository.addSingleManualQuestion(chapterId, question)
            _chapterQuestions.value = repository.getQuestionsListForChapter(chapterId)
        }
    }

    fun updateChapterDetails(chapterId: Long, title: String, topics: List<String>) {
        viewModelScope.launch {
            repository.updateChapterDetails(chapterId, title, topics)
            val updated = repository.getChapterById(chapterId)
            if (updated != null) {
                _selectedChapter.value = updated
            }
            val subjectId = _selectedSubject.value?.id
            if (subjectId != null) {
                repository.getChaptersForSubject(subjectId).collect { list ->
                    _subjectChapters.value = list
                }
            }
        }
    }

    fun undoLastChapterUpdate(chapterId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            _isProcessingContent.value = true
            _processingStatusMessage.value = "Reverting to previous version..."
            try {
                repository.restoreChapterFromSnapshot(chapterId)
                val updated = repository.getChapterById(chapterId)
                if (updated != null) {
                    _selectedChapter.value = updated
                }
                _chapterQuestions.value = repository.getQuestionsListForChapter(chapterId)
                clearMergeResults()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isProcessingContent.value = false
                _processingStatusMessage.value = ""
                onComplete()
            }
        }
    }

    fun parseUserAnswerRecords(json: String): List<UserAnswerRecord> {
        return try {
            val listType = Types.newParameterizedType(List::class.java, UserAnswerRecord::class.java)
            val adapter = moshi.adapter<List<UserAnswerRecord>>(listType)
            adapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parsePageContent(json: String): PageContent {
        return try {
            val adapter = moshi.adapter(PageContent::class.java)
            adapter.fromJson(json) ?: PageContent()
        } catch (_: Exception) {
            PageContent()
        }
    }

    fun parseTopicsJson(json: String): List<String> {
        return try {
            val listType = Types.newParameterizedType(List::class.java, String::class.java)
            val adapter = moshi.adapter<List<String>>(listType)
            adapter.fromJson(json) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _selectedSubject.value = null
            _selectedChapter.value = null
            _subjectChapters.value = emptyList()
            _chapterPages.value = emptyList()
            _chapterQuestions.value = emptyList()
        }
    }
}
