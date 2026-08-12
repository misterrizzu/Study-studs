package com.example.data.repository

import com.example.data.local.ChapterEntity
import com.example.data.local.PageEntity
import com.example.data.local.QuestionEntity
import com.example.data.local.StudyDao
import com.example.data.local.SubjectEntity
import com.example.data.local.TestResultEntity
import com.example.data.model.MoshiProvider
import com.example.data.model.PageContent
import com.example.data.model.PageElement
import com.example.data.preferences.PreferencesManager
import com.example.data.remote.GeminiProcessor
import com.example.data.remote.ProcessedChapterResult
import com.example.util.LocalContentProcessor
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class StudyRepository(
    private val studyDao: StudyDao,
    private val preferencesManager: PreferencesManager
) {
    private val geminiProcessor = GeminiProcessor()
    private val moshi = MoshiProvider.moshi
    private val pageAdapter = moshi.adapter(PageContent::class.java)

    // --- Preferences ---
    val apiKey: Flow<String?> = preferencesManager.apiKey
    val selectedModel: Flow<String> = preferencesManager.selectedModel
    val themeMode: Flow<String> = preferencesManager.themeMode
    val isSetupCompleted: Flow<Boolean> = preferencesManager.isSetupCompleted

    fun saveApiKey(key: String) = preferencesManager.saveApiKey(key)
    fun saveSelectedModel(model: String) = preferencesManager.saveSelectedModel(model)
    fun saveThemeMode(theme: String) = preferencesManager.saveThemeMode(theme)
    fun markSetupCompleted() = preferencesManager.markSetupCompleted()

    // --- Subjects ---
    val allSubjects: Flow<List<SubjectEntity>> = studyDao.getAllSubjects()

    suspend fun getSubjectById(id: Long): SubjectEntity? = studyDao.getSubjectById(id)

    suspend fun addSubject(name: String): Long {
        val subject = SubjectEntity(name = name.trim())
        return studyDao.insertSubject(subject)
    }

    suspend fun deleteSubject(id: Long) = studyDao.deleteSubject(id)

    // --- Chapters ---
    val allChapters: Flow<List<ChapterEntity>> = studyDao.getAllChapters()

    fun getChaptersForSubject(subjectId: Long): Flow<List<ChapterEntity>> =
        studyDao.getChaptersForSubject(subjectId)

    suspend fun getChapterById(id: Long): ChapterEntity? = studyDao.getChapterById(id)

    suspend fun deleteChapter(id: Long) = studyDao.deleteChapter(id)

    suspend fun updateChapterProgress(chapterId: Long, pageIndex: Int, totalPages: Int) {
        val isCompleted = pageIndex >= totalPages - 1
        studyDao.updateChapterProgress(chapterId, pageIndex, isCompleted)
    }

    suspend fun updateChapterDetails(chapterId: Long, title: String, topics: List<String>) {
        val chapter = studyDao.getChapterById(chapterId) ?: return
        val topicsJson = moshi.adapter(List::class.java).toJson(topics)
        val updated = chapter.copy(title = title, topicsJson = topicsJson)
        studyDao.updateChapter(updated)
    }

    suspend fun saveChapterSnapshot(chapterId: Long): Long {
        val chapter = studyDao.getChapterById(chapterId) ?: return -1
        val pages = studyDao.getPagesForChapter(chapterId).first()
        val questions = studyDao.getQuestionsListForChapter(chapterId)

        val pageEntitiesAdapter = moshi.adapter(Array<PageEntity>::class.java)
        val pagesJson = pageEntitiesAdapter.toJson(pages.toTypedArray())

        val questionsAdapter = moshi.adapter(Array<QuestionEntity>::class.java)
        val questionsJson = questionsAdapter.toJson(questions.toTypedArray())

        val history = com.example.data.local.ChapterHistoryEntity(
            chapterId = chapterId,
            title = chapter.title,
            topicsJson = chapter.topicsJson,
            pagesJson = pagesJson,
            questionsJson = questionsJson
        )
        return studyDao.insertChapterHistory(history)
    }

    suspend fun restoreChapterFromSnapshot(chapterId: Long): Boolean {
        val history = studyDao.getLatestChapterHistory(chapterId) ?: return false
        val currentChapter = studyDao.getChapterById(chapterId) ?: return false

        val pageEntitiesAdapter = moshi.adapter(Array<PageEntity>::class.java)
        val pages = pageEntitiesAdapter.fromJson(history.pagesJson)?.toList() ?: emptyList()

        val questionsAdapter = moshi.adapter(Array<QuestionEntity>::class.java)
        val questions = questionsAdapter.fromJson(history.questionsJson)?.toList() ?: emptyList()

        val updatedChapter = currentChapter.copy(
            title = history.title,
            topicsJson = history.topicsJson,
            totalPages = pages.size
        )
        studyDao.updateChapter(updatedChapter)

        studyDao.deletePagesForChapter(chapterId)
        studyDao.insertPages(pages)

        studyDao.deleteQuestionsForChapter(chapterId)
        if (questions.isNotEmpty()) {
            studyDao.insertQuestions(questions)
        }

        studyDao.deleteChapterHistory(chapterId)
        return true
    }

    suspend fun mergeAndUpdateChapterContent(
        chapterId: Long,
        newRawContent: String
    ): com.example.data.remote.ContentMergeResult {
        saveChapterSnapshot(chapterId)

        val chapter = studyDao.getChapterById(chapterId) ?: throw Exception("Chapter not found")
        val existingPages = studyDao.getPagesForChapter(chapterId).first()
        val existingRawText = existingPages.joinToString("\n\n") { it.contentRawText }

        val apiKeyVal = preferencesManager.getApiKey() ?: ""
        val modelVal = preferencesManager.getSelectedModel()

        val mergeResult = geminiProcessor.mergeChapterContent(
            apiKey = apiKeyVal,
            modelName = modelVal,
            existingRawContent = existingRawText,
            newRawContent = newRawContent,
            wasTotalPages = existingPages.size
        )

        studyDao.deletePagesForChapter(chapterId)
        val pageEntities = mergeResult.pages.mapIndexed { index, pageContent ->
            val contentJson = pageAdapter.toJson(pageContent) ?: "{}"
            val rawText = mergeResult.rawPageTexts.getOrNull(index) ?: ""
            PageEntity(
                chapterId = chapterId,
                pageIndex = index,
                contentRawText = rawText,
                contentJson = contentJson
            )
        }
        studyDao.insertPages(pageEntities)

        val updatedChapter = chapter.copy(totalPages = mergeResult.pages.size)
        studyDao.updateChapter(updatedChapter)

        return mergeResult
    }

    suspend fun mergeQuestionsForChapter(
        chapterId: Long,
        newRawQuestionsText: String
    ): com.example.data.remote.QuestionMergeResult {
        saveChapterSnapshot(chapterId)

        val existingQuestionsEntities = studyDao.getQuestionsListForChapter(chapterId)
        val existingQuestions = existingQuestionsEntities.map {
            com.example.data.remote.ExtractedQuestion(
                questionText = it.questionText,
                optionA = it.optionA,
                optionB = it.optionB,
                optionC = it.optionC,
                optionD = it.optionD,
                correctOption = it.correctOption,
                explanation = it.explanation
            )
        }

        val apiKeyVal = preferencesManager.getApiKey() ?: ""
        val modelVal = preferencesManager.getSelectedModel()

        val mergeResult = geminiProcessor.mergeQuestions(
            apiKey = apiKeyVal,
            modelName = modelVal,
            existingQuestions = existingQuestions,
            newRawQuestionsText = newRawQuestionsText
        )

        val newQuestionEntities = mergeResult.questions
            .filter { it.status != "SKIPPED" }
            .map { q ->
                QuestionEntity(
                    chapterId = chapterId,
                    questionText = q.questionText,
                    optionA = q.optionA,
                    optionB = q.optionB,
                    optionC = q.optionC,
                    optionD = q.optionD,
                    correctOption = q.correctOption,
                    explanation = q.explanation
                )
            }

        studyDao.deleteQuestionsForChapter(chapterId)
        if (newQuestionEntities.isNotEmpty()) {
            studyDao.insertQuestions(newQuestionEntities)
        }

        return mergeResult
    }

    suspend fun addSingleManualQuestion(
        chapterId: Long,
        question: QuestionEntity
    ) {
        studyDao.insertQuestions(listOf(question.copy(chapterId = chapterId)))
    }

    // --- Pages & Content Processing ---
    fun getPagesForChapter(chapterId: Long): Flow<List<PageEntity>> =
        studyDao.getPagesForChapter(chapterId)

    suspend fun processAndSaveChapter(
        subjectId: Long,
        title: String,
        topics: List<String>,
        rawContent: String,
        extractQuestionsAuto: Boolean,
        manualQuestions: List<QuestionEntity>,
        parserMode: Int = 0,
        rawStylePreset: String = "samsung"
    ): Long {
        val apiKeyVal = preferencesManager.getApiKey()
        val modelVal = preferencesManager.getSelectedModel()

        var result: ProcessedChapterResult? = null

        if (parserMode == 2) {
            // No Parser Mode
            result = LocalContentProcessor.processRawContentNoParsing(rawContent, rawStylePreset)
        } else if (parserMode == 1) {
            // Basic Local Parser Mode
            result = LocalContentProcessor.processContentLocally(rawContent, extractQuestionsAuto)
        } else {
            // Gemini AI Parser
            if (!apiKeyVal.isNullOrBlank()) {
                try {
                    result = geminiProcessor.processContent(
                        apiKey = apiKeyVal,
                        modelName = modelVal,
                        rawContent = rawContent,
                        extractQuestions = extractQuestionsAuto
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (result == null) {
                // Local fallback processing
                result = LocalContentProcessor.processContentLocally(
                    rawContent = rawContent,
                    extractQuestions = extractQuestionsAuto
                )
            }
        }

        val combinedTopics = mutableListOf<String>()
        topics.filter { it.isNotBlank() }.forEach { t ->
            if (!combinedTopics.any { it.equals(t.trim(), ignoreCase = true) }) {
                combinedTopics.add(t.trim())
            }
        }
        result.topics.filter { it.isNotBlank() }.forEach { t ->
            if (!combinedTopics.any { it.equals(t.trim(), ignoreCase = true) }) {
                combinedTopics.add(t.trim())
            }
        }
        if (combinedTopics.isEmpty()) {
            val titleTopic = title.replace(Regex("^(Chapter|Sec|Section|Unit)\\s*\\d*[:\\-]?\\s*", RegexOption.IGNORE_CASE), "").trim()
            if (titleTopic.isNotBlank()) {
                combinedTopics.add(titleTopic)
            } else {
                combinedTopics.add("General")
            }
        }

        val topicsJson = moshi.adapter(List::class.java).toJson(combinedTopics)
        val chapter = ChapterEntity(
            subjectId = subjectId,
            title = title,
            topicsJson = topicsJson,
            lastReadPage = 0,
            totalPages = result.pages.size,
            isCompleted = false
        )

        val chapterId = studyDao.insertChapter(chapter)

        // Save pages
        val pageEntities = result.pages.mapIndexed { index, pageContent ->
            val contentJson = pageAdapter.toJson(pageContent) ?: "{}"
            val rawText = result.rawPageTexts.getOrNull(index) ?: ""
            PageEntity(
                chapterId = chapterId,
                pageIndex = index,
                contentRawText = rawText,
                contentJson = contentJson
            )
        }
        studyDao.insertPages(pageEntities)

        // Save questions
        val questionsToSave = mutableListOf<QuestionEntity>()
        if (extractQuestionsAuto && result.questions.isNotEmpty()) {
            questionsToSave.addAll(result.questions.map {
                QuestionEntity(
                    chapterId = chapterId,
                    questionText = it.questionText,
                    optionA = it.optionA,
                    optionB = it.optionB,
                    optionC = it.optionC,
                    optionD = it.optionD,
                    correctOption = it.correctOption,
                    explanation = it.explanation
                )
            })
        }

        // Add manual questions
        for (q in manualQuestions) {
            questionsToSave.add(q.copy(chapterId = chapterId))
        }

        if (questionsToSave.isNotEmpty()) {
            studyDao.insertQuestions(questionsToSave)
        }

        return chapterId
    }

    // --- Questions & Tests ---
    fun getQuestionsForChapter(chapterId: Long): Flow<List<QuestionEntity>> =
        studyDao.getQuestionsForChapter(chapterId)

    suspend fun getQuestionsListForChapter(chapterId: Long): List<QuestionEntity> =
        studyDao.getQuestionsListForChapter(chapterId)

    suspend fun saveTestResult(
        chapterId: Long,
        subjectId: Long,
        subjectName: String,
        chapterTitle: String,
        score: Int,
        totalQuestions: Int,
        timeTakenSeconds: Long,
        userAnswersJson: String
    ): Long {
        val result = TestResultEntity(
            chapterId = chapterId,
            subjectId = subjectId,
            subjectName = subjectName,
            chapterTitle = chapterTitle,
            score = score,
            totalQuestions = totalQuestions,
            timeTakenSeconds = timeTakenSeconds,
            userAnswersJson = userAnswersJson
        )
        return studyDao.insertTestResult(result)
    }

    suspend fun getLatestTestResultForChapter(chapterId: Long): TestResultEntity? =
        studyDao.getLatestTestResultForChapter(chapterId)

    val allTestResults: Flow<List<TestResultEntity>> = studyDao.getAllTestResults()

    suspend fun getTestResultById(id: Long): TestResultEntity? = studyDao.getTestResultById(id)

    suspend fun clearAllData() {
        studyDao.clearAllSubjects()
        studyDao.clearAllTestResults()
        preferencesManager.clearAllData()
    }

    // --- Reader Editing & AI Assistant ---
    suspend fun updatePageElementText(chapterId: Long, globalElementIndex: Int, oldText: String, newElementText: String) {
        val existingPages = studyDao.getPagesForChapter(chapterId).first()
        if (existingPages.isEmpty()) return

        var currentGlobalIndex = 0
        var elementUpdated = false

        val updatedPages = existingPages.map { pageEntity ->
            if (elementUpdated) return@map pageEntity

            val currentContent = try {
                pageAdapter.fromJson(pageEntity.contentJson) ?: PageContent()
            } catch (e: Exception) {
                PageContent()
            }

            val mutableElements = currentContent.elements.toMutableList()
            var pageModified = false

            for (idx in mutableElements.indices) {
                val oldElem = mutableElements[idx]
                val oldElemText = when (oldElem) {
                    is PageElement.Heading -> oldElem.text
                    is PageElement.Paragraph -> oldElem.text
                    is PageElement.Callout -> oldElem.text
                    is PageElement.RawText -> oldElem.text
                    else -> ""
                }

                val cleanOld = oldText.replace(Regex("(?i)<font color=\"[^\"]+\">|</font>|<size scale=\"[^\"]+\">|</size>"), "").trim()
                val cleanElem = oldElemText.replace(Regex("(?i)<font color=\"[^\"]+\">|</font>|<size scale=\"[^\"]+\">|</size>"), "").trim()

                val isTarget = (currentGlobalIndex == globalElementIndex) ||
                               (cleanOld.isNotBlank() && cleanElem == cleanOld)

                if (isTarget) {
                    val updatedElem = when (oldElem) {
                        is PageElement.Heading -> oldElem.copy(text = newElementText)
                        is PageElement.Paragraph -> oldElem.copy(text = newElementText)
                        is PageElement.Callout -> oldElem.copy(text = newElementText)
                        is PageElement.RawText -> oldElem.copy(text = newElementText)
                        else -> oldElem
                    }
                    mutableElements[idx] = updatedElem
                    pageModified = true
                    elementUpdated = true
                }

                currentGlobalIndex++
                if (elementUpdated) break
            }

            if (pageModified) {
                val newContent = PageContent(mutableElements)
                val updatedJson = pageAdapter.toJson(newContent) ?: "{}"
                val rawText = mutableElements.joinToString("\n") {
                    when (it) {
                        is PageElement.Heading -> it.text
                        is PageElement.Paragraph -> it.text
                        is PageElement.Callout -> it.text
                        is PageElement.RawText -> it.text
                        else -> ""
                    }
                }
                pageEntity.copy(contentJson = updatedJson, contentRawText = rawText)
            } else {
                pageEntity
            }
        }

        if (elementUpdated) {
            studyDao.insertPages(updatedPages)
        }
    }

    suspend fun insertElementAfterGlobalIndex(chapterId: Long, globalElementIndex: Int, newElement: PageElement) {
        val existingPages = studyDao.getPagesForChapter(chapterId).first()
        if (existingPages.isEmpty()) return

        var currentGlobalIndex = 0
        var inserted = false

        val updatedPages = existingPages.map { pageEntity ->
            if (inserted) return@map pageEntity

            val currentContent = try {
                pageAdapter.fromJson(pageEntity.contentJson) ?: PageContent()
            } catch (e: Exception) {
                PageContent()
            }

            val mutableElements = currentContent.elements.toMutableList()
            var pageModified = false

            for (idx in mutableElements.indices) {
                if (currentGlobalIndex == globalElementIndex) {
                    mutableElements.add(idx + 1, newElement)
                    pageModified = true
                    inserted = true
                    break
                }
                currentGlobalIndex++
            }

            if (pageModified) {
                val newContent = PageContent(mutableElements)
                val updatedJson = pageAdapter.toJson(newContent) ?: "{}"
                val rawText = mutableElements.joinToString("\n") { el ->
                    when (el) {
                        is PageElement.Heading -> el.text
                        is PageElement.Paragraph -> el.text
                        is PageElement.Callout -> el.text
                        is PageElement.RawText -> el.text
                        else -> ""
                    }
                }
                pageEntity.copy(contentJson = updatedJson, contentRawText = rawText)
            } else {
                pageEntity
            }
        }

        if (!inserted && updatedPages.isNotEmpty()) {
            val lastPage = updatedPages.last()
            val currentContent = try {
                pageAdapter.fromJson(lastPage.contentJson) ?: PageContent()
            } catch (e: Exception) {
                PageContent()
            }
            val mutableElements = currentContent.elements.toMutableList()
            mutableElements.add(newElement)
            val newContent = PageContent(mutableElements)
            val updatedJson = pageAdapter.toJson(newContent) ?: "{}"
            val finalPages = updatedPages.dropLast(1) + lastPage.copy(contentJson = updatedJson)
            studyDao.insertPages(finalPages)
            return
        }

        if (inserted) {
            studyDao.insertPages(updatedPages)
        }
    }

    suspend fun replaceElementAtGlobalIndex(chapterId: Long, globalElementIndex: Int, newElement: PageElement) {
        replaceElementRangeAtGlobalIndex(chapterId, globalElementIndex, globalElementIndex, newElement)
    }

    suspend fun replaceElementRangeAtGlobalIndex(
        chapterId: Long,
        startIndex: Int,
        endIndex: Int,
        newElement: PageElement?
    ) {
        val existingPages = studyDao.getPagesForChapter(chapterId).first()
        if (existingPages.isEmpty()) return

        var currentGlobalIndex = 0
        val minIdx = minOf(startIndex, endIndex)
        val maxIdx = maxOf(startIndex, endIndex)
        var hasInsertedNew = false

        val updatedPages = existingPages.map { pageEntity ->
            val currentContent = try {
                pageAdapter.fromJson(pageEntity.contentJson) ?: PageContent()
            } catch (e: Exception) {
                PageContent()
            }

            val mutableElements = currentContent.elements
            val newElementsForPage = mutableListOf<PageElement>()
            var pageModified = false

            for (idx in mutableElements.indices) {
                if (currentGlobalIndex in minIdx..maxIdx) {
                    pageModified = true
                    if (currentGlobalIndex == minIdx && newElement != null && !hasInsertedNew) {
                        newElementsForPage.add(newElement)
                        hasInsertedNew = true
                    }
                } else {
                    newElementsForPage.add(mutableElements[idx])
                }
                currentGlobalIndex++
            }

            if (pageModified) {
                val newContent = PageContent(newElementsForPage)
                val updatedJson = pageAdapter.toJson(newContent) ?: "{}"
                val rawText = newElementsForPage.joinToString("\n") { el ->
                    when (el) {
                        is PageElement.Heading -> el.text
                        is PageElement.Paragraph -> el.text
                        is PageElement.Callout -> el.text
                        is PageElement.RawText -> el.text
                        else -> ""
                    }
                }
                pageEntity.copy(contentJson = updatedJson, contentRawText = rawText)
            } else {
                pageEntity
            }
        }

        studyDao.insertPages(updatedPages)
    }

    suspend fun updatePageText(chapterId: Long, pageIndex: Int, newText: String) {
        val existingPages = studyDao.getPagesForChapter(chapterId).first()
        val pageToUpdate = existingPages.find { it.pageIndex == pageIndex }

        val newElement = PageElement.Paragraph(newText)
        val contentJson = pageAdapter.toJson(PageContent(listOf(newElement))) ?: "{}"

        if (pageToUpdate != null) {
            val updated = pageToUpdate.copy(
                contentRawText = newText,
                contentJson = contentJson
            )
            studyDao.insertPages(listOf(updated))
        } else {
            val newPage = PageEntity(
                chapterId = chapterId,
                pageIndex = pageIndex,
                contentRawText = newText,
                contentJson = contentJson
            )
            studyDao.insertPages(listOf(newPage))
        }
    }

    suspend fun appendCalloutToPage(chapterId: Long, pageIndex: Int, title: String, calloutText: String) {
        val existingPages = studyDao.getPagesForChapter(chapterId).first()
        val pageToUpdate = existingPages.find { it.pageIndex == pageIndex }

        if (pageToUpdate != null) {
            val currentContent = try {
                pageAdapter.fromJson(pageToUpdate.contentJson) ?: PageContent()
            } catch (e: Exception) {
                PageContent()
            }
            val newElements = currentContent.elements.toMutableList()
            newElements.add(PageElement.Callout(title = title, text = calloutText, type = "HIGHLIGHT"))

            val updatedJson = pageAdapter.toJson(PageContent(newElements)) ?: "{}"
            val updatedPage = pageToUpdate.copy(contentJson = updatedJson)
            studyDao.insertPages(listOf(updatedPage))
        }
    }

    suspend fun runAiAssistantAction(instruction: String, targetText: String): String {
        val apiKeyVal = preferencesManager.getApiKey()
        val modelVal = preferencesManager.getSelectedModel()
        if (!apiKeyVal.isNullOrBlank()) {
            try {
                return geminiProcessor.runAiAssistantPrompt(
                    apiKey = apiKeyVal,
                    modelName = modelVal,
                    instruction = instruction,
                    targetContent = targetText
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return "✨ AI Reader Analysis:\n$instruction\n\nTarget content:\n$targetText\n\nKey takeaways:\n• Content summarized cleanly for quick learning."
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
