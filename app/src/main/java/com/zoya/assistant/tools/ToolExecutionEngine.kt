package com.zoya.assistant.tools

import android.content.Context
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * JSON tool declarations sent inside the Live API session's `setup` message, describing to
 * Gemini what functions it may call and what arguments each expects. Kept as raw strings (rather
 * than a full schema DSL) since that's exactly the shape the Live API's `tools` field wants.
 */
object ToolDeclarations {
    val FUNCTION_DECLARATIONS = """
    [
      {
        "name": "openApp",
        "description": "Launch an installed app by its Android package name.",
        "parameters": {
          "type": "OBJECT",
          "properties": {
            "packageName": { "type": "STRING", "description": "e.g. com.google.android.youtube" }
          },
          "required": ["packageName"]
        }
      },
      {
        "name": "searchAndCallContact",
        "description": "Find a contact by name and place a phone call to them.",
        "parameters": {
          "type": "OBJECT",
          "properties": {
            "contactName": { "type": "STRING" }
          },
          "required": ["contactName"]
        }
      },
      {
        "name": "sendWhatsAppMessage",
        "description": "Find a contact by name and open WhatsApp with a pre-filled message to them.",
        "parameters": {
          "type": "OBJECT",
          "properties": {
            "contactName": { "type": "STRING" },
            "message": { "type": "STRING" }
          },
          "required": ["contactName", "message"]
        }
      },
      {
        "name": "sendGmail",
        "description": "Open an email app with a pre-filled draft to send.",
        "parameters": {
          "type": "OBJECT",
          "properties": {
            "recipientEmail": { "type": "STRING" },
            "subject": { "type": "STRING" },
            "body": { "type": "STRING" }
          },
          "required": ["recipientEmail", "subject", "body"]
        }
      },
      {
        "name": "sendSms",
        "description": "Find a contact by name and send them a text (SMS) message directly, no app needed.",
        "parameters": {
          "type": "OBJECT",
          "properties": {
            "contactName": { "type": "STRING" },
            "message": { "type": "STRING" }
          },
          "required": ["contactName", "message"]
        }
      },
      {
        "name": "toggleFlashlight",
        "description": "Turn the phone's flashlight/torch on or off.",
        "parameters": {
          "type": "OBJECT",
          "properties": {
            "turnOn": { "type": "BOOLEAN" }
          },
          "required": ["turnOn"]
        }
      },
      {
        "name": "adjustVolume",
        "description": "Adjust the media volume.",
        "parameters": {
          "type": "OBJECT",
          "properties": {
            "direction": { "type": "STRING", "description": "one of: up, down, mute, unmute" }
          },
          "required": ["direction"]
        }
      },
      {
        "name": "openCamera",
        "description": "Open the camera app so the user can take a photo.",
        "parameters": { "type": "OBJECT", "properties": {} }
      },
      {
        "name": "searchWeb",
        "description": "Search the web for a query and open the results.",
        "parameters": {
          "type": "OBJECT",
          "properties": {
            "query": { "type": "STRING" }
          },
          "required": ["query"]
        }
      },
      {
        "name": "openSystemSettings",
        "description": "Open a specific system settings page.",
        "parameters": {
          "type": "OBJECT",
          "properties": {
            "section": { "type": "STRING", "description": "one of: wifi, bluetooth, display, battery, sound, apps, location, notifications" }
          },
          "required": ["section"]
        }
      }
    ]
    """.trimIndent()
}

/**
 * Receives a parsed function-call name + args from LiveSessionManager, dispatches to the right
 * DeviceTools function, and returns a ToolResult that gets sent back into the Live session as a
 * function response so Zoya can react to it in her own voice.
 */
class ToolExecutionEngine(private val context: Context) {

    fun execute(functionName: String, args: JsonObject): ToolResult {
        return when (functionName) {
            "openApp" -> DeviceTools.openApp(
                context,
                args["packageName"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
            "searchAndCallContact" -> DeviceTools.searchAndCallContact(
                context,
                args["contactName"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
            "sendWhatsAppMessage" -> DeviceTools.sendWhatsAppMessage(
                context,
                args["contactName"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                args["message"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
            "sendGmail" -> DeviceTools.sendGmail(
                context,
                args["recipientEmail"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                args["subject"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                args["body"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
            "sendSms" -> DeviceTools.sendSms(
                context,
                args["contactName"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                args["message"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
            "toggleFlashlight" -> DeviceTools.toggleFlashlight(
                context,
                args["turnOn"]?.jsonPrimitive?.booleanOrNull ?: true
            )
            "adjustVolume" -> DeviceTools.adjustVolume(
                context,
                args["direction"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
            "openCamera" -> DeviceTools.openCamera(context)
            "searchWeb" -> DeviceTools.searchWeb(
                context,
                args["query"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
            "openSystemSettings" -> DeviceTools.openSystemSettings(
                context,
                args["section"]?.jsonPrimitive?.contentOrNull.orEmpty()
            )
            else -> ToolResult.Failure("Unknown tool '$functionName'")
        }
    }
}
