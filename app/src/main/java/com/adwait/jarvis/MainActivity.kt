package com.adwait.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

data class Message(val who: String, val text: String)

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var tts: TextToSpeech
    private val messages = mutableStateListOf(
        Message("JARVIS", "Systems online. Hello, Adwait.")
    )
    private var listening by mutableStateOf(false)

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startListening()
            else speak("Microphone permission is required for voice commands.")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        setContent {
            JarvisApp(
                messages = messages,
                listening = listening,
                onMic = {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    } else startListening()
                },
                onSend = { handleCommand(it) }
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Speech recognition is not available on this device.")
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { listening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false }
            override fun onError(error: Int) { listening = false }
            override fun onResults(results: Bundle?) {
                listening = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                handleCommand(text)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
        }
        speechRecognizer?.startListening(intent)
    }

    private fun handleCommand(raw: String) {
        val input = raw.trim()
        if (input.isEmpty()) return
        messages.add(Message("YOU", input))

        val lower = input.lowercase(Locale.getDefault())
        val reply = when {
            "time" in lower -> "The time is ${
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            }."
            "date" in lower || "today" in lower -> "Today is ${
                SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
            }."
            lower.contains("open youtube") -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com")))
                "Opening YouTube."
            }
            lower.contains("open google") || lower.contains("search") -> {
                val q = input.replace(Regex("(?i)open google|search"), "").trim()
                val url = if (q.isEmpty()) "https://google.com"
                          else "https://www.google.com/search?q=" + Uri.encode(q)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                if (q.isEmpty()) "Opening Google." else "Searching Google for $q."
            }
            lower.contains("hello") || lower.contains("hi") ->
                "Hello, Adwait. JARVIS is ready."
            lower.contains("who are you") ->
                "I am JARVIS, your personal Android assistant."
            else ->
                "I heard: \"$input\". My command brain is online. The next version will add a real AI model."
        }

        messages.add(Message("JARVIS", reply))
        speak(reply)
    }

    private fun speak(text: String) {
        if (::tts.isInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}

@Composable
fun JarvisApp(
    messages: List<Message>,
    listening: Boolean,
    onMic: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val bg = Color(0xFF090A0F)
    val accent = Color(0xFF9AA3FF)

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = bg) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("JARVIS", color = Color.White, fontSize = 30.sp)
                }

                Spacer(Modifier.height(18.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().height(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier.size(if (listening) 150.dp else 125.dp)
                            .background(accent.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (listening) "LISTENING" else "J",
                            color = accent,
                            fontSize = if (listening) 18.sp else 46.sp
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { m ->
                        val isUser = m.who == "YOU"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isUser) Color(0xFF1B1C25) else Color(0xFF12141B)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(m.who, color = accent, fontSize = 11.sp)
                                    Text(m.text, color = Color.White, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Talk to JARVIS") },
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onSend(text)
                                text = ""
                            }
                        }
                    ) { Text("Send") }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = onMic,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = CircleShape
                ) {
                    Text(if (listening) "Listening…" else "🎙  TAP TO TALK", fontSize = 17.sp)
                }
            }
        }
    }
}
