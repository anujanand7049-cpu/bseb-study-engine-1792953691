package `in`.bseb.studyengine.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    indices = [Index(value = ["subject"]), Index(value = ["subSubject"])]
)
data class ChapterEntity(
    @PrimaryKey val chapterId: String,
    val subject: String,
    val subSubject: String,
    val textbook: String,
    val chapterNumber: Int,
    val titleHi: String,
    val titleEn: String,
    val isPopulated: Boolean
)

@Entity(
    tableName = "questions",
    indices = [
        Index(value = ["chapterId"]),
        Index(value = ["subject"]),
        Index(value = ["subSubject"]),
        Index(value = ["questionType"])
    ]
)
data class QuestionEntity(
    @PrimaryKey val questionId: String,
    val chapterId: String,
    val subject: String,
    val subSubject: String,
    val textbook: String,
    val topic: String,
    val questionType: String,       // MCQ, TRUE_FALSE, FILL_BLANK, MATCHING, SHORT_ANSWER, LONG_ANSWER
    val difficulty: String,         // EASY, MEDIUM, HARD
    val questionText: String,
    val optionsJson: String,        // JSON array of strings for selectable options
    val correctAnswer: String,
    val explanation: String,
    val marks: Int,
    val sourceType: String,         // SYLLABUS_PRACTICE, USER_ADDED, AI_GENERATED
    val sourceDetail: String,
    val verificationStatus: String, // VERIFIED, USER_UNVERIFIED, AI_DRAFT
    val isReportedQuestionable: Boolean = false,
    val reportReason: String = ""
)

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["questionId"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey val questionId: String,
    val noteHi: String = "",
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "mistake_notebook",
    indices = [Index(value = ["questionId"], unique = true)]
)
data class MistakeRecordEntity(
    @PrimaryKey val questionId: String,
    val chapterId: String,
    val subject: String,
    val lastOutcome: String,        // INCORRECT or SKIPPED
    val userLastAnswer: String,
    val mistakeCount: Int,
    val resolved: Boolean = false,
    val lastUpdatedEpochMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_attempts")
data class QuizAttemptEntity(
    @PrimaryKey(autoGenerate = true) val attemptId: Long = 0,
    val titleHi: String,
    val subjectFilter: String,
    val chapterFilter: String,
    val quizMode: String,
    val totalQuestions: Int,
    val attemptedCount: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val maxMarks: Double,
    val finalScore: Double,
    val negativeDeducted: Double,
    val accuracyPercentage: Double,
    val timeTakenSeconds: Int,
    val autoSubmitted: Boolean,
    val timestampEpochMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "quiz_attempt_items",
    indices = [Index(value = ["attemptId"]), Index(value = ["questionId"])]
)
data class QuizAttemptItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attemptId: Long,
    val questionId: String,
    val userAnswer: String,
    val correctAnswer: String,
    val outcome: String,            // CORRECT, INCORRECT, SKIPPED
    val marksAwarded: Double,
    val timeSpentSeconds: Int
)

@Entity(tableName = "saved_test_presets")
data class SavedTestPresetEntity(
    @PrimaryKey(autoGenerate = true) val presetId: Long = 0,
    val titleHi: String,
    val subjectFilter: String?,
    val subSubjectFilter: String?,
    val chapterIdFilter: String?,
    val questionCount: Int,
    val durationMinutes: Int,
    val negativeMarking: Double,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
