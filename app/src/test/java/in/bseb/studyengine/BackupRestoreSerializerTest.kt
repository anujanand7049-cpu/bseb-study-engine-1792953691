package `in`.bseb.studyengine

import `in`.bseb.studyengine.data.backup.BackupRestoreSerializer
import `in`.bseb.studyengine.data.backup.FullBackupSnapshot
import `in`.bseb.studyengine.data.local.BookmarkEntity
import `in`.bseb.studyengine.data.local.MistakeRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupRestoreSerializerTest {

    @Test
    fun `serializeBackup and deserializeBackup preserve Devanagari data losslessly`() {
        val original = FullBackupSnapshot(
            schemaVersion = 1,
            exportedAtMillis = 1730000000000L,
            customQuestions = emptyList(),
            bookmarks = listOf(
                BookmarkEntity(
                    questionId = "Q_HIN_001",
                    noteHi = "बोर्ड परीक्षा के लिए अति महत्वपूर्ण",
                    createdAtEpochMillis = 1730000000100L
                )
            ),
            mistakes = listOf(
                MistakeRecordEntity(
                    questionId = "Q_SAN_001",
                    chapterId = "SAN_PIY_01",
                    subject = "SANSKRIT",
                    lastOutcome = "INCORRECT",
                    userLastAnswer = "कठोपनिषद्",
                    mistakeCount = 2,
                    resolved = false,
                    lastUpdatedEpochMillis = 1730000000200L
                )
            ),
            quizAttempts = emptyList(),
            attemptItems = emptyList()
        )

        val json = BackupRestoreSerializer.serializeBackup(original)
        val restored = BackupRestoreSerializer.deserializeBackup(json)

        assertEquals(1, restored.bookmarks.size)
        assertEquals("बोर्ड परीक्षा के लिए अति महत्वपूर्ण", restored.bookmarks.first().noteHi)
        assertEquals(1, restored.mistakes.size)
        assertEquals("कठोपनिषद्", restored.mistakes.first().userLastAnswer)
        assertEquals(2, restored.mistakes.first().mistakeCount)
    }
}
