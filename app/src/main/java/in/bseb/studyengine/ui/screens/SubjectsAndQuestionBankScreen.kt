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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.bseb.studyengine.data.backup.BackupRestoreSerializer
import `in`.bseb.studyengine.data.local.ChapterEntity
import `in`.bseb.studyengine.data.local.QuestionEntity
import `in`.bseb.studyengine.domain.model.DifficultyLevel
import `in`.bseb.studyengine.domain.model.QuestionFormat
import `in`.bseb.studyengine.domain.model.SubjectType
import `in`.bseb.studyengine.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectsAndQuestionBankScreen(
    viewModel: StudyViewModel,
    initialSubjectCode: String?,
    onStartChapterQuiz: (String, String) -> Unit
) {
    val chapters by viewModel.chapters.collectAsState()
    val allQuestions by viewModel.allQuestions.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.questionId }.toSet() }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) } // 0 = अध्याय सूची, 1 = प्रश्न बैंक खोजें
    var selectedSubject by rememberSaveable {
        mutableStateOf(initialSubjectCode ?: SubjectType.HINDI.code)
    }
    var selectedSubSubject by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFormatFilter by rememberSaveable { mutableStateOf<String?>(null) }

    var showAddQuestionDialogForChapter by remember { mutableStateOf<ChapterEntity?>(null) }
    var showReportDialogForQuestion by remember { mutableStateOf<QuestionEntity?>(null) }

    val subjectChapters = remember(chapters, selectedSubject, selectedSubSubject) {
        chapters.filter {
            it.subject == selectedSubject &&
                (selectedSubSubject == null || it.subSubject == selectedSubSubject)
        }
    }

    val availableSubSubjects = remember(chapters, selectedSubject) {
        chapters.filter { it.subject == selectedSubject }
            .map { it.subSubject }
            .distinct()
    }

    val questionCountByChapter = remember(allQuestions) {
        allQuestions.groupingBy { it.chapterId }.eachCount()
    }

    val filteredQuestions = remember(allQuestions, selectedSubject, selectedSubSubject, searchQuery, selectedFormatFilter) {
        allQuestions.filter { q ->
            val matchSub = q.subject == selectedSubject
            val matchSubBranch = selectedSubSubject == null || q.subSubject == selectedSubSubject
            val matchFormat = selectedFormatFilter == null || q.questionType == selectedFormatFilter
            val matchQuery = searchQuery.isBlank() ||
                q.questionText.contains(searchQuery, ignoreCase = true) ||
                q.topic.contains(searchQuery, ignoreCase = true) ||
                q.explanation.contains(searchQuery, ignoreCase = true)
            matchSub && matchSubBranch && matchFormat && matchQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("पाठ्यक्रम व अध्याय (${subjectChapters.size})") }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("प्रश्न बैंक खोज (${filteredQuestions.size})") }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Subject Selector Chips
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubjectType.entries.forEach { sub ->
                        FilterChip(
                            selected = selectedSubject == sub.code,
                            onClick = {
                                selectedSubject = sub.code
                                selectedSubSubject = null
                            },
                            label = { Text(sub.titleHi) }
                        )
                    }
                }
            }

            // Sub-Subject (Branch) Filter Chips (Especially for Social Science: History, Geography, Civics, Economics, Disaster Mgmt)
            if (availableSubSubjects.size > 1) {
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedSubSubject == null,
                            onClick = { selectedSubSubject = null },
                            label = { Text("सभी खंड (${availableSubSubjects.size})") }
                        )
                        availableSubSubjects.forEach { branch ->
                            FilterChip(
                                selected = selectedSubSubject == branch,
                                onClick = { selectedSubSubject = branch },
                                label = { Text(branch) }
                            )
                        }
                    }
                }
            }

            if (selectedTabIndex == 0) {
                // CHAPTERS LIST
                items(subjectChapters, key = { it.chapterId }) { ch ->
                    val qCount = questionCountByChapter[ch.chapterId] ?: 0
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${ch.textbook} • ${ch.subSubject}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            if (qCount > 0) "$qCount प्रश्न उपलब्ध" else "पाठ्यक्रम सूचीबद्ध (0 प्रश्न)"
                                        )
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "अध्याय ${ch.chapterNumber}: ${ch.titleHi}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (ch.titleEn.isNotBlank()) {
                                Text(
                                    text = ch.titleEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onStartChapterQuiz(ch.subject, ch.chapterId) },
                                    enabled = qCount > 0,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (qCount > 0) "अध्याय अभ्यास / टेस्ट" else "पहले प्रश्न जोड़ें")
                                }
                                OutlinedButton(
                                    onClick = { showAddQuestionDialogForChapter = ch }
                                ) {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("प्रश्न जोड़ें")
                                }
                            }
                        }
                    }
                }
            } else {
                // QUESTION BANK SEARCH & FILTER
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        label = { Text("प्रश्न, टॉपिक या व्याख्या खोजें (Search in Hindi/Sanskrit)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFormatFilter == null,
                            onClick = { selectedFormatFilter = null },
                            label = { Text("सभी प्रारूप") }
                        )
                        QuestionFormat.entries.forEach { fmt ->
                            FilterChip(
                                selected = selectedFormatFilter == fmt.code,
                                onClick = { selectedFormatFilter = fmt.code },
                                label = { Text(fmt.labelHi) }
                            )
                        }
                    }
                }

                if (filteredQuestions.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "इस खोज/फ़िल्टर के लिए कोई प्रश्न नहीं मिला।",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "आप 'पाठ्यक्रम व अध्याय' टैब से किसी भी अध्याय में नया अभ्यास प्रश्न जोड़ सकते हैं।",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    items(filteredQuestions, key = { it.questionId }) { q ->
                        val isBookmarked = bookmarkedIds.contains(q.questionId)
                        var showAnswerInline by rememberSaveable(q.questionId) { mutableStateOf(false) }
                        val opts = remember(q.optionsJson) { BackupRestoreSerializer.parseOptionsJson(q.optionsJson) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${q.textbook} • ${QuestionFormat.fromCode(q.questionType).labelHi} • ${DifficultyLevel.fromCode(q.difficulty).labelHi}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row {
                                        IconButton(onClick = { viewModel.toggleBookmark(q.questionId) }) {
                                            Icon(
                                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                                contentDescription = "बुकमार्क",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { showReportDialogForQuestion = q }) {
                                            Icon(
                                                imageVector = Icons.Filled.Flag,
                                                contentDescription = "त्रुटि रिपोर्ट करें",
                                                tint = if (q.isReportedQuestionable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = q.questionText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (opts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    opts.forEachIndexed { idx, opt ->
                                        Text(
                                            text = "${('A' + idx)}. $opt",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "स्रोत: ${q.sourceDetail} [${q.verificationStatus}]",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (q.isReportedQuestionable) {
                                    Text(
                                        text = "⚠ पुनर्निरीक्षण हेतु चिह्नित: ${q.reportReason}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(onClick = { showAnswerInline = !showAnswerInline }) {
                                    Text(if (showAnswerInline) "उत्तर व व्याख्या छुपाएं" else "उत्तर व व्याख्या देखें")
                                }

                                if (showAnswerInline) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "सही उत्तर: ${q.correctAnswer}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                    if (q.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
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
        }
    }

    // Dialog to Add a Custom Question to a Chapter
    showAddQuestionDialogForChapter?.let { targetChapter ->
        var qText by remember { mutableStateOf("") }
        var optA by remember { mutableStateOf("") }
        var optB by remember { mutableStateOf("") }
        var optC by remember { mutableStateOf("") }
        var optD by remember { mutableStateOf("") }
        var correctAns by remember { mutableStateOf("") }
        var explanation by remember { mutableStateOf("") }
        var qType by remember { mutableStateOf("MCQ") }

        AlertDialog(
            onDismissRequest = { showAddQuestionDialogForChapter = null },
            title = { Text("नया प्रश्न जोड़ें: ${targetChapter.titleHi}") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = qType == "MCQ",
                                onClick = { qType = "MCQ" },
                                label = { Text("MCQ") }
                            )
                            FilterChip(
                                selected = qType == "SHORT_ANSWER",
                                onClick = { qType = "SHORT_ANSWER" },
                                label = { Text("लघु उत्तरीय") }
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = qText,
                            onValueChange = { qText = it },
                            label = { Text("प्रश्न लिखें (हिंदी / संस्कृत)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (qType == "MCQ") {
                        item {
                            OutlinedTextField(
                                value = optA,
                                onValueChange = { optA = it },
                                label = { Text("विकल्प A") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = optB,
                                onValueChange = { optB = it },
                                label = { Text("विकल्प B") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = optC,
                                onValueChange = { optC = it },
                                label = { Text("विकल्प C") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = optD,
                                onValueChange = { optD = it },
                                label = { Text("विकल्प D") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = correctAns,
                            onValueChange = { correctAns = it },
                            label = { Text("सही उत्तर (विकल्प से हूबहू मिलता हुआ या आदर्श उत्तर)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = explanation,
                            onValueChange = { explanation = it },
                            label = { Text("व्याख्या / मुख्य बिंदु") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (qText.isNotBlank() && correctAns.isNotBlank()) {
                            val opts = if (qType == "MCQ") {
                                listOf(optA, optB, optC, optD).filter { it.isNotBlank() }
                            } else {
                                emptyList()
                            }
                            viewModel.addUserCustomQuestion(
                                chapter = targetChapter,
                                topic = targetChapter.titleHi,
                                questionType = qType,
                                difficulty = "MEDIUM",
                                questionText = qText,
                                options = opts,
                                correctAnswer = correctAns,
                                explanation = explanation,
                                marks = if (qType == "MCQ") 1 else 2,
                                sourceDetail = "छात्र द्वारा पाठ्यपुस्तक नोट्स से जोड़ा गया"
                            )
                            showAddQuestionDialogForChapter = null
                        }
                    }
                ) {
                    Text("सहेजें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddQuestionDialogForChapter = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Dialog to Report Questionable Content
    showReportDialogForQuestion?.let { qToReport ->
        var reason by remember { mutableStateOf(qToReport.reportReason) }
        AlertDialog(
            onDismissRequest = { showReportDialogForQuestion = null },
            title = { Text("प्रश्न में संदिग्ध सामग्री या त्रुटि चिह्नित करें") },
            text = {
                Column {
                    Text(
                        text = qToReport.questionText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("त्रुटि या संदेह का कारण लिखें") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.reportQuestion(qToReport.questionId, reason)
                        showReportDialogForQuestion = null
                    }
                ) {
                    Text("चिह्नित करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialogForQuestion = null }) {
                    Text("बंद करें")
                }
            }
        )
    }
}
