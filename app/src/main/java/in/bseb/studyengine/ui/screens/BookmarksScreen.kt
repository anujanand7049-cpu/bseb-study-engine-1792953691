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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.bseb.studyengine.domain.model.QuizConfig
import `in`.bseb.studyengine.domain.model.QuizMode
import `in`.bseb.studyengine.ui.viewmodel.StudyViewModel

@Composable
fun BookmarksScreen(
    viewModel: StudyViewModel,
    onPracticeBookmarks: () -> Unit
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    val allQuestions by viewModel.allQuestions.collectAsState()
    val questionsMap = remember(allQuestions) { allQuestions.associateBy { it.questionId } }

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
                        text = "सहेजे गए महत्वपूर्ण प्रश्न (Bookmarks — ${bookmarks.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "बोर्ड परीक्षा से पहले त्वरित रिवीजन के लिए अपने बुकमार्क किए गए प्रश्नों का अभ्यास करें।",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val started = viewModel.startQuiz(
                                QuizConfig(
                                    titleHi = "बुकमार्क रिवीजन टेस्ट",
                                    onlyBookmarked = true,
                                    questionCount = bookmarks.size.coerceIn(1, 30),
                                    durationMinutes = 15,
                                    quizMode = QuizMode.PRACTICE_INSTANT
                                )
                            )
                            if (started) onPracticeBookmarks()
                        },
                        enabled = bookmarks.isNotEmpty()
                    ) {
                        Text("बुकमार्क प्रश्नों का टेस्ट दें (${bookmarks.size})")
                    }
                }
            }
        }

        if (bookmarks.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "अभी कोई प्रश्न बुकमार्क नहीं किया गया है।",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "प्रश्न बैंक या टेस्ट के दौरान बुकमार्क आइकन दबाकर कठिन या VVI प्रश्नों को यहाँ सुरक्षित रखें।",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(bookmarks, key = { it.questionId }) { bm ->
                val q = questionsMap[bm.questionId]
                if (q != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${q.textbook} • ${q.subSubject}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedButton(onClick = { viewModel.toggleBookmark(q.questionId) }) {
                                    Text("हटाएं")
                                }
                            }
                            Text(
                                text = q.questionText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
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
                        }
                    }
                }
            }
        }
    }
}
