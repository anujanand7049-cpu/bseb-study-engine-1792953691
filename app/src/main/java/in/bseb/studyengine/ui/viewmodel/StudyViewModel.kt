package `in`.bseb.studyengine.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import `in`.bseb.studyengine.data.backup.BackupRestoreSerializer
import `in`.bseb.studyengine.data.local.BookmarkEntity
import `in`.bseb.studyengine.data.local.ChapterEntity
import `in`.bseb.studyengine.data.local.MistakeRecordEntity
import `in`.bseb.studyengine.data.local.QuestionEntity
import `in`.bseb.studyengine.data.local.QuizAttemptEntity
import `in`.bseb.studyengine.data.local.QuizAttemptItemEntity
import `in`.bseb.studyengine.data.local.SavedTestPresetEntity
import `in`.bseb.studyengine.data.preferences.UserPreferencesRepository
import `in`.bseb.studyengine.data.preferences.UserSettingsState
import `in`.bseb.studyengine.data.remote.AiAssistantResult
import `in`.bseb.studyengine.data.remote.GeminiAiAssistantService
import `in`.bseb.studyengine.data.repository.StudyRepository
import `in`.bseb.studyengine.domain.logic.QuizScoringEngine
import `in`.bseb.studyengine.domain.model.QuestionResponseState
import `in`.bseb.studyengine.domain.model.QuizConfig
import `in`.bseb.studyengine.domain.model.QuizEvaluationSummary
import `in`.bseb.studyengine.domain.model.QuizMode
import `in`.bseb.studyengine.domain.model.SubjectPerformanceStat
import `in`.bseb.studyengine.domain.model.SubjectType
import `in`.bseb.studyengine.domain.model.WeakTopicSuggestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

data class ActiveQuizSessionState(
    val isActive: Boolean = false,
    val config: QuizConfig = QuizConfig(titleHi = "बिहार बोर्ड अभ्यास टेस्ट"),
    val questions: List<QuestionEntity> = emptyList(),
    val shuffledOptionsMap: Map<String, List<String>> = emptyMap(),
    val currentIndex: Int = 0,
    val responses: Map<String, QuestionResponseState> = emptyMap(),
    val remainingSeconds: Int = 0,
    val elapsedSeconds: Int = 0,
    val instantFeedbackRevealedIds: Set<String> = emptySet(),
    val latestCompletedSummary: QuizEvaluationSummary? = null
)

data class AiAssistantUiState(
    val isLoading: Boolean = false,
    val lastResponseHi: String = "",
    val errorMessageHi: String? = null,
    val generatedCountMessageHi: String? = null
)

