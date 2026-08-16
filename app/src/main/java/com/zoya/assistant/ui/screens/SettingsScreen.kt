package com.zoya.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.assistant.data.SettingsRepository
import com.zoya.assistant.ui.theme.*
import kotlinx.coroutines.launch

private val VOICE_OPTIONS = listOf("Aoede", "Kore", "Puck", "Charon", "Fenrir")

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var voiceName by remember { mutableStateOf("Aoede") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { repo.apiKeyFlow.collect { apiKey = it } }
    }
    LaunchedEffect(Unit) {
        launch { repo.voiceNameFlow.collect { voiceName = it } }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ZoyaBgTop, ZoyaBgBottom)))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ZoyaTextPrimary)
                }
                Spacer(Modifier.width(10.dp))
                Text("Settings", color = ZoyaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Spacer(Modifier.height(28.dp))

            Text("Gemini API Key", color = ZoyaTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Get one free at aistudio.google.com/app/apikey — Zoya can't connect without it.",
                color = ZoyaTextMuted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))

            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                    TextField(
                        value = apiKey,
                        onValueChange = { apiKey = it; saved = false },
                        placeholder = { Text("Paste your key here", color = ZoyaTextMuted) },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle visibility",
                            tint = ZoyaTextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    scope.launch {
                        repo.saveApiKey(apiKey)
                        saved = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZoyaViolet),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (saved) "Saved ✓" else "Save Key", color = Color.White, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(32.dp))

            Text("Zoya's Voice", color = ZoyaTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VOICE_OPTIONS.forEach { voice ->
                    Surface(
                        onClick = {
                            voiceName = voice
                            scope.launch { repo.saveVoiceName(voice) }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (voice == voiceName) ZoyaViolet.copy(alpha = 0.12f) else Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(14.dp)
                        ) {
                            Text(voice, color = ZoyaTextPrimary, fontSize = 14.sp)
                            if (voice == voiceName) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = ZoyaViolet)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                "Zoya runs fully on your device except for the live connection to Gemini — nothing is sent anywhere else.",
                color = ZoyaTextMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}
