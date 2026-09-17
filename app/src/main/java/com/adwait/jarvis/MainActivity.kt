package com.adwait.jarvis

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { JarvisApp() }
    }
}

private data class ChatMessage(val text: String, val fromJarvis: Boolean)

private val Night = Color(0xFF050B12)
private val Panel = Color(0xFF0B1A27)
private val Cyan = Color(0xFF27DDF5)
private val SoftCyan = Color(0xFFB6F5FF)

@Composable
private fun JarvisApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val messages = remember {
        mutableStateListOf(ChatMessage("Good evening. JARVIS online. How may I assist you?", true))
    }
    var prompt by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context.applicationContext) { status -> ttsReady = status == TextToSpeech.SUCCESS }
    }
    DisposableEffect(tts) { onDispose { tts.shutdown() } }

    fun replyTo(text: String) {
        val request = text.trim()
        if (request.isEmpty()) return
        messages += ChatMessage(request, false)
        prompt = ""
        val reply = jarvisReply(context, request)
        messages += ChatMessage(reply, true)
        if (ttsReady) tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "jarvis-reply")
    }

    val speechResult = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        listening = false
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (spoken != null) replyTo(spoken) else messages += ChatMessage("I did not catch that. Please try again.", true)
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            listening = true
            speechResult.launch(voiceIntent())
        } else {
            messages += ChatMessage("Microphone access is needed for voice input. You can enable it in Settings.", true)
        }
    }
    val beginListening = {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            messages += ChatMessage(context.getString(com.adwait.jarvis.R.string.voice_not_available), true)
        } else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            listening = true
            speechResult.launch(voiceIntent())
        } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    MaterialTheme {
        Surface(color = Night, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                JarvisHeader(listening)
                Spacer(Modifier.height(18.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) { items(messages) { ChatBubble(it) } }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message JARVIS", color = Color(0xFF7790A0)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Cyan, unfocusedBorderColor = Color(0xFF315062),
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            cursorColor = Cyan,
                        ),
                    )
                    IconButton(onClick = { replyTo(prompt) }, modifier = Modifier.padding(start = 6.dp)) {
                        Icon(Icons.Default.Send, "Send message", tint = Cyan)
                    }
                    IconButton(onClick = beginListening, modifier = Modifier.padding(start = 2.dp)) {
                        Icon(Icons.Default.Mic, "Speak to JARVIS", tint = Cyan)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = beginListening,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Night),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    if (listening) CircularProgressIndicator(Modifier.size(18.dp), color = Night, strokeWidth = 2.dp)
                    else Icon(Icons.Default.Mic, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (listening) "LISTENING..." else "TAP TO SPEAK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun JarvisHeader(listening: Boolean) {
    Box(
        modifier = Modifier.size(112.dp).border(2.dp, Cyan, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("J", color = Cyan, fontSize = 58.sp, fontWeight = FontWeight.Light)
    }
    Spacer(Modifier.height(12.dp))
    Text("J A R V I S", color = Cyan, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = 5.sp)
    Text(if (listening) "VOICE LINK ACTIVE" else "SYSTEM ONLINE", color = SoftCyan, fontSize = 12.sp, letterSpacing = 2.sp)
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromJarvis) Arrangement.Start else Arrangement.End) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (message.fromJarvis) Panel else Color(0xFF123C4B)),
            modifier = Modifier.fillMaxWidth(0.86f),
        ) {
            Text(
                text = message.text,
                color = Color.White,
                modifier = Modifier.padding(14.dp),
                textAlign = if (message.fromJarvis) TextAlign.Start else TextAlign.End,
            )
        }
    }
}

private fun voiceIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    putExtra(RecognizerIntent.EXTRA_PROMPT, "Listening for your command")
}

private fun jarvisReply(context: Context, request: String): String {
    val normalized = request.lowercase(Locale.getDefault())
    return when {
        normalized.contains("time") -> "The time is ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())}."
        normalized.contains("date") || normalized.contains("day") -> "Today is ${DateFormat.getDateInstance(DateFormat.FULL).format(Date())}."
        normalized.startsWith("search ") || normalized.contains("google ") -> {
            val query = request.substringAfter("search ", request.substringAfter("google ", request)).trim()
            openUrl(context, "https://www.google.com/search?q=${Uri.encode(query)}")
            "Searching Google for $query."
        }
        normalized.startsWith("youtube ") || normalized.contains("open youtube") -> {
            val query = request.substringAfter("youtube", "").trim()
            val url = if (query.isEmpty()) "https://www.youtube.com" else "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
            openUrl(context, url)
            if (query.isEmpty()) "Opening YouTube." else "Opening YouTube results for $query."
        }
        normalized.contains("settings") -> {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "Opening device settings."
        }
        else -> "I heard: $request. I can tell you the time or date, search Google, and open YouTube."
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // A reply is still displayed if no browser is installed.
    }
}