class StudyViewModel(
    private val repository: StudyRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val settingsState: StateFlow<UserSettingsState> = preferencesRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettingsState())

    val chapters: StateFlow<List<ChapterEntity>> = repository.chaptersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuestions: StateFlow<List<QuestionEntity>> = repository.questionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMistakes: StateFlow<List<MistakeRecordEntity>> = repository.activeMistakesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMistakes: StateFlow<List<MistakeRecordEntity>> = repository.allMistakesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizAttempts: StateFlow<List<QuizAttemptEntity>> = repository.quizAttemptsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttemptItems: StateFlow<List<QuizAttemptItemEntity>> = repository.allAttemptItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPresets: StateFlow<List<SavedTestPresetEntity>> = repository.savedPresetsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quizSession = MutableStateFlow(ActiveQuizSessionState())
    val quizSession: StateFlow<ActiveQuizSessionState> = _quizSession.asStateFlow()

    private val _aiState = MutableStateFlow(AiAssistantUiState())
    val aiState: StateFlow<AiAssistantUiState> = _aiState.asStateFlow()

    private val _statusBannerHi = MutableStateFlow<String?>(null)
    val statusBannerHi: StateFlow<String?> = _statusBannerHi.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureInitialSyllabusSeeded()
        }
    }

    fun dismissBanner() {
        _statusBannerHi.value = null
    }

    fun showStatusMessage(msgHi: String) {
        _statusBannerHi.value = msgHi
    }

    // ---------------- ANALYTICS COMPUTATIONS ----------------
    val subjectPerformanceStats: StateFlow<List<SubjectPerformanceStat>> = combine(
        chapters,
        allQuestions,
        allAttemptItems
    ) { chList, qList, itemList ->
        val qById = qList.associateBy { it.questionId }
        SubjectType.entries.map { subject ->
            val subChapters = chList.filter { it.subject == subject.code }
            val subQuestions = qList.filter { it.subject == subject.code }
            val subItems = itemList.filter { qById[it.questionId]?.subject == subject.code && it.outcome != "SKIPPED" }
            val correctItems = subItems.count { it.outcome == "CORRECT" }
            val acc = if (subItems.isNotEmpty()) {
                (correctItems.toDouble() / subItems.size.toDouble()) * 100.0
            } else {
                0.0
            }
            SubjectPerformanceStat(
                subjectCode = subject.code,
                subjectTitleHi = subject.titleHi,
                totalChapters = subChapters.size,
                populatedChapters = subChapters.count { it.isPopulated },
                totalQuestionsInBank = subQuestions.size,
                totalAttemptedQuestions = subItems.size,
                totalCorrectAnswers = correctItems,
                accuracyPercentage = (acc * 10.0).toInt() / 10.0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weakTopicSuggestions: StateFlow<List<WeakTopicSuggestion>> = combine(
        chapters,
        activeMistakes,
        allAttemptItems,
        allQuestions
    ) { chList, mistakesList, itemsList, qList ->
        val chById = chList.associateBy { it.chapterId }
        val qById = qList.associateBy { it.questionId }

        val groupedMistakes = mistakesList.groupBy { it.chapterId }
        groupedMistakes.mapNotNull { (chId, mRecords) ->
            val chapter = chById[chId] ?: return@mapNotNull null
            val chapterAttemptItems = itemsList.filter { qById[it.questionId]?.chapterId == chId && it.outcome != "SKIPPED" }
            val correctCount = chapterAttemptItems.count { it.outcome == "CORRECT" }
            val acc = if (chapterAttemptItems.isNotEmpty()) {
                ((correctCount.toDouble() / chapterAttemptItems.size) * 100.0).toInt().toDouble()
            } else {
                0.0
            }
            val totalMistakeWeight = mRecords.sumOf { it.mistakeCount }
            val subjectTitle = SubjectType.fromCode(chapter.subject).titleHi
            WeakTopicSuggestion(
                chapterId = chId,
                chapterTitleHi = "${chapter.textbook}: ${chapter.titleHi}",
                subjectTitleHi = subjectTitle,
                mistakeCount = totalMistakeWeight,
                accuracyPercentage = acc,
                suggestionHi = "इस अध्याय में आपके ${mRecords.size} प्रश्न गलत या छूटे हैं। पहले 'गलती सुधार पुस्तिका' से इनका रिवीजन करें और फिर 10 प्रश्नों का अध्याय-वार टेस्ट दें।"
            )
        }.sortedByDescending { it.mistakeCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getTodayAttemptedQuestionsCount(attempts: List<QuizAttemptEntity>): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDayMillis = cal.timeInMillis
        return attempts.filter { it.timestampEpochMillis >= startOfDayMillis }
            .sumOf { it.attemptedCount }
    }

    // ---------------- QUIZ WORKFLOW ----------------
    fun startQuiz(config: QuizConfig): Boolean {
        val bookmarkedIds = bookmarks.value.map { it.questionId }.toSet()
        val mistakeIds = activeMistakes.value.map { it.questionId }.toSet()

        val selectedQuestions = QuizScoringEngine.selectQuestionsForQuiz(
            allQuestions = allQuestions.value,
            bookmarkedIds = bookmarkedIds,
            mistakeQuestionIds = mistakeIds,
            config = config
        )

        if (selectedQuestions.isEmpty()) {
            showStatusMessage("चयनित फ़िल्टर के लिए कोई प्रश्न उपलब्ध नहीं है। कृपया विषय या फ़िल्टर बदलें।")
            return false
        }

        val optionsMap = selectedQuestions.associate { q ->
            val rawList = BackupRestoreSerializer.parseOptionsJson(q.optionsJson)
            val finalOptions = if (config.shuffleOptions && q.questionType == "MCQ") {
                rawList.shuffled()
            } else {
                rawList
            }
            q.questionId to finalOptions
        }

        val initialResponses = selectedQuestions.associate { q ->
            q.questionId to QuestionResponseState(questionId = q.questionId)
        }

        val totalSeconds = (config.durationMinutes.coerceAtLeast(1)) * 60

        _quizSession.value = ActiveQuizSessionState(
            isActive = true,
            config = config,
            questions = selectedQuestions,
            shuffledOptionsMap = optionsMap,
            currentIndex = 0,
            responses = initialResponses,
            remainingSeconds = totalSeconds,
            elapsedSeconds = 0,
            instantFeedbackRevealedIds = emptySet(),
            latestCompletedSummary = null
        )

        startTimerLoop()
        return true
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_quizSession.value.isActive) {
                delay(1000L)
                val current = _quizSession.value
                if (!current.isActive) break

                val currentQ = current.questions.getOrNull(current.currentIndex)
                val updatedResponses = if (currentQ != null) {
                    val prevResp = current.responses[currentQ.questionId] ?: QuestionResponseState(currentQ.questionId)
                    current.responses + (currentQ.questionId to prevResp.copy(timeSpentSeconds = prevResp.timeSpentSeconds + 1))
                } else {
                    current.responses
                }

                val newRemaining = (current.remainingSeconds - 1).coerceAtLeast(0)
                val newElapsed = current.elapsedSeconds + 1

                _quizSession.update {
                    it.copy(
                        remainingSeconds = newRemaining,
                        elapsedSeconds = newElapsed,
                        responses = updatedResponses
                    )
                }

                if (QuizScoringEngine.shouldAutoSubmit(
                        remainingSeconds = newRemaining,
                        isExamTimedMode = current.config.quizMode == QuizMode.EXAM_TIMED
                    )
                ) {
                    submitActiveQuiz(autoSubmitted = true)
                    break
                }
            }
        }
    }

    fun selectOrTypeAnswer(questionId: String, answer: String) {
        _quizSession.update { state ->
            val existing = state.responses[questionId] ?: QuestionResponseState(questionId)
            state.copy(
                responses = state.responses + (questionId to existing.copy(selectedOrTypedAnswer = answer))
            )
        }
    }

    fun setSubjectiveSelfEvaluation(questionId: String, markedCorrect: Boolean) {
        _quizSession.update { state ->
            val existing = state.responses[questionId] ?: QuestionResponseState(questionId)
            state.copy(
                responses = state.responses + (questionId to existing.copy(selfEvaluatedSubjectiveCorrect = markedCorrect))
            )
        }
    }

    fun toggleMarkForReview(questionId: String) {
        _quizSession.update { state ->
            val existing = state.responses[questionId] ?: QuestionResponseState(questionId)
            state.copy(
                responses = state.responses + (questionId to existing.copy(isMarkedForReview = !existing.isMarkedForReview))
            )
        }
    }

    fun revealInstantPracticeFeedback(questionId: String) {
        _quizSession.update { state ->
            // Never reveal answers before submission when EXAM_TIMED mode is active
            if (state.config.quizMode == QuizMode.EXAM_TIMED) {
                state
            } else {
                state.copy(instantFeedbackRevealedIds = state.instantFeedbackRevealedIds + questionId)
            }
        }
    }

    fun navigateToQuestionIndex(index: Int) {
        _quizSession.update { state ->
            if (index in state.questions.indices) {
                state.copy(currentIndex = index)
            } else {
                state
            }
        }
    }

    fun submitActiveQuiz(autoSubmitted: Boolean = false) {
        timerJob?.cancel()
        val state = _quizSession.value
        if (!state.isActive && state.latestCompletedSummary != null) return

        val summary = QuizScoringEngine.evaluateQuiz(
            questions = state.questions,
            responses = state.responses,
            config = state.config,
            totalTimeTakenSeconds = state.elapsedSeconds,
            autoSubmittedOnTimeout = autoSubmitted
        )

        _quizSession.update {
            it.copy(
                isActive = false,
                latestCompletedSummary = summary
            )
        }

        val questionsMap = state.questions.associateBy { it.questionId }
        viewModelScope.launch {
            repository.recordCompletedQuiz(
                config = state.config,
                questionsMap = questionsMap,
                evaluation = summary
            )
            if (autoSubmitted) {
                showStatusMessage("समय समाप्त! आपका टेस्ट स्वतः सबमिट (Auto-Submit) हो गया है।")
            }
        }
    }

    fun clearQuizSession() {
        timerJob?.cancel()
        _quizSession.value = ActiveQuizSessionState()
    }

    // ---------------- BOOKMARKS, MISTAKES & CONTENT REPORTING ----------------
    fun toggleBookmark(questionId: String, noteHi: String = "") {
        val isCurrentlyBookmarked = bookmarks.value.any { it.questionId == questionId }
        viewModelScope.launch {
            repository.toggleBookmark(questionId, isCurrentlyBookmarked, noteHi)
            showStatusMessage(
                if (isCurrentlyBookmarked) "बुकमार्क से हटाया गया।" else "महत्वपूर्ण प्रश्नों (बुकमार्क) में जोड़ा गया।"
            )
        }
    }

    fun reportQuestion(questionId: String, reasonHi: String) {
        viewModelScope.launch {
            repository.reportQuestionContent(questionId, reasonHi)
            showStatusMessage("प्रश्न को समीक्षा हेतु चिह्नित किया गया है।")
        }
    }

    fun markMistakeResolved(questionId: String, resolved: Boolean) {
        viewModelScope.launch {
            repository.setMistakeResolvedStatus(questionId, resolved)
        }
    }

    fun savePreset(
        titleHi: String,
        subjectFilter: String?,
        subSubjectFilter: String?,
        chapterIdFilter: String?,
        questionCount: Int,
        durationMinutes: Int,
        negativeMarking: Double
    ) {
        viewModelScope.launch {
            repository.saveTestPreset(
                SavedTestPresetEntity(
                    titleHi = titleHi.ifBlank { "मेरा सहेजा गया टेस्ट" },
                    subjectFilter = subjectFilter,
                    subSubjectFilter = subSubjectFilter,
                    chapterIdFilter = chapterIdFilter,
                    questionCount = questionCount,
                    durationMinutes = durationMinutes,
                    negativeMarking = negativeMarking
                )
            )
            showStatusMessage("टेस्ट टेम्पलेट सहेजा गया!")
        }
    }

    fun deletePreset(presetId: Long) {
        viewModelScope.launch {
            repository.deleteTestPreset(presetId)
        }
    }

    // ---------------- ADD CUSTOM QUESTION & JSON IMPORT ----------------
    fun addUserCustomQuestion(
        chapter: ChapterEntity,
        topic: String,
        questionType: String,
        difficulty: String,
        questionText: String,
        options: List<String>,
        correctAnswer: String,
        explanation: String,
        marks: Int,
        sourceDetail: String
    ) {
        viewModelScope.launch {
            val newQ = QuestionEntity(
                questionId = "Q_USR_${UUID.randomUUID().toString().take(8).uppercase()}",
                chapterId = chapter.chapterId,
                subject = chapter.subject,
                subSubject = chapter.subSubject,
                textbook = chapter.textbook,
                topic = topic.ifBlank { chapter.titleHi },
                questionType = questionType,
                difficulty = difficulty,
                questionText = questionText.trim(),
                optionsJson = BackupRestoreSerializer.encodeOptionsJson(options),
                correctAnswer = correctAnswer.trim(),
                explanation = explanation.trim(),
                marks = marks.coerceIn(1, 10),
                sourceType = "USER_ADDED",
                sourceDetail = sourceDetail.ifBlank { "छात्र द्वारा जोड़ा गया अभ्यास प्रश्न" },
                verificationStatus = "USER_VERIFIED"
            )
            repository.addCustomOrAiQuestions(listOf(newQ))
            showStatusMessage("नया प्रश्न सफलतापूर्वक प्रश्न-बैंक में जोड़ दिया गया!")
        }
    }

    fun importQuestionsFromJsonText(jsonString: String) {
        viewModelScope.launch {
            try {
                val (chCount, qCount) = repository.importQuestionBankJson(jsonString)
                showStatusMessage("सफलतापूर्वक $qCount प्रश्न और $chCount अध्याय आयात (Import) किए गए!")
            } catch (e: Exception) {
                showStatusMessage("JSON आयात त्रुटि: ${e.localizedMessage ?: "अमान्य प्रारूप"}")
            }
        }
    }

    // ---------------- SETTINGS & BACKUP/RESTORE ----------------
    fun updateProfileAndGoal(nameHi: String, dailyGoal: Int) {
        viewModelScope.launch {
            preferencesRepository.updateStudentName(nameHi)
            preferencesRepository.updateDailyGoal(dailyGoal)
            showStatusMessage("प्रोफ़ाइल और दैनिक लक्ष्य सहेजे गए।")
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(mode)
        }
    }

    fun updateShowEnglishTerms(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateShowEnglishTerms(enabled)
        }
    }

    fun updateGeminiSettings(apiKey: String, consentAccepted: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateGeminiApiKeyAndConsent(apiKey, consentAccepted)
            showStatusMessage("AI सहायक सेटिंग्स आपके फ़ोन में सुरक्षित रूप से सहेजी गईं।")
        }
    }

    fun exportBackupToCallback(onReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.createBackupJsonString()
                onReady(json)
            } catch (e: Exception) {
                showStatusMessage("बैकअप बनाने में त्रुटि: ${e.localizedMessage}")
            }
        }
    }

    fun restoreBackupFromJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val restoredItems = repository.restoreFromBackupJsonString(jsonString)
                showStatusMessage("बैकअप सफलतापूर्वक रीस्टोर हो गया! ($restoredItems रिकॉर्ड बहाल किए गए)")
            } catch (e: Exception) {
                showStatusMessage("बैकअप रीस्टोर त्रुटि: ${e.localizedMessage}")
            }
        }
    }

    fun clearAllUserProgress() {
        viewModelScope.launch {
            repository.clearAllUserProgress()
            showStatusMessage("आपका व्यक्तिगत अभ्यास इतिहास साफ़ कर दिया गया है (मूल पाठ्यक्रम सुरक्षित है)।")
        }
    }

    // ---------------- OPTIONAL AI STUDY ASSISTANT ----------------
    fun askAiTutor(
        taskType: String,
        subjectHi: String,
        userQuery: String,
        referenceAnswer: String = ""
    ) {
        val currentSettings = settingsState.value
        if (!currentSettings.aiConsentAccepted) {
            _aiState.value = AiAssistantUiState(
                errorMessageHi = "ऑनलाइन AI का उपयोग करने से पहले कृपया डेटा सहमति (Consent) स्वीकार करें।"
            )
            return
        }
        viewModelScope.launch {
            _aiState.value = AiAssistantUiState(isLoading = true)
            when (
                val res = GeminiAiAssistantService.askStudyAssistant(
                    apiKey = currentSettings.geminiApiKey,
                    taskType = taskType,
                    subjectHi = subjectHi,
                    userInput = userQuery,
                    referenceAnswer = referenceAnswer
                )
            ) {
                is AiAssistantResult.SuccessText -> {
                    _aiState.value = AiAssistantUiState(isLoading = false, lastResponseHi = res.responseHi)
                }
                is AiAssistantResult.Error -> {
                    _aiState.value = AiAssistantUiState(isLoading = false, errorMessageHi = res.messageHi)
                }
                else -> {
                    _aiState.value = AiAssistantUiState(isLoading = false)
                }
            }
        }
    }

    fun generateAiQuestionsForChapter(chapter: ChapterEntity, count: Int = 3) {
        val currentSettings = settingsState.value
        if (!currentSettings.aiConsentAccepted) {
            _aiState.value = AiAssistantUiState(
                errorMessageHi = "ऑनलाइन AI प्रश्न जनरेट करने के लिए पहले सहमति स्वीकार करें।"
            )
            return
        }
        viewModelScope.launch {
            _aiState.value = AiAssistantUiState(isLoading = true)
            when (
                val res = GeminiAiAssistantService.generateAiPracticeQuestions(
                    apiKey = currentSettings.geminiApiKey,
                    chapterId = chapter.chapterId,
                    subjectCode = chapter.subject,
                    subSubjectHi = chapter.subSubject,
                    textbookHi = chapter.textbook,
                    chapterTitleHi = chapter.titleHi,
                    count = count
                )
            ) {
                is AiAssistantResult.SuccessGeneratedQuestions -> {
                    repository.addCustomOrAiQuestions(res.questions)
                    _aiState.value = AiAssistantUiState(
                        isLoading = false,
                        generatedCountMessageHi = "'${chapter.titleHi}' के लिए ${res.questions.size} नए AI अभ्यास प्रश्न (AI_GENERATED टैग के साथ) जोड़े गए!"
                    )
                }
                is AiAssistantResult.Error -> {
                    _aiState.value = AiAssistantUiState(isLoading = false, errorMessageHi = res.messageHi)
                }
                else -> {
                    _aiState.value = AiAssistantUiState(isLoading = false)
                }
            }
        }
    }

    class Factory(
        private val repository: StudyRepository,
        private val preferencesRepository: UserPreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudyViewModel(repository, preferencesRepository) as T
        }
    }
}
