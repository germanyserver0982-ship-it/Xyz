package com.zoya.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.assistant.ai.ConversationMessage
import com.zoya.assistant.ui.theme.*
import com.zoya.assistant.viewmodel.AssistantViewModel
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(viewModel: AssistantViewModel, onBack: () -> Unit, onOpenVoice: () -> Unit) {
    val transcript by viewModel.transcript.collectAsState()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(transcript.size) {
        if (transcript.isNotEmpty()) listState.animateScrollToItem(transcript.size - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ZoyaBgTop, ZoyaBgBottom)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ZoyaTextPrimary)
                }
                Spacer(Modifier.width(10.dp))
                Text("Smart Chat", color = ZoyaTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White)
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null, tint = ZoyaTextPrimary)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (transcript.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Nothing here yet", color = ZoyaTextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Type below or tap the mic — I'm listening either way.",
                        color = ZoyaTextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(transcript) { message -> ChatBubble(message) }
                }
            }

            // Input bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 18.dp, end = 6.dp)) {
                        TextField(
                            value = draft,
                            onValueChange = { draft = it },
                            placeholder = { Text("Ask me something…", color = ZoyaTextMuted) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (draft.isNotBlank()) {
                                    viewModel.sendText(draft)
                                    draft = ""
                                }
                            }),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onOpenVoice) {
                            Icon(Icons.Filled.Mic, contentDescription = "Voice", tint = ZoyaTextMuted)
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(ZoyaViolet, ZoyaPink, ZoyaCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = {
                        if (draft.isNotBlank()) {
                            viewModel.sendText(draft)
                            draft = ""
                            scope.launch { }
                        }
                    }) {
                        Icon(Icons.Filled.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ConversationMessage) {
    val isUser = message.role == ConversationMessage.Role.USER
    Row(
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isUser) {
            AvatarDot(gradient = true)
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = if (isUser) ZoyaBubbleUser else Color.White,
                modifier = Modifier.widthIn(max = 260.dp)
            ) {
                Text(
                    message.text,
                    color = if (isUser) Color.White else ZoyaTextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            if (!isUser) {
                Row(modifier = Modifier.padding(top = 4.dp, start = 4.dp)) {
                    Icon(Icons.Filled.ContentCopy, null, tint = ZoyaTextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Filled.ThumbUpOffAlt, null, tint = ZoyaTextMuted, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Filled.VolumeUp, null, tint = ZoyaTextMuted, modifier = Modifier.size(14.dp))
                }
            }
        }
        if (isUser) {
            Spacer(Modifier.width(8.dp))
            AvatarDot(gradient = false)
        }
    }
}

@Composable
private fun AvatarDot(gradient: Boolean) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(
                if (gradient) Brush.sweepGradient(listOf(ZoyaViolet, ZoyaPink, ZoyaCyan))
                else Brush.linearGradient(listOf(ZoyaTextMuted, ZoyaTextMuted))
            )
    )
}
