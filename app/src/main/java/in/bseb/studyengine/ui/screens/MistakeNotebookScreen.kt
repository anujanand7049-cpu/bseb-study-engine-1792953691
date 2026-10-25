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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.bseb.studyengine.domain.model.QuizConfig
import `in`.bseb.studyengine.domain.model.QuizMode
import `in`.bseb.studyengine.ui.viewmodel.StudyViewModel

@Composable
fun MistakeNotebookScreen(
    viewModel: StudyViewModel,
    onPracticeMistakes: () -> Unit
) {
    val activeMistakes by viewModel.activeMistakes.collectAsState()
    val allMistakes by viewModel.allMistakes.collectAsState()
    val allQuestions by viewModel.allQuestions.collectAsState()

    var showResolvedToo by rememberSaveable { mutableStateOf(false) }
    val questionsMap = remember(allQuestions) { allQuestions.associateBy { it.questionId } }

    val listToDisplay = if (showResolvedToo) allMistakes else activeMistakes

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "गलती सुधार पुस्तिका (Mistake Notebook)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "जब भी किसी टेस्ट में कोई प्रश्न गलत होता है या छूट जाता है, वह स्वतः यहाँ दर्ज हो जाता है। जब आप बाद में टेस्ट में उसका सही उत्तर देते हैं, तो वह 'सुधारा गया (Resolved)' हो जाता है।",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val started = viewModel.startQuiz(
                                    QuizConfig(
                                        titleHi = "गलती सुधार रिवीजन टेस्ट",
                                        onlyMistakes = true,
                                        questionCount = activeMistakes.size.coerceIn(1, 25),
                                        durationMinutes = 15,
                                        quizMode = QuizMode.PRACTICE_INSTANT
                                    )
                                )
                                if (started) onPracticeMistakes()
                            },
                            enabled = activeMistakes.isNotEmpty()
                        ) {
                            Text("सभी ${activeMistakes.size} गलतियों का अभ्यास करें")
                        }
                        FilterChip(
                            selected = showResolvedToo,
                            onClick = { showResolvedToo = !showResolvedToo },
                            label = { Text(if (showResolvedToo) "केवल सक्रिय दिखाएं" else "सुधारे गए भी दिखाएं") }
                        )
                    }
                }
            }
        }

        if (listToDisplay.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "बधाई! आपकी गलती पुस्तिका में अभी कोई लंबित प्रश्न नहीं है।",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "जब आप अभ्यास या टाइमर टेस्ट देंगे, तो गलत या छूटे हुए प्रश्न यहाँ अपने-आप जुड़ जाएंगे।",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(listToDisplay, key = { it.questionId }) { record ->
                val q = questionsMap[record.questionId]
                if (q != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${q.textbook} • ${if (record.lastOutcome == "SKIPPED") "छोड़ा गया" else "गलत उत्तर"} (${record.mistakeCount} बार)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                                if (record.resolved) {
                                    Text(
                                        text = "✓ सुधारा गया",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = q.questionText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (record.userLastAnswer.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "आपका पिछला उत्तर: ${record.userLastAnswer}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "सही उत्तर: ${q.correctAnswer}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "व्याख्या: ${q.explanation}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.markMistakeResolved(record.questionId, !record.resolved) }
                            ) {
                                Text(if (record.resolved) "पुनः सक्रिय गलती में रखें" else "मैंने यह सीख लिया (Resolved चिह्नित करें)")
                            }
                        }
                    }
                }
            }
        }
    }
}
