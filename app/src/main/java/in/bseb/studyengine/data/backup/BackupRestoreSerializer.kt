package `in`.bseb.studyengine.data.backup

import `in`.bseb.studyengine.data.local.BookmarkEntity
import `in`.bseb.studyengine.data.local.ChapterEntity
import `in`.bseb.studyengine.data.local.MistakeRecordEntity
import `in`.bseb.studyengine.data.local.QuestionEntity
import `in`.bseb.studyengine.data.local.QuizAttemptEntity
import `in`.bseb.studyengine.data.local.QuizAttemptItemEntity
import org.json.JSONArray
import org.json.JSONObject

data class SeedBundle(
    val chapters: List<ChapterEntity>,
    val questions: List<QuestionEntity>
)

data class FullBackupSnapshot(
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val customQuestions: List<QuestionEntity>,
    val bookmarks: List<BookmarkEntity>,
    val mistakes: List<MistakeRecordEntity>,
    val quizAttempts: List<QuizAttemptEntity>,
    val attemptItems: List<QuizAttemptItemEntity>
)

object BackupRestoreSerializer {

    fun parseOptionsJson(optionsJson: String): List<String> {
        if (optionsJson.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(optionsJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.optString(i, ""))
            }
            list.filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun encodeOptionsJson(options: List<String>): String {
        val arr = JSONArray()
        options.forEach { arr.put(it) }
        return arr.toString()
    }

    /**
     * Parses `assets/bseb_initial_questions.json` or a user-imported question bank JSON file.
     */
    fun parseSeedOrImportJson(jsonString: String): SeedBundle {
        val root = JSONObject(jsonString)
        val chaptersArr = root.optJSONArray("chapters") ?: JSONArray()
        val questionsArr = root.optJSONArray("questions") ?: JSONArray()

        val chapters = mutableListOf<ChapterEntity>()
        for (i in 0 until chaptersArr.length()) {
            val obj = chaptersArr.getJSONObject(i)
            chapters.add(
                ChapterEntity(
                    chapterId = obj.getString("chapterId"),
                    subject = obj.getString("subject"),
                    subSubject = obj.optString("subSubject", ""),
                    textbook = obj.optString("textbook", ""),
                    chapterNumber = obj.optInt("chapterNumber", i + 1),
                    titleHi = obj.getString("titleHi"),
                    titleEn = obj.optString("titleEn", ""),
                    isPopulated = obj.optBoolean("isPopulated", false)
                )
            )
        }

        val questions = mutableListOf<QuestionEntity>()
        for (i in 0 until questionsArr.length()) {
            val obj = questionsArr.getJSONObject(i)
            val optsArr = obj.optJSONArray("options") ?: JSONArray()
            val optsList = mutableListOf<String>()
            for (j in 0 until optsArr.length()) {
                optsList.add(optsArr.optString(j))
            }
            questions.add(
                QuestionEntity(
                    questionId = obj.getString("questionId"),
                    chapterId = obj.getString("chapterId"),
                    subject = obj.getString("subject"),
                    subSubject = obj.optString("subSubject", ""),
                    textbook = obj.optString("textbook", ""),
                    topic = obj.optString("topic", ""),
                    questionType = obj.optString("questionType", "MCQ"),
                    difficulty = obj.optString("difficulty", "MEDIUM"),
                    questionText = obj.getString("questionText"),
                    optionsJson = encodeOptionsJson(optsList),
                    correctAnswer = obj.getString("correctAnswer"),
                    explanation = obj.optString("explanation", ""),
                    marks = obj.optInt("marks", 1),
                    sourceType = obj.optString("sourceType", "USER_IMPORTED"),
                    sourceDetail = obj.optString("sourceDetail", "उपयोगकर्ता द्वारा आयातित"),
                    verificationStatus = obj.optString("verificationStatus", "USER_VERIFIED"),
                    isReportedQuestionable = obj.optBoolean("isReportedQuestionable", false),
                    reportReason = obj.optString("reportReason", "")
                )
            )
        }

        return SeedBundle(chapters = chapters, questions = questions)
    }

    /**
     * Serializes all user study progress, bookmarks, mistakes, attempts, and custom questions to JSON.
     */
    fun serializeBackup(snapshot: FullBackupSnapshot): String {
        val root = JSONObject()
        root.put("backupType", "BSEB_AI_STUDY_ENGINE_BACKUP")
        root.put("schemaVersion", snapshot.schemaVersion)
        root.put("exportedAtMillis", snapshot.exportedAtMillis)

        val customQArr = JSONArray()
        for (q in snapshot.customQuestions) {
            val obj = JSONObject()
            obj.put("questionId", q.questionId)
            obj.put("chapterId", q.chapterId)
            obj.put("subject", q.subject)
            obj.put("subSubject", q.subSubject)
            obj.put("textbook", q.textbook)
            obj.put("topic", q.topic)
            obj.put("questionType", q.questionType)
            obj.put("difficulty", q.difficulty)
            obj.put("questionText", q.questionText)
            obj.put("optionsJson", q.optionsJson)
            obj.put("correctAnswer", q.correctAnswer)
            obj.put("explanation", q.explanation)
            obj.put("marks", q.marks)
            obj.put("sourceType", q.sourceType)
            obj.put("sourceDetail", q.sourceDetail)
            obj.put("verificationStatus", q.verificationStatus)
            obj.put("isReportedQuestionable", q.isReportedQuestionable)
            obj.put("reportReason", q.reportReason)
            customQArr.put(obj)
        }
        root.put("customQuestions", customQArr)

        val bookmarksArr = JSONArray()
        for (b in snapshot.bookmarks) {
            val obj = JSONObject()
            obj.put("questionId", b.questionId)
            obj.put("noteHi", b.noteHi)
            obj.put("createdAtEpochMillis", b.createdAtEpochMillis)
            bookmarksArr.put(obj)
        }
        root.put("bookmarks", bookmarksArr)

        val mistakesArr = JSONArray()
        for (m in snapshot.mistakes) {
            val obj = JSONObject()
            obj.put("questionId", m.questionId)
            obj.put("chapterId", m.chapterId)
            obj.put("subject", m.subject)
            obj.put("lastOutcome", m.lastOutcome)
            obj.put("userLastAnswer", m.userLastAnswer)
            obj.put("mistakeCount", m.mistakeCount)
            obj.put("resolved", m.resolved)
            obj.put("lastUpdatedEpochMillis", m.lastUpdatedEpochMillis)
            mistakesArr.put(obj)
        }
        root.put("mistakes", mistakesArr)

        val attemptsArr = JSONArray()
        for (a in snapshot.quizAttempts) {
            val obj = JSONObject()
            obj.put("attemptId", a.attemptId)
            obj.put("titleHi", a.titleHi)
            obj.put("subjectFilter", a.subjectFilter)
            obj.put("chapterFilter", a.chapterFilter)
            obj.put("quizMode", a.quizMode)
            obj.put("totalQuestions", a.totalQuestions)
            obj.put("attemptedCount", a.attemptedCount)
            obj.put("correctCount", a.correctCount)
            obj.put("incorrectCount", a.incorrectCount)
            obj.put("skippedCount", a.skippedCount)
            obj.put("maxMarks", a.maxMarks)
            obj.put("finalScore", a.finalScore)
            obj.put("negativeDeducted", a.negativeDeducted)
            obj.put("accuracyPercentage", a.accuracyPercentage)
            obj.put("timeTakenSeconds", a.timeTakenSeconds)
            obj.put("autoSubmitted", a.autoSubmitted)
            obj.put("timestampEpochMillis", a.timestampEpochMillis)
            attemptsArr.put(obj)
        }
        root.put("quizAttempts", attemptsArr)

        val itemsArr = JSONArray()
        for (item in snapshot.attemptItems) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("attemptId", item.attemptId)
            obj.put("questionId", item.questionId)
            obj.put("userAnswer", item.userAnswer)
            obj.put("correctAnswer", item.correctAnswer)
            obj.put("outcome", item.outcome)
            obj.put("marksAwarded", item.marksAwarded)
            obj.put("timeSpentSeconds", item.timeSpentSeconds)
            itemsArr.put(obj)
        }
        root.put("attemptItems", itemsArr)

        return root.toString(2)
    }

