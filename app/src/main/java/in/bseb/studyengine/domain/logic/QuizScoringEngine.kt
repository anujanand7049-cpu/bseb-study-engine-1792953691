package `in`.bseb.studyengine.domain.logic

import `in`.bseb.studyengine.data.local.QuestionEntity
import `in`.bseb.studyengine.domain.model.AttemptOutcome
import `in`.bseb.studyengine.domain.model.EvaluatedQuestionItem
import `in`.bseb.studyengine.domain.model.QuestionFormat
import `in`.bseb.studyengine.domain.model.QuestionResponseState
import `in`.bseb.studyengine.domain.model.QuizConfig
import `in`.bseb.studyengine.domain.model.QuizEvaluationSummary
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Pure Kotlin domain engine for quiz evaluation, negative marking, answer normalization,
 * countdown timer formatting, and filtered question selection.
 */
object QuizScoringEngine {

    /**
     * Normalizes Hindi/Sanskrit/English strings for fair comparison (trims whitespace,
     * normalizes punctuation and case).
     */
    fun normalizeAnswer(raw: String): String {
        return raw.trim()
            .replace(Regex("\\s+"), " ")
            .replace("।", "")
            .replace(".", "")
            .lowercase()
    }

    /**
     * Checks whether a student's answer matches the official correct answer.
     * Supports exact option match for MCQs/Matching/TrueFalse and normalized text match for Fill-in-the-blank.
     */
    fun isAnswerCorrect(
        questionType: QuestionFormat,
        userAnswer: String,
        correctAnswer: String,
        selfEvaluatedSubjective: Boolean? = null
    ): Boolean {
        if (userAnswer.isBlank()) return false
        if (!questionType.isObjective) {
            // For short/long answer practice, use student's self-evaluation against rubric or keyword check
            return selfEvaluatedSubjective ?: (normalizeAnswer(userAnswer).length >= 15)
        }
        return normalizeAnswer(userAnswer) == normalizeAnswer(correctAnswer)
    }

    /**
     * Evaluates an entire quiz session and returns detailed per-question and aggregate metrics.
     */
    fun evaluateQuiz(
        questions: List<QuestionEntity>,
        responses: Map<String, QuestionResponseState>,
        config: QuizConfig,
        totalTimeTakenSeconds: Int,
        autoSubmittedOnTimeout: Boolean
    ): QuizEvaluationSummary {
        var attempted = 0
        var correct = 0
        var incorrect = 0
        var skipped = 0
        var maxMarks = 0.0
        var positiveMarks = 0.0
        var negativeMarks = 0.0

        val items = mutableListOf<EvaluatedQuestionItem>()

        for (q in questions) {
            val qFormat = QuestionFormat.fromCode(q.questionType)
            val questionMaxMarks = if (config.marksPerQuestion > 0.0) {
                config.marksPerQuestion
            } else {
                q.marks.toDouble()
            }
            maxMarks += questionMaxMarks

            val response = responses[q.questionId]
            val userAns = response?.selectedOrTypedAnswer?.trim().orEmpty()
            val timeSpent = response?.timeSpentSeconds ?: 0

            if (userAns.isEmpty()) {
                skipped++
                items.add(
                    EvaluatedQuestionItem(
                        questionId = q.questionId,
                        questionText = q.questionText,
                        questionType = qFormat,
                        userAnswer = "",
                        correctAnswer = q.correctAnswer,
                        explanation = q.explanation,
                        status = AttemptOutcome.SKIPPED,
                        marksAwarded = 0.0,
                        maxMarks = questionMaxMarks,
                        timeSpentSeconds = timeSpent
                    )
                )
            } else {
                attempted++
                val isRight = isAnswerCorrect(
                    questionType = qFormat,
                    userAnswer = userAns,
                    correctAnswer = q.correctAnswer,
                    selfEvaluatedSubjective = response?.selfEvaluatedSubjectiveCorrect
                )
                if (isRight) {
                    correct++
                    positiveMarks += questionMaxMarks
                    items.add(
                        EvaluatedQuestionItem(
                            questionId = q.questionId,
                            questionText = q.questionText,
                            questionType = qFormat,
                            userAnswer = userAns,
                            correctAnswer = q.correctAnswer,
                            explanation = q.explanation,
                            status = AttemptOutcome.CORRECT,
                            marksAwarded = questionMaxMarks,
                            maxMarks = questionMaxMarks,
                            timeSpentSeconds = timeSpent
                        )
                    )
                } else {
                    incorrect++
                    val penalty = if (qFormat.isObjective && config.negativeMarkingPerWrong > 0.0) {
                        config.negativeMarkingPerWrong
                    } else {
                        0.0
                    }
                    negativeMarks += penalty
                    items.add(
                        EvaluatedQuestionItem(
                            questionId = q.questionId,
                            questionText = q.questionText,
                            questionType = qFormat,
                            userAnswer = userAns,
                            correctAnswer = q.correctAnswer,
                            explanation = q.explanation,
                            status = AttemptOutcome.INCORRECT,
                            marksAwarded = -penalty,
                            maxMarks = questionMaxMarks,
                            timeSpentSeconds = timeSpent
                        )
                    )
                }
            }
        }

        val finalScore = max(0.0, roundTwoDecimals(positiveMarks - negativeMarks))
        val accuracyPct = if (attempted > 0) {
            roundTwoDecimals((correct.toDouble() / attempted.toDouble()) * 100.0)
        } else {
            0.0
        }
        val scorePct = if (maxMarks > 0.0) {
            roundTwoDecimals((finalScore / maxMarks) * 100.0)
        } else {
            0.0
        }

        return QuizEvaluationSummary(
            totalQuestions = questions.size,
            attemptedCount = attempted,
            correctCount = correct,
            incorrectCount = incorrect,
            skippedCount = skipped,
            maxPossibleMarks = roundTwoDecimals(maxMarks),
            rawPositiveMarks = roundTwoDecimals(positiveMarks),
            negativeMarksDeducted = roundTwoDecimals(negativeMarks),
            finalScore = finalScore,
            accuracyPercentage = accuracyPct,
            scorePercentage = scorePct,
            totalTimeTakenSeconds = max(0, totalTimeTakenSeconds),
            autoSubmittedOnTimeout = autoSubmittedOnTimeout,
            itemEvaluations = items
        )
    }

