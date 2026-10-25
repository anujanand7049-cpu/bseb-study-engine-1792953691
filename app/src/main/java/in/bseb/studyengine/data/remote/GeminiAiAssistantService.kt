package `in`.bseb.studyengine.data.remote

import `in`.bseb.studyengine.data.backup.BackupRestoreSerializer
import `in`.bseb.studyengine.data.local.QuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

sealed class AiAssistantResult {
    data class SuccessText(val responseHi: String) : AiAssistantResult()
    data class SuccessGeneratedQuestions(val questions: List<QuestionEntity>) : AiAssistantResult()
    data class Error(val messageHi: String) : AiAssistantResult()
}

/**
 * Optional AI Study Assistant powered by Google AI Studio (Gemini 2.5 Flash free tier).
 *
 * PRIVACY & SECURITY DESIGN:
 * 1. Zero hardcoded API keys in the APK. The student optionally enters their personal free key
 *    in Settings (stored locally in Android DataStore).
 * 2. Works 100% independently of the core offline app. If offline or no key is configured,
 *    built-in offline grammar tips and rubric checklists are provided immediately.
 * 3. Every AI-generated practice question is strictly tagged with:
 *    - sourceType = "AI_GENERATED"
 *    - verificationStatus = "AI_DRAFT"
 *    - sourceDetail = "AI अभ्यास प्रश्न (आधिकारिक बोर्ड प्रश्न नहीं)"
 */
object GeminiAiAssistantService {

    private const val ENDPOINT_BASE =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="

