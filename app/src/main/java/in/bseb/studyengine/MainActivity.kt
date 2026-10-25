package `in`.bseb.studyengine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.bseb.studyengine.ui.navigation.AppDestination
import `in`.bseb.studyengine.ui.screens.AiStudyAssistantScreen
import `in`.bseb.studyengine.ui.screens.BookmarksScreen
import `in`.bseb.studyengine.ui.screens.HomeScreen
import `in`.bseb.studyengine.ui.screens.MistakeNotebookScreen
import `in`.bseb.studyengine.ui.screens.PracticeAndTestConfigScreen
import `in`.bseb.studyengine.ui.screens.ProgressAnalyticsScreen
import `in`.bseb.studyengine.ui.screens.QuizActiveScreen
import `in`.bseb.studyengine.ui.screens.QuizResultScreen
import `in`.bseb.studyengine.ui.screens.SettingsAndBackupScreen
import `in`.bseb.studyengine.ui.screens.SubjectsAndQuestionBankScreen
import `in`.bseb.studyengine.ui.theme.BsebStudyEngineTheme
import `in`.bseb.studyengine.ui.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as BsebStudyApplication

        setContent {
            val studyViewModel: StudyViewModel = viewModel(
                factory = StudyViewModel.Factory(app.repository, app.preferencesRepository)
            )
            val settings by studyViewModel.settingsState.collectAsState()

            BsebStudyEngineTheme(themeMode = settings.darkThemeMode) {
                BsebStudyEngineRootApp(viewModel = studyViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BsebStudyEngineRootApp(viewModel: StudyViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    val quizSession by viewModel.quizSession.collectAsState()
    val statusBanner by viewModel.statusBannerHi.collectAsState()

    var currentRoute by rememberSaveable { mutableStateOf(AppDestination.HOME.route) }
    var preselectedSubjectCode by rememberSaveable { mutableStateOf<String?>(null) }
    var preselectedChapterId by rememberSaveable { mutableStateOf<String?>(null) }

    // Automatically route to Active Quiz or Quiz Result when quiz session transitions
    val effectiveDestination = when {
        quizSession.isActive -> AppDestination.ACTIVE_QUIZ
        quizSession.latestCompletedSummary != null && currentRoute == AppDestination.ACTIVE_QUIZ.route -> AppDestination.QUIZ_RESULT
        else -> AppDestination.entries.firstOrNull { it.route == currentRoute } ?: AppDestination.HOME
    }

    val bottomDestinations = AppDestination.entries.filter { it.showInBottomBar }

    Scaffold(
        topBar = {
            if (effectiveDestination != AppDestination.ACTIVE_QUIZ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (settings.showEnglishTerms) {
                                    "${effectiveDestination.titleHi} (${effectiveDestination.titleEn})"
                                } else {
                                    effectiveDestination.titleHi
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "बिहार बोर्ड कक्षा 10 • पूर्णतः ऑफलाइन अध्ययन इंजन",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { currentRoute = AppDestination.BOOKMARKS.route }) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = "बुकमार्क (Bookmarks)",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(onClick = { currentRoute = AppDestination.AI_ASSISTANT.route }) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "AI अध्ययन सहायक",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(onClick = { currentRoute = AppDestination.SETTINGS.route }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "सेटिंग्स व बैकअप",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        },
        bottomBar = {
            if (effectiveDestination != AppDestination.ACTIVE_QUIZ) {
                NavigationBar {
                    bottomDestinations.forEach { dest ->
                        val selected = effectiveDestination == dest
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (quizSession.latestCompletedSummary != null) {
                                    viewModel.clearQuizSession()
                                }
                                currentRoute = dest.route
                            },
                            icon = { Icon(imageVector = dest.icon, contentDescription = dest.titleHi) },
                            label = {
                                Text(
                                    text = dest.titleHi,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!statusBanner.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusBanner.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.dismissBanner() }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "बंद करें")
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (effectiveDestination) {
                    AppDestination.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToSubjects = { subjectCode ->
                            preselectedSubjectCode = subjectCode
                            currentRoute = AppDestination.SUBJECTS.route
                        },
                        onNavigateToTests = { subjectCode, chapterId ->
                            preselectedSubjectCode = subjectCode
                            preselectedChapterId = chapterId
                            currentRoute = AppDestination.TESTS.route
                        },
                        onNavigateToMistakes = { currentRoute = AppDestination.MISTAKES.route },
                        onNavigateToBookmarks = { currentRoute = AppDestination.BOOKMARKS.route },
                        onNavigateToProgress = { currentRoute = AppDestination.PROGRESS.route },
                        onNavigateToAi = { currentRoute = AppDestination.AI_ASSISTANT.route }
                    )
                    AppDestination.SUBJECTS -> SubjectsAndQuestionBankScreen(
                        viewModel = viewModel,
                        initialSubjectCode = preselectedSubjectCode,
                        onStartChapterQuiz = { subjectCode, chapterId ->
                            preselectedSubjectCode = subjectCode
                            preselectedChapterId = chapterId
                            currentRoute = AppDestination.TESTS.route
                        }
                    )
                    AppDestination.TESTS -> PracticeAndTestConfigScreen(
                        viewModel = viewModel,
                        initialSubjectCode = preselectedSubjectCode,
                        initialChapterId = preselectedChapterId,
                        onQuizStarted = {
                            currentRoute = AppDestination.ACTIVE_QUIZ.route
                        }
                    )
                    AppDestination.MISTAKES -> MistakeNotebookScreen(
                        viewModel = viewModel,
                        onPracticeMistakes = {
                            currentRoute = AppDestination.ACTIVE_QUIZ.route
                        }
                    )
                    AppDestination.BOOKMARKS -> BookmarksScreen(
                        viewModel = viewModel,
                        onPracticeBookmarks = {
                            currentRoute = AppDestination.ACTIVE_QUIZ.route
                        }
                    )
                    AppDestination.PROGRESS -> ProgressAnalyticsScreen(
                        viewModel = viewModel,
                        onReviseWeakChapter = { chapterId ->
                            preselectedSubjectCode = null
                            preselectedChapterId = chapterId
                            currentRoute = AppDestination.TESTS.route
                        }
                    )
                    AppDestination.AI_ASSISTANT -> AiStudyAssistantScreen(
                        viewModel = viewModel,
                        onOpenSettings = { currentRoute = AppDestination.SETTINGS.route }
                    )
                    AppDestination.SETTINGS -> SettingsAndBackupScreen(
                        viewModel = viewModel
                    )
                    AppDestination.ACTIVE_QUIZ -> QuizActiveScreen(
                        viewModel = viewModel,
                        onQuizSubmitted = {
                            currentRoute = AppDestination.QUIZ_RESULT.route
                        },
                        onExitQuiz = {
                            viewModel.clearQuizSession()
                            currentRoute = AppDestination.HOME.route
                        }
                    )
                    AppDestination.QUIZ_RESULT -> QuizResultScreen(
                        viewModel = viewModel,
                        onDone = {
                            viewModel.clearQuizSession()
                            currentRoute = AppDestination.HOME.route
                        },
                        onRetakeMistakes = {
                            currentRoute = AppDestination.ACTIVE_QUIZ.route
                        }
                    )
                }
            }
        }
    }
}
