package com.zoya.assistant.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Result of a tool call, sent back to Gemini so it can narrate the outcome in character. */
sealed class ToolResult {
    data class Success(val message: String) : ToolResult()
    data class MissingPermission(val permission: String, val friendlyReason: String) : ToolResult()
    data class Failure(val reason: String) : ToolResult()
}

private fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Native implementations behind each function-calling tool Gemini can invoke. Every function
 * checks its own permission first and returns a MissingPermission result instead of crashing or
 * silently failing — ToolExecutionEngine turns that into a prompt Zoya can react to in character.
 */
object DeviceTools {

    fun openApp(context: Context, packageName: String): ToolResult {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ToolResult.Success("Opened $packageName")
        } else {
            ToolResult.Failure("No app found for package '$packageName'")
        }
    }

    private fun lookupContactNumber(context: Context, contactName: String): String? {
        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%$contactName%")

        resolver.query(uri, projection, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return cursor.getString(numberIndex)
            }
        }
        return null
    }

    fun searchAndCallContact(context: Context, contactName: String): ToolResult {
        if (!hasPermission(context, Manifest.permission.READ_CONTACTS)) {
            return ToolResult.MissingPermission(
                Manifest.permission.READ_CONTACTS,
                "I need contacts access to find $contactName"
            )
        }
        if (!hasPermission(context, Manifest.permission.CALL_PHONE)) {
            return ToolResult.MissingPermission(
                Manifest.permission.CALL_PHONE,
                "I need call permission to actually dial"
            )
        }
        val number = lookupContactNumber(context, contactName)
            ?: return ToolResult.Failure("Couldn't find a contact named '$contactName'")

        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(callIntent)
        return ToolResult.Success("Calling $contactName")
    }

    fun sendWhatsAppMessage(context: Context, contactName: String, message: String): ToolResult {
        if (!hasPermission(context, Manifest.permission.READ_CONTACTS)) {
            return ToolResult.MissingPermission(
                Manifest.permission.READ_CONTACTS,
                "I need contacts access to find $contactName"
            )
        }
        val number = lookupContactNumber(context, contactName)
            ?: return ToolResult.Failure("Couldn't find a contact named '$contactName'")

        // WhatsApp expects international format with no leading '+' or symbols for its api URI.
        val cleanNumber = number.replace(Regex("[^0-9]"), "")
        val uri = Uri.parse(
            "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}"
        )
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ToolResult.Success("Message queued to $contactName on WhatsApp")
        } catch (e: android.content.ActivityNotFoundException) {
            ToolResult.Failure("WhatsApp isn't installed")
        }
    }

    fun sendGmail(context: Context, recipientEmail: String, subject: String, body: String): ToolResult {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            setPackage("com.google.android.gm")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ToolResult.Success("Drafted an email to $recipientEmail")
        } catch (e: android.content.ActivityNotFoundException) {
            // Fall back to any mail client if Gmail specifically isn't present.
            val fallback = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(fallback)
                ToolResult.Success("Drafted an email to $recipientEmail")
            } catch (e2: android.content.ActivityNotFoundException) {
                ToolResult.Failure("No email app available")
            }
        }
    }

    fun sendSms(context: Context, contactName: String, message: String): ToolResult {
        if (!hasPermission(context, Manifest.permission.READ_CONTACTS)) {
            return ToolResult.MissingPermission(
                Manifest.permission.READ_CONTACTS,
                "I need contacts access to find $contactName"
            )
        }
        if (!hasPermission(context, Manifest.permission.SEND_SMS)) {
            return ToolResult.MissingPermission(
                Manifest.permission.SEND_SMS,
                "I need SMS permission to text $contactName"
            )
        }
        val number = lookupContactNumber(context, contactName)
            ?: return ToolResult.Failure("Couldn't find a contact named '$contactName'")
        return try {
            android.telephony.SmsManager.getDefault().sendTextMessage(number, null, message, null, null)
            ToolResult.Success("Texted $contactName")
        } catch (e: Exception) {
            ToolResult.Failure("Couldn't send the text: ${e.message}")
        }
    }

    /** Torch on/off — this is the flashlight LED, not full camera capture, so no permission needed. */
    fun toggleFlashlight(context: Context, turnOn: Boolean): ToolResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return ToolResult.Failure("No camera/flash on this device")
            cameraManager.setTorchMode(cameraId, turnOn)
            ToolResult.Success(if (turnOn) "Flashlight on" else "Flashlight off")
        } catch (e: Exception) {
            ToolResult.Failure("Couldn't toggle the flashlight: ${e.message}")
        }
    }

    fun adjustVolume(context: Context, direction: String): ToolResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val adjustment = when (direction.lowercase()) {
            "up" -> AudioManager.ADJUST_RAISE
            "down" -> AudioManager.ADJUST_LOWER
            "mute" -> AudioManager.ADJUST_MUTE
            "unmute" -> AudioManager.ADJUST_UNMUTE
            else -> return ToolResult.Failure("Unknown volume direction '$direction'")
        }
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, adjustment, AudioManager.FLAG_SHOW_UI)
        return ToolResult.Success("Volume $direction")
    }

    fun openCamera(context: Context): ToolResult {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ToolResult.Success("Camera's open, say cheese")
        } catch (e: android.content.ActivityNotFoundException) {
            ToolResult.Failure("No camera app available")
        }
    }

    fun searchWeb(context: Context, query: String): ToolResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(android.app.SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ToolResult.Success("Searching the web for '$query'")
        } catch (e: android.content.ActivityNotFoundException) {
            // Fall back to a plain browser search if no dedicated search handler exists.
            val fallback = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(fallback)
            ToolResult.Success("Searching the web for '$query'")
        }
    }

    /** Deep-links into a system settings page — e.g. "wifi", "bluetooth", "display", "battery". */
    fun openSystemSettings(context: Context, section: String): ToolResult {
        val action = when (section.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            "apps" -> Settings.ACTION_APPLICATION_SETTINGS
            "location" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "notifications" -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        return try {
            context.startActivity(intent)
            ToolResult.Success("Opened $section settings")
        } catch (e: android.content.ActivityNotFoundException) {
            ToolResult.Failure("Couldn't open $section settings on this device")
        }
    }
}
