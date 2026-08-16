package com.zoya.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.assistant.ai.ZoyaState
import com.zoya.assistant.ui.components.ZoyaOrb
import com.zoya.assistant.ui.theme.*
import com.zoya.assistant.viewmodel.AssistantViewModel

private fun statusLine(state: ZoyaState): String = when (state) {
    ZoyaState.IDLE -> "Say “Zoya” or tap the mic to talk"
    ZoyaState.CONNECTING -> "Waking up…"
    ZoyaState.LISTENING -> "Listening…"
    ZoyaState.THINKING -> "One sec, thinking"
    ZoyaState.SPEAKING -> "Zoya's talking"
}

@Composable
fun VoiceScreen(viewModel: AssistantViewModel, onBack: () -> Unit, onOpenChat: () -> Unit) {
    val state by viewModel.zoyaState.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val error by viewModel.connectionError.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ZoyaBgTop, ZoyaBgBottom)))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))

            // Top bar: back, title with gradient dot, overflow
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RoundGlassIcon(Icons.Filled.ArrowBack, onClick = onBack)
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(ZoyaViolet, ZoyaPink, ZoyaCyan)))
                )
                Spacer(Modifier.width(8.dp))
                Text("Voice Pulse", color = ZoyaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                RoundGlassIcon(Icons.Filled.MoreVert, onClick = {})
            }

            Spacer(Modifier.height(48.dp))

            Text(
                "AI Voice Recognition",
                color = ZoyaTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(36.dp))

            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                ZoyaOrb(state = state, amplitude = amplitude, onTap = { viewModel.onOrbTapped() })
            }

            Spacer(Modifier.height(20.dp))

            Text(
                statusLine(state),
                color = ZoyaTextMuted,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    error.orEmpty(),
                    color = ZoyaPink,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(Modifier.weight(1f))

            // Two info chips, matching the reference's "Voice Clarity Boost" / "Instant Speech Capture"
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                InfoChip(Icons.Filled.GraphicEq, "Voice Clarity\nBoost", Modifier.weight(1f))
                InfoChip(Icons.Filled.RecordVoiceOver, "Instant Speech\nCapture", Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            // Bottom control bar: open chat, primary mic toggle, close/end call
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                RoundGlassIcon(Icons.Filled.ChatBubbleOutline, onClick = onOpenChat)
                Spacer(Modifier.width(20.dp))
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(ZoyaViolet, ZoyaPink, ZoyaCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = { viewModel.onOrbTapped() }) {
                        Icon(Icons.Filled.Mic, contentDescription = "Talk", tint = Color.White)
                    }
                }
                Spacer(Modifier.width(20.dp))
                RoundGlassIcon(Icons.Filled.Close, onClick = { viewModel.endConversation(); onBack() })
            }
        }
    }
}

@Composable
private fun RoundGlassIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White)
    ) {
        Icon(icon, contentDescription = null, tint = ZoyaTextPrimary)
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = modifier.height(88.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = ZoyaViolet, modifier = Modifier.size(20.dp))
            Text(label, color = ZoyaTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp)
        }
    }
}
