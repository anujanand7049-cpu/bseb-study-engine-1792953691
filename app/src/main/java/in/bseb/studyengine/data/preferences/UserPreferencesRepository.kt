package `in`.bseb.studyengine.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bseb_user_settings")

data class UserSettingsState(
    val studentNameHi: String = "बिहार बोर्ड छात्र (कक्षा 10)",
    val darkThemeMode: String = "SYSTEM",        // SYSTEM, LIGHT, DARK
    val showEnglishTerms: Boolean = true,
    val dailyQuestionGoal: Int = 20,
    val defaultQuizQuestionCount: Int = 10,
    val defaultQuizDurationMinutes: Int = 15,
    val defaultNegativeMarking: Double = 0.0,
    val geminiApiKey: String = "",               // Stored strictly locally on user's phone, never hardcoded in APK
    val aiConsentAccepted: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val STUDENT_NAME = stringPreferencesKey("student_name_hi")
        val DARK_THEME_MODE = stringPreferencesKey("dark_theme_mode")
        val SHOW_ENGLISH_TERMS = booleanPreferencesKey("show_english_terms")
        val DAILY_QUESTION_GOAL = intPreferencesKey("daily_question_goal")
        val DEFAULT_QUIZ_COUNT = intPreferencesKey("default_quiz_count")
        val DEFAULT_QUIZ_DURATION = intPreferencesKey("default_quiz_duration")
        val DEFAULT_NEGATIVE_MARKING = doublePreferencesKey("default_negative_marking")
        val GEMINI_API_KEY = stringPreferencesKey("optional_gemini_api_key")
        val AI_CONSENT_ACCEPTED = booleanPreferencesKey("ai_online_consent_accepted")
    }

    val settingsFlow: Flow<UserSettingsState> = context.dataStore.data.map { prefs ->
        UserSettingsState(
            studentNameHi = prefs[Keys.STUDENT_NAME] ?: "बिहार बोर्ड छात्र (कक्षा 10)",
            darkThemeMode = prefs[Keys.DARK_THEME_MODE] ?: "SYSTEM",
            showEnglishTerms = prefs[Keys.SHOW_ENGLISH_TERMS] ?: true,
            dailyQuestionGoal = prefs[Keys.DAILY_QUESTION_GOAL] ?: 20,
            defaultQuizQuestionCount = prefs[Keys.DEFAULT_QUIZ_COUNT] ?: 10,
            defaultQuizDurationMinutes = prefs[Keys.DEFAULT_QUIZ_DURATION] ?: 15,
            defaultNegativeMarking = prefs[Keys.DEFAULT_NEGATIVE_MARKING] ?: 0.0,
            geminiApiKey = prefs[Keys.GEMINI_API_KEY] ?: "",
            aiConsentAccepted = prefs[Keys.AI_CONSENT_ACCEPTED] ?: false
        )
    }

    suspend fun updateStudentName(name: String) {
        context.dataStore.edit { it[Keys.STUDENT_NAME] = name.trim().ifEmpty { "बिहार बोर्ड छात्र" } }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.DARK_THEME_MODE] = mode }
    }

    suspend fun updateShowEnglishTerms(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_ENGLISH_TERMS] = enabled }
    }

    suspend fun updateDailyGoal(goal: Int) {
        context.dataStore.edit { it[Keys.DAILY_QUESTION_GOAL] = goal.coerceIn(5, 200) }
    }

    suspend fun updateDefaultQuizConfig(count: Int, durationMinutes: Int, negativeMarking: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_QUIZ_COUNT] = count.coerceIn(5, 100)
            prefs[Keys.DEFAULT_QUIZ_DURATION] = durationMinutes.coerceIn(5, 180)
            prefs[Keys.DEFAULT_NEGATIVE_MARKING] = negativeMarking.coerceIn(0.0, 1.0)
        }
    }

    suspend fun updateGeminiApiKeyAndConsent(apiKey: String, consentAccepted: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GEMINI_API_KEY] = apiKey.trim()
            prefs[Keys.AI_CONSENT_ACCEPTED] = consentAccepted
        }
    }
}