    /**
     * Filters and selects questions according to QuizConfig criteria.
     */
    fun selectQuestionsForQuiz(
        allQuestions: List<QuestionEntity>,
        bookmarkedIds: Set<String>,
        mistakeQuestionIds: Set<String>,
        config: QuizConfig,
        randomSeed: Long = System.currentTimeMillis()
    ): List<QuestionEntity> {
        val filtered = allQuestions.filter { q ->
            val matchesSubject = config.subjectFilter.isNullOrBlank() ||
                q.subject.equals(config.subjectFilter, ignoreCase = true)
            val matchesSubSubject = config.subSubjectFilter.isNullOrBlank() ||
                q.subSubject.equals(config.subSubjectFilter, ignoreCase = true)
            val matchesChapter = config.chapterIdFilter.isNullOrBlank() ||
                q.chapterId.equals(config.chapterIdFilter, ignoreCase = true)

            val qFormat = QuestionFormat.fromCode(q.questionType)
            val matchesType = when {
                config.includeObjectiveOnly -> qFormat.isObjective
                config.includeSubjectiveOnly -> !qFormat.isObjective
                else -> true
            }

            val matchesBookmark = !config.onlyBookmarked || bookmarkedIds.contains(q.questionId)
            val matchesMistake = !config.onlyMistakes || mistakeQuestionIds.contains(q.questionId)

            matchesSubject && matchesSubSubject && matchesChapter &&
                matchesType && matchesBookmark && matchesMistake
        }

        val ordered = if (config.shuffleQuestions) {
            filtered.shuffled(Random(randomSeed))
        } else {
            filtered
        }

        return ordered.take(max(1, config.questionCount))
    }

    /**
     * Formats remaining seconds as MM:SS or HH:MM:SS for the countdown timer.
     */
    fun formatCountdownTimer(remainingSeconds: Int): String {
        val safeSeconds = max(0, remainingSeconds)
        val hours = safeSeconds / 3600
        val minutes = (safeSeconds % 3600) / 60
        val seconds = safeSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Determines whether the timer has expired and automatic submission must trigger.
     */
    fun shouldAutoSubmit(remainingSeconds: Int, isExamTimedMode: Boolean): Boolean {
        return isExamTimedMode && remainingSeconds <= 0
    }

    private fun roundTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
