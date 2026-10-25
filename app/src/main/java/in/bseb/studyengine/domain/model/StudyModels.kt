package `in`.bseb.studyengine.domain.model

enum class SubjectType(val code: String, val titleHi: String, val titleEn: String, val descriptionHi: String) {
    HINDI(
        code = "HINDI",
        titleHi = "हिंदी (मातृभाषा)",
        titleEn = "Hindi (MT)",
        descriptionHi = "गोधूलि भाग-2 (गद्य एवं काव्य खण्ड), वर्णिका भाग-2 एवं हिंदी व्याकरण"
    ),
    SANSKRIT(
        code = "SANSKRIT",
        titleHi = "संस्कृत (द्वितीय भारतीय भाषा)",
        titleEn = "Sanskrit (SIL)",
        descriptionHi = "पीयूषम् भाग-2 (14 पाठ) एवं संस्कृत व्याकरण व अनुवाद"
    ),
    SOCIAL_SCIENCE(
        code = "SOCIAL_SCIENCE",
        titleHi = "सामाजिक विज्ञान",
        titleEn = "Social Science",
        descriptionHi = "इतिहास, भूगोल, राजनीति विज्ञान, अर्थशास्त्र एवं आपदा प्रबंधन"
    );

    companion object {
        fun fromCode(code: String): SubjectType =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HINDI
    }
}

enum class QuestionFormat(val code: String, val labelHi: String, val isObjective: Boolean) {
    MCQ("MCQ", "बहुविकल्पीय (MCQ)", true),
    TRUE_FALSE("TRUE_FALSE", "सत्य / असत्य", true),
    FILL_BLANK("FILL_BLANK", "रिक्त स्थान की पूर्ति", true),
    MATCHING("MATCHING", "सही मिलान (Matching)", true),
    SHORT_ANSWER("SHORT_ANSWER", "लघु उत्तरीय (2 अंक)", false),
    LONG_ANSWER("LONG_ANSWER", "दीर्घ उत्तरीय (4-7 अंक)", false);

    companion object {
        fun fromCode(code: String): QuestionFormat =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: MCQ
    }
}

enum class DifficultyLevel(val code: String, val labelHi: String) {
    EASY("EASY", "सरल (Easy)"),
    MEDIUM("MEDIUM", "मध्यम (Medium)"),
    HARD("HARD", "कठिन (Hard)");

    companion object {
        fun fromCode(code: String): DifficultyLevel =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: MEDIUM
    }
}

enum class QuizMode(val code: String, val labelHi: String, val descriptionHi: String) {
    PRACTICE_INSTANT(
        code = "PRACTICE_INSTANT",
        labelHi = "अभ्यास मोड (तुरंत उत्तर व व्याख्या)",
        descriptionHi = "प्रत्येक प्रश्न के बाद सही उत्तर और विस्तृत हिंदी व्याख्या तुरंत देखें।"
    ),
    EXAM_TIMED(
        code = "EXAM_TIMED",
        labelHi = "परीक्षा मोड (टाइमर के साथ टेस्ट)",
        descriptionHi = "बोर्ड परीक्षा जैसा अनुभव। टेस्ट समाप्त होने से पहले उत्तर नहीं दिखेंगे और समय समाप्त होने पर ऑटो-सबमिट होगा।"
    )
}

data class QuizConfig(
    val titleHi: String,
    val subjectFilter: String? = null,      // null = Mixed / All subjects
    val subSubjectFilter: String? = null,   // e.g., इतिहास, भूगोल, आपदा प्रबंधन
    val chapterIdFilter: String? = null,
    val includeObjectiveOnly: Boolean = false,
    val includeSubjectiveOnly: Boolean = false,
    val onlyBookmarked: Boolean = false,
    val onlyMistakes: Boolean = false,
    val questionCount: Int = 10,
    val durationMinutes: Int = 15,
    val marksPerQuestion: Double = 1.0,
    val negativeMarkingPerWrong: Double = 0.0,
    val shuffleQuestions: Boolean = true,
    val shuffleOptions: Boolean = true,
    val quizMode: QuizMode = QuizMode.EXAM_TIMED
)

data class QuestionResponseState(
    val questionId: String,
    val selectedOrTypedAnswer: String = "",
    val isMarkedForReview: Boolean = false,
    val timeSpentSeconds: Int = 0,
    val selfEvaluatedSubjectiveCorrect: Boolean? = null
)

data class EvaluatedQuestionItem(
    val questionId: String,
    val questionText: String,
    val questionType: QuestionFormat,
    val userAnswer: String,
    val correctAnswer: String,
    val explanation: String,
    val status: AttemptOutcome,
    val marksAwarded: Double,
    val maxMarks: Double,
    val timeSpentSeconds: Int
)

enum class AttemptOutcome(val labelHi: String) {
    CORRECT("सही (Correct)"),
    INCORRECT("गलत (Wrong)"),
    SKIPPED("छोड़ा गया (Skipped)")
}

data class QuizEvaluationSummary(
    val totalQuestions: Int,
    val attemptedCount: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val skippedCount: Int,
    val maxPossibleMarks: Double,
    val rawPositiveMarks: Double,
    val negativeMarksDeducted: Double,
    val finalScore: Double,
    val accuracyPercentage: Double,
    val scorePercentage: Double,
    val totalTimeTakenSeconds: Int,
    val autoSubmittedOnTimeout: Boolean,
    val itemEvaluations: List<EvaluatedQuestionItem>
)

data class SubjectPerformanceStat(
    val subjectCode: String,
    val subjectTitleHi: String,
    val totalChapters: Int,
    val populatedChapters: Int,
    val totalQuestionsInBank: Int,
    val totalAttemptedQuestions: Int,
    val totalCorrectAnswers: Int,
    val accuracyPercentage: Double
)

data class WeakTopicSuggestion(
    val chapterId: String,
    val chapterTitleHi: String,
    val subjectTitleHi: String,
    val mistakeCount: Int,
    val accuracyPercentage: Double,
    val suggestionHi: String
)
