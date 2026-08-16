package com.zoya.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.zoya.assistant.ai.ConversationMessage
import com.zoya.assistant.ui.theme.*
import com.zoya.assistant.viewmodel.AssistantViewModel

@Composable
fun HomeScreen(
    viewModel: AssistantViewModel,
    onOpenChat: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val transcript by viewModel.transcript.collectAsState()
    val recentUserLines = transcript
        .filter { it.role == ConversationMessage.Role.USER }
        .takeLast(4)
        .reversed()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ZoyaBgTop, ZoyaBgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
        ) {
            Spacer(Modifier.height(18.dp))

            // Top bar: avatar + settings gear (closest equivalent to the bell in the reference)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(ZoyaViolet, ZoyaPink, ZoyaCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ZoyaSurface)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = ZoyaTextPrimary)
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Hello,", color = ZoyaTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Row {
                Text("Ask Me ", color = ZoyaPink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Anything", color = ZoyaTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Text("You Need", color = ZoyaTextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))

            // Quick action row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    onClick = onOpenChat,
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Smart Chat", color = ZoyaTextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                RoundIconButton(Icons.Filled.Mic, onClick = onOpenVoice)
                RoundIconButton(Icons.Filled.Image, onClick = onOpenChat)
                RoundIconButton(Icons.Filled.AttachFile, onClick = onOpenChat)
            }

            Spacer(Modifier.height(20.dp))

            // Two feature cards
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                FeatureCard(
                    title = "Begin Smart\nChat",
                    icon = Icons.Filled.ChatBubbleOutline,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenChat
                )
                FeatureCard(
                    title = "Voice\nPulse",
                    icon = Icons.Filled.GraphicEq,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenVoice
                )
            }

            Spacer(Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Chat History", color = ZoyaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    "See All",
                    color = ZoyaTextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onOpenChat() }
                )
            }

            Spacer(Modifier.height(10.dp))

            if (recentUserLines.isEmpty()) {
                Text(
                    "No conversations yet — tap the orb or Smart Chat to say hi.",
                    color = ZoyaTextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recentUserLines) { message ->
                        ChatHistoryRow(text = message.text, onClick = onOpenChat)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White)
    ) {
        Icon(icon, contentDescription = null, tint = ZoyaTextPrimary)
    }
}

@Composable
private fun FeatureCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = modifier.height(110.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(ZoyaViolet, ZoyaPink))),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text(title, color = ZoyaTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun ChatHistoryRow(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(ZoyaBgBottom),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.ChatBubbleOutline,
                    contentDescription = null,
                    tint = ZoyaViolet,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                color = ZoyaTextPrimary,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
