package `in`.bseb.studyengine.data.repository

import android.content.Context
import `in`.bseb.studyengine.data.backup.BackupRestoreSerializer
import `in`.bseb.studyengine.data.backup.FullBackupSnapshot
import `in`.bseb.studyengine.data.local.BookmarkEntity
import `in`.bseb.studyengine.data.local.ChapterEntity
import `in`.bseb.studyengine.data.local.MistakeRecordEntity
import `in`.bseb.studyengine.data.local.QuestionEntity
import `in`.bseb.studyengine.data.local.QuizAttemptEntity
import `in`.bseb.studyengine.data.local.QuizAttemptItemEntity
import `in`.bseb.studyengine.data.local.SavedTestPresetEntity
import `in`.bseb.studyengine.data.local.StudyDao
import `in`.bseb.studyengine.domain.model.AttemptOutcome
import `in`.bseb.studyengine.domain.model.QuizConfig
import `in`.bseb.studyengine.domain.model.QuizEvaluationSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class StudyRepository(
    private val context: Context,
    private val dao: StudyDao
) {
    val chaptersFlow: Flow<List<ChapterEntity>> = dao.observeAllChapters()
    val questionsFlow: Flow<List<QuestionEntity>> = dao.observeAllQuestions()
    val bookmarksFlow: Flow<List<BookmarkEntity>> = dao.observeBookmarks()
    val activeMistakesFlow: Flow<List<MistakeRecordEntity>> = dao.observeActiveMistakes()
    val allMistakesFlow: Flow<List<MistakeRecordEntity>> = dao.observeAllMistakes()
    val quizAttemptsFlow: Flow<List<QuizAttemptEntity>> = dao.observeQuizAttempts()
    val allAttemptItemsFlow: Flow<List<QuizAttemptItemEntity>> = dao.observeAllAttemptItems()
    val savedPresetsFlow: Flow<List<SavedTestPresetEntity>> = dao.observeSavedPresets()

    /**
     * Seeds the Room database on first launch from `assets/bseb_initial_questions.json`.
     */
    suspend fun ensureInitialSyllabusSeeded() = withContext(Dispatchers.IO) {
        val currentCount = dao.getQuestionCount()
        if (currentCount == 0) {
            val jsonText = context.assets.open("bseb_initial_questions.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val bundle = BackupRestoreSerializer.parseSeedOrImportJson(jsonText)
            dao.insertChapters(bundle.chapters)
            dao.insertQuestions(bundle.questions)
        }
    }

    suspend fun toggleBookmark(questionId: String, currentlyBookmarked: Boolean, noteHi: String = "") {
        withContext(Dispatchers.IO) {
            if (currentlyBookmarked) {
                dao.removeBookmark(questionId)
            } else {
                dao.insertBookmark(BookmarkEntity(questionId = questionId, noteHi = noteHi))
            }
        }
    }

    suspend fun reportQuestionContent(questionId: String, reasonHi: String) {
        withContext(Dispatchers.IO) {
            dao.updateQuestionReportStatus(
                questionId = questionId,
                reported = true,
                reason = reasonHi.trim().ifEmpty { "पुनर्निरीक्षण हेतु चिह्नित" }
            )
        }
    }

    suspend fun addCustomOrAiQuestions(questions: List<QuestionEntity>) = withContext(Dispatchers.IO) {
        dao.insertQuestions(questions)
        questions.map { it.chapterId }.distinct().forEach { chId ->
            dao.markChapterPopulated(chId)
        }
    }

    suspend fun importQuestionBankJson(jsonString: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val bundle = BackupRestoreSerializer.parseSeedOrImportJson(jsonString)
        if (bundle.chapters.isNotEmpty()) {
            dao.insertChapters(bundle.chapters)
        }
        if (bundle.questions.isNotEmpty()) {
            dao.insertQuestions(bundle.questions)
            bundle.questions.map { it.chapterId }.distinct().forEach { chId ->
                dao.markChapterPopulated(chId)
            }
        }
        Pair(bundle.chapters.size, bundle.questions.size)
    }

    /**
     * Persists a completed quiz attempt, its item-level breakdown, and automatically updates
     * the Mistake Notebook (recording wrong/skipped questions and marking previously mistaken
     * questions as resolved if answered correctly).
     */
    suspend fun recordCompletedQuiz(
        config: QuizConfig,
        questionsMap: Map<String, QuestionEntity>,
        evaluation: QuizEvaluationSummary
    ): Long = withContext(Dispatchers.IO) {
        val attemptEntity = QuizAttemptEntity(
            titleHi = config.titleHi,
            subjectFilter = config.subjectFilter ?: "ALL",
            chapterFilter = config.chapterIdFilter ?: "ALL",
            quizMode = config.quizMode.code,
            totalQuestions = evaluation.totalQuestions,
            attemptedCount = evaluation.attemptedCount,
            correctCount = evaluation.correctCount,
            incorrectCount = evaluation.incorrectCount,
            skippedCount = evaluation.skippedCount,
            maxMarks = evaluation.maxPossibleMarks,
            finalScore = evaluation.finalScore,
            negativeDeducted = evaluation.negativeMarksDeducted,
            accuracyPercentage = evaluation.accuracyPercentage,
            timeTakenSeconds = evaluation.totalTimeTakenSeconds,
            autoSubmitted = evaluation.autoSubmittedOnTimeout
        )

        val attemptId = dao.insertQuizAttempt(attemptEntity)

        val itemEntities = evaluation.itemEvaluations.map { item ->
            QuizAttemptItemEntity(
                attemptId = attemptId,
                questionId = item.questionId,
                userAnswer = item.userAnswer,
                correctAnswer = item.correctAnswer,
                outcome = item.status.name,
                marksAwarded = item.marksAwarded,
                timeSpentSeconds = item.timeSpentSeconds
            )
        }
        dao.insertQuizAttemptItems(itemEntities)

        // Update Mistake Notebook
        val now = System.currentTimeMillis()
        for (item in evaluation.itemEvaluations) {
            val qEntity = questionsMap[item.questionId] ?: continue
            val existingMistake = dao.getMistakeByQuestionId(item.questionId)

            when (item.status) {
                AttemptOutcome.INCORRECT, AttemptOutcome.SKIPPED -> {
                    val updatedCount = (existingMistake?.mistakeCount ?: 0) + 1
                    dao.upsertMistake(
                        MistakeRecordEntity(
                            questionId = item.questionId,
                            chapterId = qEntity.chapterId,
                            subject = qEntity.subject,
                            lastOutcome = item.status.name,
                            userLastAnswer = item.userAnswer,
                            mistakeCount = updatedCount,
                            resolved = false,
                            lastUpdatedEpochMillis = now
                        )
                    )
                }
                AttemptOutcome.CORRECT -> {
                    if (existingMistake != null && !existingMistake.resolved) {
                        dao.setMistakeResolved(questionId = item.questionId, resolved = true, timestamp = now)
                    }
                }
            }
        }

        attemptId
    }

    suspend fun setMistakeResolvedStatus(questionId: String, resolved: Boolean) = withContext(Dispatchers.IO) {
        dao.setMistakeResolved(questionId = questionId, resolved = resolved)
    }

    suspend fun saveTestPreset(preset: SavedTestPresetEntity) = withContext(Dispatchers.IO) {
        dao.insertSavedPreset(preset)
    }

    suspend fun deleteTestPreset(presetId: Long) = withContext(Dispatchers.IO) {
        dao.deleteSavedPreset(presetId)
    }

    suspend fun createBackupJsonString(): String = withContext(Dispatchers.IO) {
        val customQuestions = dao.getAllQuestionsOnce().filter { it.sourceType != "SYLLABUS_PRACTICE" }
        val bookmarks = dao.getAllBookmarksOnce()
        val mistakes = dao.getAllMistakesOnce()
        val attempts = dao.getAllQuizAttemptsOnce()
        val items = dao.getAllAttemptItemsOnce()

        val snapshot = FullBackupSnapshot(
            schemaVersion = 1,
            exportedAtMillis = System.currentTimeMillis(),
            customQuestions = customQuestions,
            bookmarks = bookmarks,
            mistakes = mistakes,
            quizAttempts = attempts,
            attemptItems = items
        )
        BackupRestoreSerializer.serializeBackup(snapshot)
    }

    suspend fun restoreFromBackupJsonString(jsonString: String): Int = withContext(Dispatchers.IO) {
        val snapshot = BackupRestoreSerializer.deserializeBackup(jsonString)
        if (snapshot.customQuestions.isNotEmpty()) {
            dao.insertQuestions(snapshot.customQuestions)
        }
        if (snapshot.bookmarks.isNotEmpty()) {
            dao.insertBookmarks(snapshot.bookmarks)
        }
        if (snapshot.mistakes.isNotEmpty()) {
            dao.insertMistakes(snapshot.mistakes)
        }
        if (snapshot.quizAttempts.isNotEmpty()) {
            dao.insertQuizAttempts(snapshot.quizAttempts)
        }
        if (snapshot.attemptItems.isNotEmpty()) {
            dao.insertQuizAttemptItems(snapshot.attemptItems)
        }
        snapshot.quizAttempts.size + snapshot.bookmarks.size + snapshot.mistakes.size + snapshot.customQuestions.size
    }

    suspend fun clearAllUserStudyProgress() = withContext(Dispatchers.IO) {
        dao.clearAllUserProgressKeepSyllabus()
    }
}