    /**
     * Deserializes a JSON backup string back into a FullBackupSnapshot.
     */
    fun deserializeBackup(jsonString: String): FullBackupSnapshot {
        val root = JSONObject(jsonString)
        val backupType = root.optString("backupType", "")
        require(backupType == "BSEB_AI_STUDY_ENGINE_BACKUP") {
            "अमान्य बैकअप फ़ाइल (Invalid Backup File Format)"
        }

        val schemaVersion = root.optInt("schemaVersion", 1)
        val exportedAt = root.optLong("exportedAtMillis", System.currentTimeMillis())

        val customQuestions = mutableListOf<QuestionEntity>()
        val customQArr = root.optJSONArray("customQuestions") ?: JSONArray()
        for (i in 0 until customQArr.length()) {
            val obj = customQArr.getJSONObject(i)
            customQuestions.add(
                QuestionEntity(
                    questionId = obj.getString("questionId"),
                    chapterId = obj.getString("chapterId"),
                    subject = obj.getString("subject"),
                    subSubject = obj.optString("subSubject", ""),
                    textbook = obj.optString("textbook", ""),
                    topic = obj.optString("topic", ""),
                    questionType = obj.optString("questionType", "MCQ"),
                    difficulty = obj.optString("difficulty", "MEDIUM"),
                    questionText = obj.getString("questionText"),
                    optionsJson = obj.optString("optionsJson", "[]"),
                    correctAnswer = obj.getString("correctAnswer"),
                    explanation = obj.optString("explanation", ""),
                    marks = obj.optInt("marks", 1),
                    sourceType = obj.optString("sourceType", "USER_ADDED"),
                    sourceDetail = obj.optString("sourceDetail", ""),
                    verificationStatus = obj.optString("verificationStatus", "USER_UNVERIFIED"),
                    isReportedQuestionable = obj.optBoolean("isReportedQuestionable", false),
                    reportReason = obj.optString("reportReason", "")
                )
            )
        }

        val bookmarks = mutableListOf<BookmarkEntity>()
        val bookmarksArr = root.optJSONArray("bookmarks") ?: JSONArray()
        for (i in 0 until bookmarksArr.length()) {
            val obj = bookmarksArr.getJSONObject(i)
            bookmarks.add(
                BookmarkEntity(
                    questionId = obj.getString("questionId"),
                    noteHi = obj.optString("noteHi", ""),
                    createdAtEpochMillis = obj.optLong("createdAtEpochMillis", System.currentTimeMillis())
                )
            )
        }

        val mistakes = mutableListOf<MistakeRecordEntity>()
        val mistakesArr = root.optJSONArray("mistakes") ?: JSONArray()
        for (i in 0 until mistakesArr.length()) {
            val obj = mistakesArr.getJSONObject(i)
            mistakes.add(
                MistakeRecordEntity(
                    questionId = obj.getString("questionId"),
                    chapterId = obj.getString("chapterId"),
                    subject = obj.getString("subject"),
                    lastOutcome = obj.optString("lastOutcome", "INCORRECT"),
                    userLastAnswer = obj.optString("userLastAnswer", ""),
                    mistakeCount = obj.optInt("mistakeCount", 1),
                    resolved = obj.optBoolean("resolved", false),
                    lastUpdatedEpochMillis = obj.optLong("lastUpdatedEpochMillis", System.currentTimeMillis())
                )
            )
        }

        val attempts = mutableListOf<QuizAttemptEntity>()
        val attemptsArr = root.optJSONArray("quizAttempts") ?: JSONArray()
        for (i in 0 until attemptsArr.length()) {
            val obj = attemptsArr.getJSONObject(i)
            attempts.add(
                QuizAttemptEntity(
                    attemptId = obj.getLong("attemptId"),
                    titleHi = obj.getString("titleHi"),
                    subjectFilter = obj.optString("subjectFilter", "ALL"),
                    chapterFilter = obj.optString("chapterFilter", "ALL"),
                    quizMode = obj.optString("quizMode", "EXAM_TIMED"),
                    totalQuestions = obj.getInt("totalQuestions"),
                    attemptedCount = obj.getInt("attemptedCount"),
                    correctCount = obj.getInt("correctCount"),
                    incorrectCount = obj.getInt("incorrectCount"),
                    skippedCount = obj.getInt("skippedCount"),
                    maxMarks = obj.getDouble("maxMarks"),
                    finalScore = obj.getDouble("finalScore"),
                    negativeDeducted = obj.optDouble("negativeDeducted", 0.0),
                    accuracyPercentage = obj.getDouble("accuracyPercentage"),
                    timeTakenSeconds = obj.getInt("timeTakenSeconds"),
                    autoSubmitted = obj.optBoolean("autoSubmitted", false),
                    timestampEpochMillis = obj.getLong("timestampEpochMillis")
                )
            )
        }

        val items = mutableListOf<QuizAttemptItemEntity>()
        val itemsArr = root.optJSONArray("attemptItems") ?: JSONArray()
        for (i in 0 until itemsArr.length()) {
            val obj = itemsArr.getJSONObject(i)
            items.add(
                QuizAttemptItemEntity(
                    id = obj.getLong("id"),
                    attemptId = obj.getLong("attemptId"),
                    questionId = obj.getString("questionId"),
                    userAnswer = obj.optString("userAnswer", ""),
                    correctAnswer = obj.optString("correctAnswer", ""),
                    outcome = obj.optString("outcome", "SKIPPED"),
                    marksAwarded = obj.optDouble("marksAwarded", 0.0),
                    timeSpentSeconds = obj.optInt("timeSpentSeconds", 0)
                )
            )
        }

        return FullBackupSnapshot(
            schemaVersion = schemaVersion,
            exportedAtMillis = exportedAt,
            customQuestions = customQuestions,
            bookmarks = bookmarks,
            mistakes = mistakes,
            quizAttempts = attempts,
            attemptItems = items
        )
    }
}
