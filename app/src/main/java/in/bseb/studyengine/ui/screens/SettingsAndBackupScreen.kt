package `in`.bseb.studyengine.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.bseb.studyengine.ui.viewmodel.StudyViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndBackupScreen(viewModel: StudyViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settingsState.collectAsState()

    var nameInput by rememberSaveable(settings.studentNameHi) { mutableStateOf(settings.studentNameHi) }
    var goalInput by rememberSaveable(settings.dailyQuestionGoal) { mutableStateOf(settings.dailyQuestionGoal.toString()) }
    var apiKeyInput by rememberSaveable(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var aiConsent by rememberSaveable(settings.aiConsentAccepted) { mutableStateOf(settings.aiConsentAccepted) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // Android Storage Access Framework (SAF) Launchers — Zero storage permissions required!
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackupToCallback { jsonContent ->
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonContent.toByteArray(Charsets.UTF_8))
                    }
                    viewModel.showStatusMessage("बैकअप फ़ाइल आपके फ़ोन में सफलतापूर्वक सहेज ली गई!")
                } catch (e: Exception) {
                    viewModel.showStatusMessage("फ़ाइल लिखने में त्रुटि: ${e.localizedMessage}")
                }
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    viewModel.restoreBackupFromJson(content)
                }
            } catch (e: Exception) {
                viewModel.showStatusMessage("फ़ाइल पढ़ने में त्रुटि: ${e.localizedMessage}")
            }
        }
    }

    val importQuestionPackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)
                    ?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    viewModel.importQuestionsFromJsonText(content)
                }
            } catch (e: Exception) {
                viewModel.showStatusMessage("प्रश्न बैंक फ़ाइल पढ़ने में त्रुटि: ${e.localizedMessage}")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Theme & Language Preferences
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("1. दिखावट एवं भाषा (Theme & Language)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("थीम चुनें (Light / Dark Mode):")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SYSTEM" to "सिस्टम डिफ़ॉल्ट", "LIGHT" to "लाइट मोड (Light)", "DARK" to "डार्क मोड (Dark)").forEach { (code, label) ->
                            FilterChip(
                                selected = settings.darkThemeMode == code,
                                onClick = { viewModel.updateThemeMode(code) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("हिंदी के साथ तकनीकी अंग्रेजी शब्द (Bilingual Labels) दिखाएं", modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.showEnglishTerms,
                            onCheckedChange = { viewModel.updateShowEnglishTerms(it) }
                        )
                    }
                }
            }
        }

        // 2. Student Profile & Daily Study Goal
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("2. छात्र प्रोफ़ाइल एवं दैनिक लक्ष्य", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("आपका नाम (हिंदी में)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("दैनिक प्रश्न अभ्यास लक्ष्य (जैसे: 20 या 50 प्रश्न)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.updateProfileAndGoal(
                                nameHi = nameInput,
                                dailyGoal = goalInput.toIntOrNull() ?: 20
                            )
                        }
                    ) {
                        Text("लक्ष्य सहेजें")
                    }
                }
            }
        }

        // 3. Backup, Restore & Curriculum JSON Import
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("3. डेटा बैकअप, रीस्टोर एवं प्रश्न-बैंक आयात (100% स्थानीय नियंत्रण)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "आपका सारा अध्ययन डेटा आपके Vivo T4R फ़ोन में सुरक्षित रहता है। फ़ोन बदलने या रीसेट करने से पहले यहाँ से JSON बैकअप फ़ाइल अपने फ़ोन स्टोरेज में सेव कर लें।",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { exportBackupLauncher.launch("bseb_study_backup_${System.currentTimeMillis()}.json") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("पूरा डेटा बैकअप फ़ाइल में निर्यात (Export Backup) करें")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("बैकअप फ़ाइल से डेटा रीस्टोर (Restore Backup) करें")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { importQuestionPackLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("नया अध्याय/प्रश्न-बैंक JSON पैक इम्पोर्ट करें")
                    }
                }
            }
        }

        // 4. Optional Online AI Configuration
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("4. वैकल्पिक ऑनलाइन AI सहायक सेटिंग्स (₹0 Free Key)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "सुरक्षा नियम के अनुसार APK में कोई भी प्राइवेट API Key हार्डकोड नहीं की गई है। यदि आप ऑनलाइन AI फ़ीचर चाहते हैं, तो aistudio.google.com से अपनी निःशुल्क Gemini API Key यहाँ डालें।",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("आपकी वैकल्पिक Gemini API Key (केवल इस फ़ोन पर सुरक्षित)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "मैं समझता/समझती हूँ कि AI सहायक का उपयोग करने पर केवल मेरा पूछा गया प्रश्न ऑनलाइन AI सेवा को भेजा जाएगा।",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(checked = aiConsent, onCheckedChange = { aiConsent = it })
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.updateGeminiSettings(apiKeyInput, aiConsent) }) {
                        Text("AI सेटिंग्स सहेजें")
                    }
                }
            }
        }

        // 5. Clear Personal Study History
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("5. अध्ययन इतिहास रीसेट करें (Clear Study Data)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "इससे आपके द्वारा दिए गए टेस्ट, बुकमार्क और गलती पुस्तिका साफ़ हो जाएँगे, लेकिन ऐप का मूल बिहार बोर्ड पाठ्यक्रम और सत्यापित प्रश्न सुरक्षित रहेंगे।",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("व्यक्तिगत प्रगति डेटा साफ़ करें")
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("क्या आप वाकई डेटा रीसेट करना चाहते हैं?") },
            text = { Text("सभी टेस्ट स्कोर, बुकमार्क और गलती पुस्तिका के रिकॉर्ड हटा दिए जाएँगे। (मूल 75 अध्याय और 72 पाठ्यक्रम प्रश्न सुरक्षित रहेंगे।)") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm = false
                        viewModel.clearAllUserProgress()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("हाँ, साफ़ करें")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
