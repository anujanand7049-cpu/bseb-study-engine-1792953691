package `in`.bseb.studyengine.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.bseb.studyengine.domain.model.QuizConfig
import `in`.bseb.studyengine.domain.model.QuizMode
import `in`.bseb.studyengine.domain.model.SubjectType
import `in`.bseb.studyengine.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PracticeAndTestConfigScreen(
    viewModel: StudyViewModel,
    initialSubjectCode: String?,
    initialChapterId: String?,
    onQuizStarted: () -> Unit
) {
    val chapters by viewModel.chapters.collectAsState()
    val savedPresets by viewModel.savedPresets.collectAsState()
    val settings by viewModel.settingsState.collectAsState()

    var selectedSubject by rememberSaveable { mutableStateOf(initialSubjectCode) }
    var selectedSubSubject by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedChapterId by rememberSaveable { mutableStateOf(initialChapterId) }
    var selectedModeCode by rememberSaveable { mutableStateOf(QuizMode.EXAM_TIMED.code) }
    var questionCount by rememberSaveable { mutableIntStateOf(settings.defaultQuizQuestionCount) }
    var durationMinutes by rememberSaveable { mutableIntStateOf(settings.defaultQuizDurationMinutes) }
    var negativeMarking by rememberSaveable { mutableDoubleStateOf(settings.defaultNegativeMarking) }
    var objectiveOnly by rememberSaveable { mutableStateOf(true) }
    var subjectiveOnly by rememberSaveable { mutableStateOf(false) }
    var shuffleQuestions by rememberSaveable { mutableStateOf(true) }
    var shuffleOptions by rememberSaveable { mutableStateOf(true) }
    var customPresetName by rememberSaveable { mutableStateOf("") }

    val subSubjectsForSubject = remember(chapters, selectedSubject) {
        if (selectedSubject == null) emptyList()
        else chapters.filter { it.subject == selectedSubject }.map { it.subSubject }.distinct()
    }

    val chaptersForFilter = remember(chapters, selectedSubject, selectedSubSubject) {
        chapters.filter { ch ->
            ch.isPopulated &&
                (selectedSubject == null || ch.subject == selectedSubject) &&
                (selectedSubSubject == null || ch.subSubject == selectedSubSubject)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "कस्टम टेस्ट एवं अभ्यास कॉन्फ़िगरेशन",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• परीक्षा मोड (Exam Mode) में टाइमर समाप्त होने से पहले उत्तर नहीं दिखाए जाते और समय पूरा होने पर टेस्ट स्वतः सबमिट हो जाता है।
• अभ्यास मोड (Practice Mode) में आप प्रत्येक प्रश्न के तुरंत बाद सही उत्तर और व्याख्या देख सकते हैं।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // 1. Mode Selection
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. मोड चुनें (Select Mode)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    QuizMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedModeCode == mode.code,
                            onClick = { selectedModeCode = mode.code },
                            label = { Text(mode.labelHi) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = mode.descriptionHi,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }

        // 2. Subject, Branch & Chapter Filter
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. विषय एवं अध्याय चुनें", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSubject == null,
                            onClick = {
                                selectedSubject = null
                                selectedSubSubject = null
                                selectedChapterId = null
                            },
                            label = { Text("मिश्रित (सभी विषय)") }
                        )
                        SubjectType.entries.forEach { sub ->
                            FilterChip(
                                selected = selectedSubject == sub.code,
                                onClick = {
                                    selectedSubject = sub.code
                                    selectedSubSubject = null
                                    selectedChapterId = null
                                },
                                label = { Text(sub.titleHi) }
                            )
                        }
                    }

                    if (subSubjectsForSubject.size > 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("उप-विषय / खंड चुनें:", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedSubSubject == null,
                                onClick = {
                                    selectedSubSubject = null
                                    selectedChapterId = null
                                },
                                label = { Text("सभी खंड") }
                            )
                            subSubjectsForSubject.forEach { branch ->
                                FilterChip(
                                    selected = selectedSubSubject == branch,
                                    onClick = {
                                        selectedSubSubject = branch
                                        selectedChapterId = null
                                    },
                                    label = { Text(branch) }
                                )
                            }
                        }
                    }

                    if (chaptersForFilter.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("अध्याय चुनें (वैकल्पिक):", style = MaterialTheme.typography.labelLarge)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedChapterId == null,
                                onClick = { selectedChapterId = null },
                                label = { Text("सभी उपलब्ध अध्याय (${chaptersForFilter.size})") }
                            )
                            chaptersForFilter.forEach { ch ->
                                FilterChip(
                                    selected = selectedChapterId == ch.chapterId,
                                    onClick = { selectedChapterId = ch.chapterId },
                                    label = { Text("${ch.chapterNumber}. ${ch.titleHi.take(24)}") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Question Count, Duration, Negative Marking & Question Type
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("3. प्रश्नों की संख्या, समय और नेगेटिव मार्किंग", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("प्रश्नों की संख्या: $questionCount प्रश्न", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 20, 30, 50).forEach { count ->
                            FilterChip(
                                selected = questionCount == count,
                                onClick = { questionCount = count },
                                label = { Text("$count") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("समय सीमा (Timer): $durationMinutes मिनट", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 20, 30, 60).forEach { mins ->
                            FilterChip(
                                selected = durationMinutes == mins,
                                onClick = { durationMinutes = mins },
                                label = { Text("$mins मिनट") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("नेगेटिव मार्किंग (गलत उत्तर पर कटौती): -$negativeMarking अंक", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.0 to "कोई नहीं (BSEB पैटर्न: 0)", 0.25 to "-0.25 अंक", 0.33 to "-0.33 अंक").forEach { (neg, label) ->
                            FilterChip(
                                selected = negativeMarking == neg,
                                onClick = { negativeMarking = neg },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("प्रश्न प्रकार फ़िल्टर:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = objectiveOnly && !subjectiveOnly,
                            onClick = {
                                objectiveOnly = true
                                subjectiveOnly = false
                            },
                            label = { Text("केवल वस्तुनिष्ठ (Objective)") }
                        )
                        FilterChip(
                            selected = subjectiveOnly && !objectiveOnly,
                            onClick = {
                                subjectiveOnly = true
                                objectiveOnly = false
                            },
                            label = { Text("केवल लघु/दीर्घ उत्तरीय (Subjective)") }
                        )
                        FilterChip(
                            selected = !objectiveOnly && !subjectiveOnly,
                            onClick = {
                                objectiveOnly = false
                                subjectiveOnly = false
                            },
                            label = { Text("दोनों (Objective + Subjective)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("प्रश्नों और विकल्पों का क्रम बदलें (Shuffle)")
                        Switch(
                            checked = shuffleQuestions,
                            onCheckedChange = {
                                shuffleQuestions = it
                                shuffleOptions = it
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val mode = if (selectedModeCode == QuizMode.PRACTICE_INSTANT.code) {
                                QuizMode.PRACTICE_INSTANT
                            } else {
                                QuizMode.EXAM_TIMED
                            }
                            val title = buildString {
                                append(selectedSubject?.let { SubjectType.fromCode(it).titleHi } ?: "मिश्रित विषय")
                                if (selectedSubSubject != null) append(" ($selectedSubSubject)")
                                append(" — $questionCount प्रश्न")
                            }
                            val started = viewModel.startQuiz(
                                QuizConfig(
                                    titleHi = title,
                                    subjectFilter = selectedSubject,
                                    subSubjectFilter = selectedSubSubject,
                                    chapterIdFilter = selectedChapterId,
                                    includeObjectiveOnly = objectiveOnly,
                                    includeSubjectiveOnly = subjectiveOnly,
                                    questionCount = questionCount,
                                    durationMinutes = durationMinutes,
                                    marksPerQuestion = 1.0,
                                    negativeMarkingPerWrong = negativeMarking,
                                    shuffleQuestions = shuffleQuestions,
                                    shuffleOptions = shuffleOptions,
                                    quizMode = mode
                                )
                            )
                            if (started) onQuizStarted()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("अब टेस्ट / अभ्यास शुरू करें", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customPresetName,
                            onValueChange = { customPresetName = it },
                            label = { Text("टेस्ट टेम्पलेट का नाम (Save Test)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedButton(
                            onClick = {
                                viewModel.savePreset(
                                    titleHi = customPresetName.ifBlank {
                                        "${selectedSubject ?: "मिश्रित"} ($questionCount प्रश्न)"
                                    },
                                    subjectFilter = selectedSubject,
                                    subSubjectFilter = selectedSubSubject,
                                    chapterIdFilter = selectedChapterId,
                                    questionCount = questionCount,
                                    durationMinutes = durationMinutes,
                                    negativeMarking = negativeMarking
                                )
                                customPresetName = ""
                            }
                        ) {
                            Icon(imageVector = Icons.Filled.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("सहेजें")
                        }
                    }
                }
            }
        }

        // 4. Saved Test Presets
        if (savedPresets.isNotEmpty()) {
            item {
                Text("सहेजे गए टेस्ट टेम्पलेट (Saved Tests)", style = MaterialTheme.typography.titleLarge)
            }
            items(savedPresets, key = { it.presetId }) { preset ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preset.titleHi, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${preset.questionCount} प्रश्न • ${preset.durationMinutes} मिनट • नेगेटिव: -${preset.negativeMarking}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row {
                            Button(
                                onClick = {
                                    val started = viewModel.startQuiz(
                                        QuizConfig(
                                            titleHi = preset.titleHi,
                                            subjectFilter = preset.subjectFilter,
                                            subSubjectFilter = preset.subSubjectFilter,
                                            chapterIdFilter = preset.chapterIdFilter,
                                            questionCount = preset.questionCount,
                                            durationMinutes = preset.durationMinutes,
                                            negativeMarkingPerWrong = preset.negativeMarking,
                                            quizMode = QuizMode.EXAM_TIMED
                                        )
                                    )
                                    if (started) onQuizStarted()
                                }
                            ) {
                                Text("शुरू करें")
                            }
                            IconButton(onClick = { viewModel.deletePreset(preset.presetId) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "हटाएं")
                            }
                        }
                    }
                }
            }
        }
    }
}
