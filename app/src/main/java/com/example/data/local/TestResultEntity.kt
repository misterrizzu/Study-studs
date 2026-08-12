package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_results")
data class TestResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chapterId: Long,
    val subjectId: Long,
    val subjectName: String,
    val chapterTitle: String,
    val score: Int,
    val totalQuestions: Int,
    val timeTakenSeconds: Long,
    val userAnswersJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
