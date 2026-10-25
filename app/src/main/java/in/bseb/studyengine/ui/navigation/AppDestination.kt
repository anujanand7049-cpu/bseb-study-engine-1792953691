package `in`.bseb.studyengine.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val route: String,
    val titleHi: String,
    val titleEn: String,
    val icon: ImageVector,
    val showInBottomBar: Boolean
) {
    HOME("home", "मुख्य पृष्ठ", "Home", Icons.Filled.Home, true),
    SUBJECTS("subjects", "विषय व अध्याय", "Subjects", Icons.Filled.MenuBook, true),
    TESTS("tests", "अभ्यास व टेस्ट", "Tests", Icons.Filled.Quiz, true),
    MISTAKES("mistakes", "गलती पुस्तिका", "Mistakes", Icons.Filled.ErrorOutline, true),
    PROGRESS("progress", "प्रगति", "Progress", Icons.Filled.Analytics, true),
    BOOKMARKS("bookmarks", "बुकमार्क", "Bookmarks", Icons.Filled.Bookmark, false),
    AI_ASSISTANT("ai_assistant", "AI अध्ययन सहायक", "AI Tutor", Icons.Filled.AutoAwesome, false),
    SETTINGS("settings", "सेटिंग्स व बैकअप", "Settings", Icons.Filled.Settings, false),
    ACTIVE_QUIZ("active_quiz", "सक्रिय परीक्षा", "Active Quiz", Icons.Filled.Quiz, false),
    QUIZ_RESULT("quiz_result", "परीक्षा परिणाम", "Quiz Result", Icons.Filled.Analytics, false)
}
