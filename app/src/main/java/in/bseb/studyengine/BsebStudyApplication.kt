package `in`.bseb.studyengine

import android.app.Application
import `in`.bseb.studyengine.data.local.AppDatabase
import `in`.bseb.studyengine.data.preferences.UserPreferencesRepository
import `in`.bseb.studyengine.data.repository.StudyRepository

class BsebStudyApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: StudyRepository by lazy { StudyRepository(this, database.studyDao()) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
}
