package `in`.bseb.studyengine.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.bseb.studyengine.domain.logic.QuizScoringEngine
import `in`.bseb.studyengine.domain.model.AttemptOutcome
import `in`.bseb.studyengine.domain.model.QuizConfig
import `in`.bseb.studyengine.domain.model.QuizMode
import `in`.bseb.studyengine.ui.viewmodel.StudyViewModel

@Composable
fun QuizResultScreen(
    viewModel: StudyViewModel,
    onDone: () -> Unit,
    onRetakeMistakes: () -> Unit
) {
    val session by viewModel.quizSession.collectAsState()
    val summary = session.latestCompletedSummary

    if (summary == null) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("कोई परिणाम उपलब्ध नहीं है।")
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onDone) { Text("मुख्य पृष्ठ पर जाएं") }
        }
        return
    }

    var filterOutcome by rememberSaveable { mutableStateOf<String?>(null) } // null = All, CORRECT, INCORRECT, SKIPPED

    val displayedItems = summary.itemEvaluations.filter {
        filterOutcome == null || it.status.name == filterOutcome
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Scoreboard Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (summary.autoSubmittedOnTimeout) {
                            "परीक्षा परिणाम (समय समाप्ति पर स्वतः सबमिट)"
                        } else {
                            "परीक्षा परिणाम एवं विस्तृत विश्लेषण"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "अंतिम प्राप्तांक: ${summary.finalScore} / ${summary.maxPossibleMarks} (${summary.scorePercentage}%)",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• सटीकता (Accuracy): ${summary.accuracyPercentage}%
" +
                            "• कुल समय: ${QuizScoringEngine.formatCountdownTimer(summary.totalTimeTakenSeconds)}
" +
                            "• सही उत्तर: ${summary.correctCount} | गलत उत्तर: ${summary.incorrectCount} | छोड़े गए (Unanswered): ${summary.skippedCount}
" +
                            "• नेगेटिव मार्किंग कटौती: -${summary.negativeMarksDeducted} अंक",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onDone, modifier = Modifier.weight(1f)) {
                            Text("मुख्य पृष्ठ")
                        }
                        if (summary.incorrectCount + summary.skippedCount > 0) {
                            OutlinedButton(
                                onClick = {
                                    val started = viewModel.startQuiz(
                                        QuizConfig(
                                            titleHi = "गलत व छूटे प्रश्नों का पुनः अभ्यास",
                                            onlyMistakes = true,
                                            questionCount = (summary.incorrectCount + summary.skippedCount).coerceAtLeast(5),
                                            durationMinutes = 10,
                                            quizMode = QuizMode.PRACTICE_INSTANT
                                        )
                                    )
                                    if (started) onRetakeMistakes()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("गलतियों का अभ्यास करें")
                            }
                        }
                    }
                }
            }
        }

        // Filter Chips for Question-by-Question Review
        item {
            Text(
                text = "प्रश्नवार उत्तर एवं व्याख्या समीक्षा",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterOutcome == null,
                    onClick = { filterOutcome = null },
                    label = { Text("सभी (${summary.totalQuestions})") }
                )
                FilterChip(
                    selected = filterOutcome == AttemptOutcome.INCORRECT.name,
                    onClick = { filterOutcome = AttemptOutcome.INCORRECT.name },
                    label = { Text("गलत (${summary.incorrectCount})") }
                )
                FilterChip(
                    selected = filterOutcome == AttemptOutcome.SKIPPED.name,
                    onClick = { filterOutcome = AttemptOutcome.SKIPPED.name },
                    label = { Text("छोड़े गए (${summary.skippedCount})") }
                )
                FilterChip(
                    selected = filterOutcome == AttemptOutcome.CORRECT.name,
                    onClick = { filterOutcome = AttemptOutcome.CORRECT.name },
                    label = { Text("सही (${summary.correctCount})") }
                )
            }
        }

        itemsIndexed(displayedItems, key = { _, item -> item.questionId }) { idx, item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${idx + 1}. ${item.status.labelHi}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = when (item.status) {
                                AttemptOutcome.CORRECT -> MaterialTheme.colorScheme.tertiary
                                AttemptOutcome.INCORRECT -> MaterialTheme.colorScheme.error
                                AttemptOutcome.SKIPPED -> MaterialTheme.colorScheme.secondary
                            }
                        )
                        Text(
                            text = "अंक: ${item.marksAwarded}/${item.maxMarks} • समय: ${item.timeSpentSeconds}s",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.questionText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "आपका उत्तर: ${item.userAnswer.ifBlank { "कोई उत्तर नहीं दिया (Skipped)" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "सही उत्तर: ${item.correctAnswer}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    if (item.explanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "व्याख्या: ${item.explanation}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
