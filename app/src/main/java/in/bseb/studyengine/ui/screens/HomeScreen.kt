package `in`.bseb.studyengine.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.bseb.studyengine.domain.model.QuizConfig
import `in`.bseb.studyengine.domain.model.QuizMode
import `in`.bseb.studyengine.ui.viewmodel.StudyViewModel

@Composable
fun HomeScreen(
    viewModel: StudyViewModel,
    onNavigateToSubjects: (String?) -> Unit,
    onNavigateToTests: (String?, String?) -> Unit,
    onNavigateToMistakes: () -> Unit,
    onNavigateToBookmarks: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToAi: () -> Unit
) {
    val settings by viewModel.settingsState.collectAsState()
    val subjectStats by viewModel.subjectPerformanceStats.collectAsState()
    val attempts by viewModel.quizAttempts.collectAsState()
    val activeMistakes by viewModel.activeMistakes.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val weakTopics by viewModel.weakTopicSuggestions.collectAsState()
    val allQuestions by viewModel.allQuestions.collectAsState()

    val todaySolved = viewModel.getTodayAttemptedQuestionsCount(attempts)
    val goalProgress = (todaySolved.toFloat() / settings.dailyQuestionGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome & Daily Study Goal Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "नमस्ते, ${settings.studentNameHi}!",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "बिहार विद्यालय परीक्षा समिति (BSEB) कक्षा 10 मैट्रिक परीक्षा की तैयारी — 100% ऑफलाइन और आपके Vivo T4R फ़ोन के लिए अनुकूलित।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "आज का अभ्यास लक्ष्य: $todaySolved / ${settings.dailyQuestionGoal} प्रश्न",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${(goalProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { goalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                }
            }
        }

        // Quick Action Buttons (Instant Mixed Test, Mistake Notebook, Bookmarks, AI Tutor)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "त्वरित अभ्यास (Quick Start)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val started = viewModel.startQuiz(
                                    QuizConfig(
                                        titleHi = "त्वरित मिश्रित बोर्ड मॉडल टेस्ट (10 प्रश्न)",
                                        questionCount = 10,
                                        durationMinutes = 10,
                                        includeObjectiveOnly = true,
                                        quizMode = QuizMode.EXAM_TIMED
                                    )
                                )
                                if (started) {
                                    onNavigateToTests(null, null)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("10-प्रश्न टेस्ट")
                        }

                        FilledTonalButton(
                            onClick = {
                                val started = viewModel.startQuiz(
                                    QuizConfig(
                                        titleHi = "त्वरित अभ्यास मोड (तुरंत व्याख्या)",
                                        questionCount = 10,
                                        durationMinutes = 15,
                                        quizMode = QuizMode.PRACTICE_INSTANT
                                    )
                                )
                                if (started) {
                                    onNavigateToTests(null, null)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("अभ्यास मोड")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToMistakes,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Filled.ErrorOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("गलतियाँ (${activeMistakes.size})")
                        }
                        OutlinedButton(
                            onClick = onNavigateToBookmarks,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Filled.Bookmark, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("बुकमार्क (${bookmarks.size})")
                        }
                    }
                }
            }
        }

        // Core Subjects Overview (Hindi, Sanskrit, Social Science)
        item {
            Text(
                text = "आपके मुख्य विषय (BSEB Class 10 Curriculum)",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(subjectStats) { stat ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSubjects(stat.subjectCode) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stat.subjectTitleHi,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "सटीकता: ${stat.accuracyPercentage}%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "कुल पाठ्यक्रम अध्याय: ${stat.totalChapters} (सत्यापित प्रश्न उपलब्ध: ${stat.populatedChapters} अध्याय, कुल ${stat.totalQuestionsInBank} प्रश्न)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "हल किए गए प्रश्न: ${stat.totalAttemptedQuestions} (सही: ${stat.totalCorrectAnswers})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        FilledTonalButton(
                            onClick = { onNavigateToTests(stat.subjectCode, null) }
                        ) {
                            Text("टेस्ट दें")
                        }
                    }
                }
            }
        }

        // Revision Suggestions from Mistake History
        if (weakTopics.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "रिवीजन सुझाव (कमजोर अध्याय)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        weakTopics.take(2).forEach { weak ->
                            Text(
                                text = "• ${weak.chapterTitleHi} (${weak.subjectTitleHi}) — ${weak.suggestionHi}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        OutlinedButton(onClick = onNavigateToProgress) {
                            Text("पूरी विश्लेषण रिपोर्ट देखें")
                        }
                    }
                }
            }
        }

        // Optional AI Study Assistant Card + Curriculum Verification Notice
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "वैकल्पिक AI अध्ययन सहायक एवं पाठ्यक्रम पारदर्शिता",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• इस ऐप में बिहार बोर्ड कक्षा 10 के सभी 75 आधिकारिक अध्याय (गोधूलि-2, वर्णिका-2, पीयूषम्-2, इतिहास, भूगोल, राजनीति विज्ञान, अर्थशास्त्र एवं आपदा प्रबंधन) सूचीबद्ध हैं और वर्तमान में ${allQuestions.size} सत्यापित अभ्यास प्रश्न लोड हैं।
• किसी भी नकली प्रश्न को 'आधिकारिक बोर्ड पेपर' नहीं बताया गया है। आप स्वयं नए प्रश्न जोड़ सकते हैं या JSON फ़ाइल इम्पोर्ट कर सकते हैं।",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = onNavigateToAi) {
                        Text("संस्कृत व्याकरण व उत्तर जाँच (AI / ऑफलाइन गाइड) खोलें")
                    }
                }
            }
        }
    }
}
