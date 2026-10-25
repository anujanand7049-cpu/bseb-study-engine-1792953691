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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.bseb.studyengine.domain.logic.QuizScoringEngine
import `in`.bseb.studyengine.ui.viewmodel.StudyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProgressAnalyticsScreen(
    viewModel: StudyViewModel,
    onReviseWeakChapter: (String) -> Unit
) {
    val subjectStats by viewModel.subjectPerformanceStats.collectAsState()
    val attempts by viewModel.quizAttempts.collectAsState()
    val weakTopics by viewModel.weakTopicSuggestions.collectAsState()

    val totalTests = attempts.size
    val totalQuestionsAttempted = attempts.sumOf { it.attemptedCount }
    val totalCorrect = attempts.sumOf { it.correctCount }
    val overallAccuracy = if (totalQuestionsAttempted > 0) {
        ((totalCorrect.toDouble() / totalQuestionsAttempted) * 1000.0).toInt() / 10.0
    } else {
        0.0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Overall Summary
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "समग्र अध्ययन प्रगति एवं प्रदर्शन डैशबोर्ड",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• पूर्ण किए गए टेस्ट: $totalTests
" +
                            "• कुल हल किए गए प्रश्न: $totalQuestionsAttempted (सही उत्तर: $totalCorrect)
" +
                            "• समग्र सटीकता (Overall Accuracy): $overallAccuracy%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Subject-wise Performance
        item {
            Text("विषय-वार प्रदर्शन एवं अध्याय कवरेज", style = MaterialTheme.typography.titleLarge)
        }

        items(subjectStats) { stat ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stat.subjectTitleHi, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("सटीकता: ${stat.accuracyPercentage}%", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (stat.accuracyPercentage.toFloat() / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "हल किए गए प्रश्न: ${stat.totalAttemptedQuestions} | सही: ${stat.totalCorrectAnswers} | उपलब्ध प्रश्न-बैंक: ${stat.totalQuestionsInBank}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Weak Topics & Revision Recommendations
        item {
            Text("कमजोर अध्याय एवं रिवीजन सुझाव", style = MaterialTheme.typography.titleLarge)
        }

        if (weakTopics.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "अभी तक किसी अध्याय में गंभीर कमजोरी दर्ज नहीं हुई है। अधिक सटीक विश्लेषण के लिए अध्याय-वार टेस्ट दें।",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(weakTopics) { weak ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "${weak.subjectTitleHi} • ${weak.chapterTitleHi}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "सटीकता: ${weak.accuracyPercentage}% • कुल त्रुटि भार: ${weak.mistakeCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = weak.suggestionHi,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { onReviseWeakChapter(weak.chapterId) }) {
                            Text("इस अध्याय का अभ्यास टेस्ट दें")
                        }
                    }
                }
            }
        }

        // Test History Log
        item {
            Text("पिछले टेस्ट का इतिहास (Test History)", style = MaterialTheme.typography.titleLarge)
        }

        if (attempts.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("आपने अभी तक कोई टेस्ट पूरा नहीं किया है।", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(attempts, key = { it.attemptId }) { att ->
                val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    .format(Date(att.timestampEpochMillis))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(att.titleHi, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("${att.finalScore}/${att.maxMarks}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            text = "दिनांक: $dateStr • सटीकता: ${att.accuracyPercentage}% • समय: ${QuizScoringEngine.formatCountdownTimer(att.timeTakenSeconds)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "सही: ${att.correctCount} | गलत: ${att.incorrectCount} | छोड़े गए: ${att.skippedCount}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
