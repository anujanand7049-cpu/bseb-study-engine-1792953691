package `in`.bseb.studyengine.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    // ---------------- CHAPTERS ----------------
    @Query("SELECT * FROM chapters ORDER BY subject ASC, textbook ASC, chapterNumber ASC")
    fun observeAllChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters ORDER BY subject ASC, textbook ASC, chapterNumber ASC")
    suspend fun getAllChaptersOnce(): List<ChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Query("UPDATE chapters SET isPopulated = 1 WHERE chapterId = :chapterId")
    suspend fun markChapterPopulated(chapterId: String)

    // ---------------- QUESTIONS ----------------
    @Query("SELECT * FROM questions ORDER BY questionId ASC")
    fun observeAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions ORDER BY questionId ASC")
    suspend fun getAllQuestionsOnce(): List<QuestionEntity>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleQuestion(question: QuestionEntity)

    @Query("UPDATE questions SET isReportedQuestionable = :reported, reportReason = :reason WHERE questionId = :questionId")
    suspend fun updateQuestionReportStatus(questionId: String, reported: Boolean, reason: String)

    // ---------------- BOOKMARKS ----------------
    @Query("SELECT * FROM bookmarks ORDER BY createdAtEpochMillis DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks")
    suspend fun getAllBookmarksOnce(): List<BookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Query("DELETE FROM bookmarks WHERE questionId = :questionId")
    suspend fun removeBookmark(questionId: String)

    // ---------------- MISTAKE NOTEBOOK ----------------
    @Query("SELECT * FROM mistake_notebook WHERE resolved = 0 ORDER BY mistakeCount DESC, lastUpdatedEpochMillis DESC")
    fun observeActiveMistakes(): Flow<List<MistakeRecordEntity>>

    @Query("SELECT * FROM mistake_notebook ORDER BY lastUpdatedEpochMillis DESC")
    fun observeAllMistakes(): Flow<List<MistakeRecordEntity>>

    @Query("SELECT * FROM mistake_notebook")
    suspend fun getAllMistakesOnce(): List<MistakeRecordEntity>

    @Query("SELECT * FROM mistake_notebook WHERE questionId = :questionId LIMIT 1")
    suspend fun getMistakeByQuestionId(questionId: String): MistakeRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMistake(record: MistakeRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistakes(records: List<MistakeRecordEntity>)

    @Query("UPDATE mistake_notebook SET resolved = :resolved, lastUpdatedEpochMillis = :timestamp WHERE questionId = :questionId")
    suspend fun setMistakeResolved(questionId: String, resolved: Boolean, timestamp: Long = System.currentTimeMillis())

    // ---------------- QUIZ ATTEMPTS & HISTORY ----------------
    @Query("SELECT * FROM quiz_attempts ORDER BY timestampEpochMillis DESC")
    fun observeQuizAttempts(): Flow<List<QuizAttemptEntity>>

    @Query("SELECT * FROM quiz_attempts ORDER BY timestampEpochMillis DESC")
    suspend fun getAllQuizAttemptsOnce(): List<QuizAttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizAttempt(attempt: QuizAttemptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizAttempts(attempts: List<QuizAttemptEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizAttemptItems(items: List<QuizAttemptItemEntity>)

    @Query("SELECT * FROM quiz_attempt_items WHERE attemptId = :attemptId")
    suspend fun getItemsForAttempt(attemptId: Long): List<QuizAttemptItemEntity>

    @Query("SELECT * FROM quiz_attempt_items")
    fun observeAllAttemptItems(): Flow<List<QuizAttemptItemEntity>>

    @Query("SELECT * FROM quiz_attempt_items")
    suspend fun getAllAttemptItemsOnce(): List<QuizAttemptItemEntity>

    // ---------------- SAVED TEST PRESETS ----------------
    @Query("SELECT * FROM saved_test_presets ORDER BY createdAtEpochMillis DESC")
    fun observeSavedPresets(): Flow<List<SavedTestPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPreset(preset: SavedTestPresetEntity)

    @Query("DELETE FROM saved_test_presets WHERE presetId = :presetId")
    suspend fun deleteSavedPreset(presetId: Long)

    // ---------------- CLEAR USER DATA ----------------
    @Transaction
    suspend fun clearAllUserProgressKeepSyllabus() {
        deleteAllAttemptItems()
        deleteAllAttempts()
        deleteAllMistakes()
        deleteAllBookmarks()
        deleteUserAndAiQuestions()
    }

    @Query("DELETE FROM quiz_attempt_items")
    suspend fun deleteAllAttemptItems()

    @Query("DELETE FROM quiz_attempts")
    suspend fun deleteAllAttempts()

    @Query("DELETE FROM mistake_notebook")
    suspend fun deleteAllMistakes()

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()

    @Query("DELETE FROM questions WHERE sourceType != 'SYLLABUS_PRACTICE'")
    suspend fun deleteUserAndAiQuestions()
}
