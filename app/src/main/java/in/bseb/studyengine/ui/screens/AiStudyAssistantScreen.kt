package `in`.bseb.studyengine.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import `in`.bseb.studyengine.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiStudyAssistantScreen(
    viewModel: StudyViewModel,
    onOpenSettings: () -> Unit
) {
    val settings by viewModel.settingsState.collectAsState()
    val chapters by viewModel.chapters.collectAsState()
    val aiState by viewModel.aiState.collectAsState()

    var selectedTask by rememberSaveable { mutableStateOf("EXPLAIN_CONCEPT") }
    var selectedSubjectHi by rememberSaveable { mutableStateOf("हिंदी / संस्कृत / सामाजिक विज्ञान") }
    var userInputText by rememberSaveable { mutableStateOf("") }
    var referenceRubricText by rememberSaveable { mutableStateOf("") }
    var selectedChapterIndex by rememberSaveable { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Offline Built-in Sanskrit Grammar & Subjective Answer Reference Card (Always Works 100% Offline)
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ऑफलाइन संस्कृत व्याकरण एवं बोर्ड उत्तर-लेखन सूत्र (बिना इंटरनेट)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. प्रमुख कारक सूत्र (BSEB VVI):
" +
                            "• सहार्थे तृतीया: 'सह/साकम्' (साथ) के योग में तृतीया विभक्ति (जैसे— रामेण सह सीता गच्छति)।
" +
                            "• येनाङ्गविकारः: शरीर के जिस अंग में विकार हो उसमें तृतीया (जैसे— सः कर्णेन बधिरः अस्ति)।
" +
                            "• रुच्यर्थानां प्रीयमाणः / नमः स्वस्ति स्वाहा: चतुर्थी विभक्ति (जैसे— बालकाय मोदकं रोचते, श्रीगणेशाय नमः)।
" +
                            "• भुवः प्रभवः / भीतरार्थानां भयहेतुः: उद्गम स्थान व भय में पञ्चमी विभक्ति (जैसे— गङ्गा हिमालयात् प्रभवति, चौरात् बिभेति)।
" +
                            "2. दीर्घ उत्तरीय उत्तर (4-5 अंक) की संरचना:
" +
                            "• (क) 2 पंक्तियों में पाठ/लेखक या ऐतिहासिक संदर्भ का परिचय, (ख) 3 से 4 स्पष्ट बुलेट बिंदु (मुख्य तर्क), (ग) 1-2 पंक्ति में निष्कर्ष।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Online Optional AI Disclosure & Configuration Card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "वैकल्पिक ऑनलाइन AI अध्ययन सहायक (Google Gemini Free Tier)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• पारदर्शिता सूचना: ऐप के सभी मुख्य फीचर (क्विज़, टाइमर, प्रश्न-बैंक, गलती पुस्तिका, प्रगति) 100% ऑफलाइन चलते हैं।
" +
                            "• यदि आप चाहें, तो Google AI Studio की निःशुल्क (₹0) API Key सेटिंग्स में डालकर ऑनलाइन AI से सरल हिंदी व्याख्या, संस्कृत अनुवाद, लघु/दीर्घ उत्तर जाँच और अतिरिक्त AI अभ्यास प्रश्न (स्पष्ट रूप से 'AI_GENERATED' चिह्नित) प्राप्त कर सकते हैं।
" +
                            "• सीमाएँ (Rate Limits): निःशुल्क टियर में प्रति मिनट सीमित अनुरोध (लगभग 15 RPM) और इंटरनेट कनेक्शन आवश्यक है।",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (settings.geminiApiKey.isBlank() || !settings.aiConsentAccepted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onOpenSettings) {
                            Text("सेटिंग्स में निःशुल्क Gemini API Key व सहमति सेट करें")
                        }
                    }
                }
            }
        }

        // AI Tutor Task Selector & Input
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("AI सहायक से पूछें:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedTask == "EXPLAIN_CONCEPT",
                            onClick = { selectedTask = "EXPLAIN_CONCEPT" },
                            label = { Text("सरल हिंदी व्याख्या") }
                        )
                        FilterChip(
                            selected = selectedTask == "SANSKRIT_GRAMMAR",
                            onClick = { selectedTask = "SANSKRIT_GRAMMAR" },
                            label = { Text("संस्कृत व्याकरण व अनुवाद") }
                        )
                        FilterChip(
                            selected = selectedTask == "EVALUATE_SUBJECTIVE",
                            onClick = { selectedTask = "EVALUATE_SUBJECTIVE" },
                            label = { Text("लघु/दीर्घ उत्तर की जाँच") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userInputText,
                        onValueChange = { userInputText = it },
                        label = {
                            Text(
                                when (selectedTask) {
                                    "SANSKRIT_GRAMMAR" -> "संस्कृत श्लोक, संधि, समास या अनुवाद के लिए वाक्य लिखें"
                                    "EVALUATE_SUBJECTIVE" -> "प्रश्न और अपना लिखा हुआ उत्तर यहाँ दर्ज करें"
                                    else -> "कोई भी कठिन टॉपिक या प्रश्न लिखें (जैसे: तिनकठिया प्रणाली क्या थी?)"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    if (selectedTask == "EVALUATE_SUBJECTIVE") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = referenceRubricText,
                            onValueChange = { referenceRubricText = it },
                            label = { Text("आदर्श उत्तर / मुख्य बिंदु (वैकल्पिक)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (userInputText.isNotBlank()) {
                                viewModel.askAiTutor(
                                    taskType = selectedTask,
                                    subjectHi = selectedSubjectHi,
                                    userQuery = userInputText,
                                    referenceAnswer = referenceRubricText
                                )
                            }
                        },
                        enabled = !aiState.isLoading && userInputText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("AI से स्पष्टीकरण प्राप्त करें")
                    }

                    if (aiState.isLoading) {
                        Spacer(modifier = Modifier.height(10.dp))
                        CircularProgressIndicator()
                    }

                    if (!aiState.errorMessageHi.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = aiState.errorMessageHi.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    if (aiState.lastResponseHi.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "AI शिक्षक का उत्तर:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = aiState.lastResponseHi,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // Generate Additional AI Practice Questions (Clearly Tagged as AI_GENERATED)
        if (chapters.isNotEmpty()) {
            item {
                val safeIndex = selectedChapterIndex.coerceIn(0, chapters.lastIndex)
                val targetCh = chapters[safeIndex]

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "अतिरिक्त AI अभ्यास प्रश्न जनरेटर (स्पष्ट 'AI_GENERATED' लेबल के साथ)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "चयनित अध्याय: ${targetCh.textbook} — ${targetCh.titleHi}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    selectedChapterIndex = (safeIndex - 1 + chapters.size) % chapters.size
                                }
                            ) {
                                Text("← पिछला अध्याय")
                            }
                            OutlinedButton(
                                onClick = {
                                    selectedChapterIndex = (safeIndex + 1) % chapters.size
                                }
                            ) {
                                Text("अगला अध्याय →")
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.generateAiQuestionsForChapter(targetCh, count = 3)
                            },
                            enabled = !aiState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("इस अध्याय के लिए 3 नए AI अभ्यास प्रश्न बनाएं")
                        }
                        if (!aiState.generatedCountMessageHi.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = aiState.generatedCountMessageHi.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
}
