package com.zoya.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.zoya.assistant.ui.theme.ZoyaBackground
import com.zoya.assistant.ui.theme.ZoyaPink
import com.zoya.assistant.ui.theme.ZoyaTextMuted
import com.zoya.assistant.ui.theme.ZoyaTextPrimary
import com.zoya.assistant.ui.theme.ZoyaViolet
import com.zoya.assistant.util.ZoyaPermissions

private data class PermissionCopy(val permission: String, val label: String, val sassyReason: String)

private val PERMISSION_COPY = listOf(
    PermissionCopy(
        android.Manifest.permission.RECORD_AUDIO,
        "Microphone",
        "Kinda need to hear you to be your assistant, genius."
    ),
    PermissionCopy(
        android.Manifest.permission.READ_CONTACTS,
        "Contacts",
        "So I can actually find who you're talking about."
    ),
    PermissionCopy(
        android.Manifest.permission.CALL_PHONE,
        "Phone calls",
        "How else am I supposed to dial for you, telepathy?"
    ),
    PermissionCopy(
        android.Manifest.permission.SEND_SMS,
        "Messages",
        "For texts I can fire off without you lifting a finger."
    ),
    PermissionCopy(
        android.Manifest.permission.CAMERA,
        "Camera",
        "So 'open the camera' actually opens the camera."
    ),
    PermissionCopy(
        android.Manifest.permission.POST_NOTIFICATIONS,
        "Notifications",
        "I'll live quietly in your notification shade — let me in."
    )
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsOnboardingScreen(onAllGranted: () -> Unit) {
    val permissionsState = rememberMultiplePermissionsState(ZoyaPermissions.REQUIRED)

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) onAllGranted()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZoyaBackground)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Hey, I'm Zoya 💅",
            color = ZoyaTextPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Before I can be your ridiculously capable assistant, I need a few keys to the house.",
            color = ZoyaTextMuted,
            fontSize = 15.sp
        )

        Spacer(Modifier.height(28.dp))

        permissionsState.permissions.forEach { perm ->
            val copy = PERMISSION_COPY.find { it.permission == perm.permission }
            if (copy != null) {
                PermissionRow(
                    label = copy.label,
                    reason = copy.sassyReason,
                    granted = perm.status == PermissionStatus.Granted
                )
                Spacer(Modifier.height(14.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { permissionsState.launchMultiplePermissionRequest() },
            colors = ButtonDefaults.buttonColors(containerColor = ZoyaViolet),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                if (permissionsState.allPermissionsGranted) "All set ✓" else "Alright, let's do this",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PermissionRow(label: String, reason: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (granted) ZoyaViolet else ZoyaPink, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = ZoyaTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(reason, color = ZoyaTextMuted, fontSize = 12.sp)
        }
    }
}