    suspend fun askStudyAssistant(
        apiKey: String,
        taskType: String, // EXPLAIN_CONCEPT, SANSKRIT_GRAMMAR, EVALUATE_SUBJECTIVE
        subjectHi: String,
        userInput: String,
        referenceAnswer: String = ""
    ): AiAssistantResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AiAssistantResult.Error(
                "ऑनलाइन AI सहायक के लिए 'सेटिंग्स' में अपनी निःशुल्क Google AI Studio (Gemini) API Key दर्ज करें। (बिना इंटरनेट/Key के नीचे दिए गए ऑफलाइन व्याकरण व उत्तर-लेखन सूत्र देखें।)"
            )
        }

        val systemInstruction = when (taskType) {
            "SANSKRIT_GRAMMAR" ->
                "आप बिहार बोर्ड (BSEB) कक्षा 10 के एक अनुभवी संस्कृत शिक्षक हैं। छात्र के प्रश्न या वाक्य का सरल हिंदी में पदच्छेद, संधि-विच्छेद, समास, कारक-विभक्ति सूत्र (जैसे- कर्तरि प्रथमा, कर्मणि द्वितीया, सहार्थे तृतीया, भुवः प्रभवः आदि) और शुद्ध संस्कृत अनुवाद स्पष्ट करें।"
            "EVALUATE_SUBJECTIVE" ->
                "आप बिहार बोर्ड कक्षा 10 के परीक्षक हैं। छात्र द्वारा लिखे गए लघु/दीर्घ उत्तरीय उत्तर की तुलना आदर्श उत्तर से करें। सरल हिंदी में बताएं: (1) उत्तर के मजबूत बिंदु, (2) क्या छूटा है या क्या सुधार करें, (3) बोर्ड परीक्षा में पूरे अंक पाने के लिए सुझाव, और (4) अनुमानित अंक। आदर्श संदर्भ उत्तर: $referenceAnswer"
            else ->
                "आप बिहार बोर्ड (BSEB) कक्षा 10 ($subjectHi) के एक मित्रवत शिक्षक हैं। छात्र के प्रश्न को बहुत सरल, स्पष्ट और परीक्षा-उपयोगी हिंदी में मुख्य बिंदुओं के साथ समझाएं।"
        }

        try {
            val promptText = "$systemInstruction\n\nछात्र का प्रश्न/उत्तर:\n$userInput"
            val rawReply = executeGeminiPost(apiKey = apiKey, prompt = promptText)
            AiAssistantResult.SuccessText(rawReply)
        } catch (e: Exception) {
            AiAssistantResult.Error(
                "ऑनलाइन AI सेवा से जुड़ने में त्रुटि: ${e.localizedMessage ?: "नेटवर्क या API सीमा (Rate Limit) की जाँच करें"}। कृपया इंटरनेट कनेक्शन या API Key जाँचें।"
            )
        }
    }

    suspend fun generateAiPracticeQuestions(
        apiKey: String,
        chapterId: String,
        subjectCode: String,
        subSubjectHi: String,
        textbookHi: String,
        chapterTitleHi: String,
        count: Int = 3
    ): AiAssistantResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AiAssistantResult.Error(
                "नए AI अभ्यास प्रश्न बनाने के लिए कृपया पहले सेटिंग्स में अपनी निःशुल्क Gemini API Key जोड़ें।"
            )
        }

        val prompt =
            "बिहार बोर्ड (BSEB) कक्षा 10 के विषय '$subjectCode' ($subSubjectHi), पाठ्यपुस्तक '$textbookHi', अध्याय '$chapterTitleHi' पर आधारित $count बहुविकल्पीय (MCQ) अभ्यास प्रश्न केवल शुद्ध JSON Array प्रारूप में बनाएं। " +
            "महत्वपूर्ण नियम: किसी भी प्रश्न को पिछले वर्ष का आधिकारिक पेपर न लिखें। केवल JSON Array लौटाएं जिसमें topic, difficulty, questionText, options (4 विकल्प), correctAnswer और explanation फ़ील्ड हों।"

        try {
            val rawReply = executeGeminiPost(apiKey = apiKey, prompt = prompt)
            val cleanedJson = rawReply
                .replace("```json", "")
                .replace("```", "")
                .trim()
            val arr = JSONArray(cleanedJson)
            val generated = mutableListOf<QuestionEntity>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val optsArr = obj.optJSONArray("options") ?: JSONArray()
                val opts = mutableListOf<String>()
                for (j in 0 until optsArr.length()) {
                    opts.add(optsArr.optString(j))
                }
                val correct = obj.optString("correctAnswer", opts.firstOrNull().orEmpty())
                if (opts.size >= 2 && correct.isNotBlank()) {
                    generated.add(
                        QuestionEntity(
                            questionId = "Q_AI_${UUID.randomUUID().toString().take(8).uppercase()}",
                            chapterId = chapterId,
                            subject = subjectCode,
                            subSubject = subSubjectHi,
                            textbook = textbookHi,
                            topic = obj.optString("topic", chapterTitleHi),
                            questionType = "MCQ",
                            difficulty = obj.optString("difficulty", "MEDIUM"),
                            questionText = obj.getString("questionText"),
                            optionsJson = BackupRestoreSerializer.encodeOptionsJson(opts),
                            correctAnswer = correct,
                            explanation = obj.optString("explanation", "AI द्वारा जनित अभ्यास व्याख्या।"),
                            marks = 1,
                            sourceType = "AI_GENERATED",
                            sourceDetail = "AI अभ्यास प्रश्न (आधिकारिक बोर्ड प्रश्न नहीं)",
                            verificationStatus = "AI_DRAFT"
                        )
                    )
                }
            }
            if (generated.isEmpty()) {
                AiAssistantResult.Error("AI से वैध प्रश्न प्रारूप प्राप्त नहीं हुआ। कृपया पुनः प्रयास करें।")
            } else {
                AiAssistantResult.SuccessGeneratedQuestions(generated)
            }
        } catch (e: Exception) {
            AiAssistantResult.Error(
                "AI प्रश्न निर्माण में त्रुटि: ${e.localizedMessage ?: "नेटवर्क या कोटा सीमा"}।"
            )
        }
    }

    private fun executeGeminiPost(apiKey: String, prompt: String): String {
        val url = URL("$ENDPOINT_BASE$apiKey")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val responseBody = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }

        if (code !in 200..299) {
            throw IllegalStateException("HTTP $code: $responseBody")
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates") ?: JSONArray()
        if (candidates.length() == 0) {
            throw IllegalStateException("कोई उत्तर प्राप्त नहीं हुआ (Empty candidates).")
        }
        val content = candidates.getJSONObject(0).optJSONObject("content")
        val parts = content?.optJSONArray("parts") ?: JSONArray()
        return if (parts.length() > 0) {
            parts.getJSONObject(0).optString("text", "")
        } else {
            ""
        }
    }
}
