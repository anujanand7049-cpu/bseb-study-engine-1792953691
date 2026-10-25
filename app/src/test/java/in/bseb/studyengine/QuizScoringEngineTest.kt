package `in`.bseb.studyengine

import `in`.bseb.studyengine.data.local.QuestionEntity
import `in`.bseb.studyengine.domain.logic.QuizScoringEngine
import `in`.bseb.studyengine.domain.model.AttemptOutcome
import `in`.bseb.studyengine.domain.model.QuestionFormat
import `in`.bseb.studyengine.domain.model.QuestionResponseState
import `in`.bseb.studyengine.domain.model.QuizConfig
import `in`.bseb.studyengine.domain.model.QuizMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizScoringEngineTest {

    private val sampleQuestions = listOf(
        QuestionEntity(
            questionId = "Q1",
            chapterId = "HIN_GOD_G01",
            subject = "HINDI",
            subSubject = "गद्य खण्ड",
            textbook = "गोधूलि भाग-2",
            topic = "श्रम विभाजन",
            questionType = "MCQ",
            difficulty = "EASY",
            questionText = "प्रश्न 1",
            optionsJson = "["विकल्प A","विकल्प B"]",
            correctAnswer = "विकल्प A",
            explanation = "व्याख्या 1",
            marks = 1,
            sourceType = "SYLLABUS_PRACTICE",
            sourceDetail = "गोधूलि भाग-2",
            verificationStatus = "VERIFIED"
        ),
        QuestionEntity(
            questionId = "Q2",
            chapterId = "SAN_PIY_01",
            subject = "SANSKRIT",
            subSubject = "पद्य",
            textbook = "पीयूषम् भाग-2",
            topic = "मङ्गलम्",
            questionType = "MCQ",
            difficulty = "MEDIUM",
            questionText = "प्रश्न 2",
            optionsJson = "["मुण्डकोपनिषद्","कठोपनिषद्"]",
            correctAnswer = "मुण्डकोपनिषद्",
            explanation = "व्याख्या 2",
            marks = 1,
            sourceType = "SYLLABUS_PRACTICE",
            sourceDetail = "पीयूषम् भाग-2",
            verificationStatus = "VERIFIED"
        ),
        QuestionEntity(
            questionId = "Q3",
            chapterId = "SST_HIS_01",
            subject = "SOCIAL_SCIENCE",
            subSubject = "इतिहास (History)",
            textbook = "इतिहास की दुनिया भाग-2",
            topic = "राष्ट्रवाद",
            questionType = "FILL_BLANK",
            difficulty = "EASY",
            questionText = "रूस में बोल्शेविक क्रांति ____ ई. में हुई।",
            optionsJson = "[]",
            correctAnswer = "1917",
            explanation = "व्याख्या 3",
            marks = 1,
            sourceType = "SYLLABUS_PRACTICE",
            sourceDetail = "इतिहास की दुनिया भाग-2",
            verificationStatus = "VERIFIED"
        )
    )

    @Test
    fun `evaluateQuiz calculates correct score, negative marking, and accuracy`() {
        val config = QuizConfig(
            titleHi = "जाँच टेस्ट",
            marksPerQuestion = 1.0,
            negativeMarkingPerWrong = 0.25,
            quizMode = QuizMode.EXAM_TIMED
        )

        val responses = mapOf(
            "Q1" to QuestionResponseState(questionId = "Q1", selectedOrTypedAnswer = "विकल्प A", timeSpentSeconds = 12),
            "Q2" to QuestionResponseState(questionId = "Q2", selectedOrTypedAnswer = "कठोपनिषद्", timeSpentSeconds = 18),
            "Q3" to QuestionResponseState(questionId = "Q3", selectedOrTypedAnswer = "", timeSpentSeconds = 5)
        )

        val summary = QuizScoringEngine.evaluateQuiz(
            questions = sampleQuestions,
            responses = responses,
            config = config,
            totalTimeTakenSeconds = 35,
            autoSubmittedOnTimeout = false
        )

        assertEquals(3, summary.totalQuestions)
        assertEquals(2, summary.attemptedCount)
        assertEquals(1, summary.correctCount)
        assertEquals(1, summary.incorrectCount)
        assertEquals(1, summary.skippedCount)
        assertEquals(1.0, summary.rawPositiveMarks, 0.001)
        assertEquals(0.25, summary.negativeMarksDeducted, 0.001)
        assertEquals(0.75, summary.finalScore, 0.001)
        assertEquals(50.0, summary.accuracyPercentage, 0.001)
        assertEquals(AttemptOutcome.SKIPPED, summary.itemEvaluations[2].status)
    }

    @Test
    fun `normalizeAnswer handles Devanagari purna viram and extra spaces`() {
        val correct = QuizScoringEngine.isAnswerCorrect(
            questionType = QuestionFormat.FILL_BLANK,
            userAnswer = "  देवनागरी। ",
            correctAnswer = "देवनागरी"
        )
        assertTrue(correct)
    }

    @Test
    fun `timer formatting and auto-submit rules work accurately`() {
        assertEquals("00:00", QuizScoringEngine.formatCountdownTimer(-5))
        assertEquals("01:05", QuizScoringEngine.formatCountdownTimer(65))
        assertEquals("01:01:01", QuizScoringEngine.formatCountdownTimer(3661))

        assertTrue(QuizScoringEngine.shouldAutoSubmit(0, isExamTimedMode = true))
        assertFalse(QuizScoringEngine.shouldAutoSubmit(1, isExamTimedMode = true))
        assertFalse(QuizScoringEngine.shouldAutoSubmit(0, isExamTimedMode = false))
    }

    @Test
    fun `selectQuestionsForQuiz filters by subject, bookmarks, and mistakes`() {
        val hindiConfig = QuizConfig(
            titleHi = "हिंदी टेस्ट",
            subjectFilter = "HINDI",
            questionCount = 10,
            shuffleQuestions = false
        )
        val selectedHindi = QuizScoringEngine.selectQuestionsForQuiz(
            allQuestions = sampleQuestions,
            bookmarkedIds = emptySet(),
            mistakeQuestionIds = emptySet(),
            config = hindiConfig
        )
        assertEquals(1, selectedHindi.size)
        assertEquals("Q1", selectedHindi.first().questionId)

        val mistakeConfig = QuizConfig(
            titleHi = "गलती टेस्ट",
            onlyMistakes = true,
            questionCount = 10,
            shuffleQuestions = false
        )
        val selectedMistakes = QuizScoringEngine.selectQuestionsForQuiz(
            allQuestions = sampleQuestions,
            bookmarkedIds = emptySet(),
            mistakeQuestionIds = setOf("Q2", "Q3"),
            config = mistakeConfig
        )
        assertEquals(2, selectedMistakes.size)
    }
}
