package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    // --- Subjects ---
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubject(id: Long)

    // --- Chapters ---
    @Query("SELECT * FROM chapters ORDER BY title ASC")
    fun getAllChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY createdAt ASC")
    fun getChaptersForSubject(subjectId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): ChapterEntity?

    @Query("SELECT COUNT(*) FROM chapters WHERE subjectId = :subjectId")
    suspend fun getChapterCountForSubject(subjectId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Query("UPDATE chapters SET lastReadPage = :pageIndex, isCompleted = :isCompleted WHERE id = :chapterId")
    suspend fun updateChapterProgress(chapterId: Long, pageIndex: Int, isCompleted: Boolean)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapter(id: Long)

    // --- Pages ---
    @Query("SELECT * FROM pages WHERE chapterId = :chapterId ORDER BY pageIndex ASC")
    fun getPagesForChapter(chapterId: Long): Flow<List<PageEntity>>

    @Query("SELECT * FROM pages WHERE chapterId = :chapterId AND pageIndex = :pageIndex LIMIT 1")
    suspend fun getPageByIndex(chapterId: Long, pageIndex: Int): PageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Query("DELETE FROM pages WHERE chapterId = :chapterId")
    suspend fun deletePagesForChapter(chapterId: Long)

    // --- Questions ---
    @Query("SELECT * FROM questions WHERE chapterId = :chapterId")
    fun getQuestionsForChapter(chapterId: Long): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE chapterId = :chapterId")
    suspend fun getQuestionsListForChapter(chapterId: Long): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE chapterId = :chapterId")
    suspend fun deleteQuestionsForChapter(chapterId: Long)

    // --- Test Results ---
    @Query("SELECT * FROM test_results ORDER BY timestamp DESC")
    fun getAllTestResults(): Flow<List<TestResultEntity>>

    @Query("SELECT * FROM test_results WHERE id = :id")
    suspend fun getTestResultById(id: Long): TestResultEntity?

    @Query("SELECT * FROM test_results WHERE chapterId = :chapterId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTestResultForChapter(chapterId: Long): TestResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestResult(result: TestResultEntity): Long

    @Query("DELETE FROM test_results WHERE id = :id")
    suspend fun deleteTestResult(id: Long)

    // --- Chapter History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapterHistory(history: ChapterHistoryEntity): Long

    @Query("SELECT * FROM chapter_history WHERE chapterId = :chapterId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestChapterHistory(chapterId: Long): ChapterHistoryEntity?

    @Query("DELETE FROM chapter_history WHERE chapterId = :chapterId")
    suspend fun deleteChapterHistory(chapterId: Long)

    @Query("DELETE FROM subjects")
    suspend fun clearAllSubjects()

    @Query("DELETE FROM test_results")
    suspend fun clearAllTestResults()
}
