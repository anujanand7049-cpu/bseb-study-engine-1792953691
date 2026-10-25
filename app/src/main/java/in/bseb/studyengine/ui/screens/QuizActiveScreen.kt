package `in`.bseb.studyengine.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.bseb.studyengine.domain.logic.QuizScoringEngine
import `in`.bseb.studyengine.domain.model.QuestionFormat
import `in`.bseb.studyengine.domain.model.QuizMode
import `in`.bseb.studyengine.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizActiveScreen(
    viewModel: StudyViewModel,
    onQuizSubmitted: () -> Unit,
    onExitQuiz: () -> Unit
) {
    val session by viewModel.quizSession.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    var showSubmitConfirmDialog by remember { mutableStateOf(false) }

    // Automatically transition to Result screen if auto-submitted on timer expiration
    LaunchedEffect(session.isActive, session.latestCompletedSummary) {
        if (!session.isActive && session.latestCompletedSummary != null) {
            onQuizSubmitted()
        }
    }

    val currentQuestion = session.questions.getOrNull(session.currentIndex)
    if (currentQuestion == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("कोई सक्रिय प्रश्न नहीं मिला।", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onExitQuiz) {
                Text("मुख्य पृष्ठ पर वापस जाएं")
            }
        }
        return
    }

    val qFormat = QuestionFormat.fromCode(currentQuestion.questionType)
    val currentResponse = session.responses[currentQuestion.questionId]
    val selectedAnswer = currentResponse?.selectedOrTypedAnswer.orEmpty()
    val isMarkedForReview = currentResponse?.isMarkedForReview == true
    val isBookmarked = bookmarks.any { it.questionId == currentQuestion.questionId }
    val options = session.shuffledOptionsMap[currentQuestion.questionId].orEmpty()
    val isExamTimedMode = session.config.quizMode == QuizMode.EXAM_TIMED
    val isInstantRevealed = !isExamTimedMode && session.instantFeedbackRevealedIds.contains(currentQuestion.questionId)

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Exam Header Bar with Countdown Timer
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.config.titleHi,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Timer, contentDescription = "Timer")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = QuizScoringEngine.formatCountdownTimer(session.remainingSeconds),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (session.remainingSeconds <= 60) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (session.currentIndex + 1).toFloat() / session.questions.size.coerceAtLeast(1).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Question Palette (Quick Jump & Review Status)
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    session.questions.forEachIndexed { idx, q ->
                        val resp = session.responses[q.questionId]
                        val answered = !resp?.selectedOrTypedAnswer.isNullOrBlank()
                        val marked = resp?.isMarkedForReview == true
                        FilterChip(
                            selected = idx == session.currentIndex,
                            onClick = { viewModel.navigateToQuestionIndex(idx) },
                            label = {
                                Text(
                                    buildString {
                                        append("${idx + 1}")
                                        if (marked) append("★")
                                        else if (answered) append("✓")
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // Question Metadata & Actions (Bookmark, Mark for Later)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "प्रश्न ${session.currentIndex + 1} / ${session.questions.size} • ${qFormat.labelHi}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(
                            onClick = { viewModel.toggleMarkForReview(currentQuestion.questionId) },
                            label = {
                                Text(if (isMarkedForReview) "★ बाद के लिए चिह्नित" else "बाद के लिए चिह्नित करें")
                            }
                        )
                        IconButton(onClick = { viewModel.toggleBookmark(currentQuestion.questionId) }) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "बुकमार्क करें",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.reportQuestion(
                                    currentQuestion.questionId,
                                    "टेस्ट के दौरान छात्र द्वारा समीक्षा हेतु चिह्नित"
                                )
                            }
                        ) {
                            Icon(imageVector = Icons.Filled.Flag, contentDescription = "त्रुटि रिपोर्ट करें")
                        }
                    }
                }
            }

            // Question Stem Card
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${currentQuestion.textbook} • ${currentQuestion.subSubject} (${currentQuestion.marks} अंक)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentQuestion.questionText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Answer Input Area based on QuestionFormat
            item {
                if (options.isNotEmpty() && (qFormat == QuestionFormat.MCQ || qFormat == QuestionFormat.TRUE_FALSE || qFormat == QuestionFormat.MATCHING)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        options.forEachIndexed { idx, optionText ->
                            val isSelected = selectedAnswer == optionText
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectOrTypeAnswer(currentQuestion.questionId, optionText)
                                    },
                                border = if (isSelected) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.selectOrTypeAnswer(currentQuestion.questionId, optionText)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${('A' + idx)}. $optionText",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Fill in the blank OR Short/Long Subjective Answer
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = selectedAnswer,
                            onValueChange = {
                                viewModel.selectOrTypeAnswer(currentQuestion.questionId, it)
                            },
                            label = {
                                Text(
                                    if (qFormat == QuestionFormat.FILL_BLANK) {
                                        "रिक्त स्थान का सही शब्द लिखें (हिंदी / संस्कृत / संख्या)"
                                    } else {
                                        "अपना लघु/दीर्घ उत्तर यहाँ हिंदी या संस्कृत में लिखें..."
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = if (qFormat.isObjective) 1 else 4
                        )

                        if (!qFormat.isObjective && selectedAnswer.isNotBlank()) {
                            Text(
                                text = "स्व-मूल्यांकन (टेस्ट सबमिट करने से पहले चुनें कि क्या आपका उत्तर मुख्य बिंदुओं को कवर करता है):",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = currentResponse?.selfEvaluatedSubjectiveCorrect == true,
                                    onClick = {
                                        viewModel.setSubjectiveSelfEvaluation(currentQuestion.questionId, true)
                                    },
                                    label = { Text("हाँ, मुख्य बिंदु लिखे हैं") }
                                )
                                FilterChip(
                                    selected = currentResponse?.selfEvaluatedSubjectiveCorrect == false,
                                    onClick = {
                                        viewModel.setSubjectiveSelfEvaluation(currentQuestion.questionId, false)
                                    },
                                    label = { Text("अधूरा है / रिवीजन चाहिए") }
                                )
                            }
                        }
                    }
                }
            }

            // Instant Feedback Section (STRICTLY ONLY in PRACTICE_INSTANT mode, NEVER in EXAM_TIMED mode)
            if (!isExamTimedMode) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.revealInstantPracticeFeedback(currentQuestion.questionId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("अभ्यास मोड: सही उत्तर और व्याख्या अभी देखें")
                    }

                    if (isInstantRevealed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "सही उत्तर: ${currentQuestion.correctAnswer}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "व्याख्या: ${currentQuestion.explanation}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Navigation Bar (Previous, Next, Submit Test)
        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.navigateToQuestionIndex(session.currentIndex - 1) },
                    enabled = session.currentIndex > 0
                ) {
                    Text("← पिछला")
                }

                Button(
                    onClick = { showSubmitConfirmDialog = true }
                ) {
                    Text("टेस्ट सबमिट करें")
                }

                OutlinedButton(
                    onClick = { viewModel.navigateToQuestionIndex(session.currentIndex + 1) },
                    enabled = session.currentIndex < session.questions.lastIndex
                ) {
                    Text("अगला →")
                }
            }
        }
    }

    if (showSubmitConfirmDialog) {
        val answeredCount = session.responses.values.count { it.selectedOrTypedAnswer.isNotBlank() }
        val unansweredCount = session.questions.size - answeredCount
        val markedCount = session.responses.values.count { it.isMarkedForReview }

        AlertDialog(
            onDismissRequest = { showSubmitConfirmDialog = false },
            title = { Text("क्या आप टेस्ट सबमिट करना चाहते हैं?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• कुल प्रश्न: ${session.questions.size}")
                    Text("• उत्तर दिए गए: $answeredCount")
                    Text("• बिना उत्तर के छूटे प्रश्न: $unansweredCount")
                    Text("• बाद के लिए चिह्नित (★): $markedCount")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmDialog = false
                        viewModel.submitActiveQuiz(autoSubmitted = false)
                        onQuizSubmitted()
                    }
                ) {
                    Text("हाँ, परिणाम देखें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmDialog = false }) {
                    Text("अभी वापस लौटें")
                }
            }
        )
    }
}
