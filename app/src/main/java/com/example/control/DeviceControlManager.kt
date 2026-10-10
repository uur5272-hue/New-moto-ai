package com.example.control

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.util.Log

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null
)

sealed class DeviceAction {
    data class OpenApp(val appName: String, val packageName: String) : DeviceAction()
    data class OpenUrl(val url: String) : DeviceAction()
    data class OpenSetting(val name: String, val intentAction: String) : DeviceAction()
    data class RequestCall(val target: String) : DeviceAction()
    data class RequestMessage(val target: String, val body: String) : DeviceAction()
}

class DeviceControlManager(private val context: Context) {

    /**
     * Retrieve launchable user-installed or system apps
     */
    fun getLaunchableApps(): List<AppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        return resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            val label = resolveInfo.loadLabel(pm).toString()
            val icon = try { resolveInfo.loadIcon(pm) } catch (e: Exception) { null }
            AppInfo(name = label, packageName = pkg, icon = icon)
        }.distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Launch an app by package name
     */
    fun launchAppByPackage(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Error launching package: $packageName", e)
            false
        }
    }

    /**
     * Search and launch app by query (e.g. "youtube", "camera", "whatsapp", "settings")
     */
    fun findAndLaunchApp(query: String): Boolean {
        val cleanQuery = query.trim().lowercase()
        val apps = getLaunchableApps()
        val match = apps.firstOrNull { it.name.lowercase() == cleanQuery }
            ?: apps.firstOrNull { it.name.lowercase().contains(cleanQuery) }
            ?: apps.firstOrNull { it.packageName.lowercase().contains(cleanQuery) }

        return if (match != null) {
            launchAppByPackage(match.packageName)
        } else {
            false
        }
    }

    /**
     * Open website in browser
     */
    fun openWebUrl(rawUrl: String): Boolean {
        val url = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            "https://$rawUrl"
        } else rawUrl

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Failed to open URL $url", e)
            false
        }
    }

    /**
     * Open specific Android Settings screen
     */
    fun openSettingScreen(action: String): Boolean {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    /**
     * Open Dialer with phone number prefilled (requires user to tap call button, safe & zero permissions needed)
     */
    fun openDialer(phoneNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber.trim())}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Failed to open dialer", e)
            false
        }
    }

    /**
     * Directly place phone call (after user explicitly confirms in confirmation dialog)
     */
    fun initiateCallDirect(phoneNumber: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${Uri.encode(phoneNumber.trim())}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback to ACTION_DIAL if permission not granted
            openDialer(phoneNumber)
        }
    }

    /**
     * Compose draft SMS message
     */
    fun draftSms(recipient: String, messageBody: String): Boolean {
        return try {
            val uri = Uri.parse("smsto:${Uri.encode(recipient.trim())}")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", messageBody)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("DeviceControlManager", "Failed to draft SMS", e)
            false
        }
    }

    /**
     * Compose WhatsApp message
     */
    fun draftWhatsApp(recipientPhone: String?, messageBody: String): Boolean {
        return try {
            val intent = if (!recipientPhone.isNullOrBlank()) {
                val cleanNumber = recipientPhone.replace(Regex("[^0-9]"), "")
                val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(messageBody)}"
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_TEXT, messageBody)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            draftSms(recipientPhone ?: "", messageBody)
        }
    }

    /**
     * Check if a voice or text command represents a device action
     */
    fun parseDeviceCommand(text: String): DeviceAction? {
        val lower = text.trim().lowercase()

        // Call command
        if (lower.startsWith("call ") || lower.startsWith("phone ") || lower.startsWith("dial ") || lower.startsWith("phone karo ")) {
            val target = lower.removePrefix("call ")
                .removePrefix("phone ")
                .removePrefix("dial ")
                .removePrefix("phone karo ")
                .trim()
            if (target.isNotEmpty()) {
                return DeviceAction.RequestCall(target)
            }
        }

        // Message command
        if (lower.startsWith("send message to ") || lower.startsWith("message ") || lower.startsWith("sms ")) {
            val clean = lower.removePrefix("send message to ").removePrefix("message ").removePrefix("sms ")
            val parts = clean.split(" saying ", " that ", " : ")
            val target = parts.firstOrNull()?.trim() ?: ""
            val body = if (parts.size > 1) parts[1].trim() else "Hello from MOTO AI"
            if (target.isNotEmpty()) {
                return DeviceAction.RequestMessage(target, body)
            }
        }

        // Open settings
        if (lower.contains("open wifi") || lower.contains("wifi settings")) {
            return DeviceAction.OpenSetting("Wi-Fi Settings", Settings.ACTION_WIFI_SETTINGS)
        }
        if (lower.contains("open bluetooth") || lower.contains("bluetooth settings")) {
            return DeviceAction.OpenSetting("Bluetooth Settings", Settings.ACTION_BLUETOOTH_SETTINGS)
        }
        if (lower == "open settings" || lower == "settings kholo") {
            return DeviceAction.OpenSetting("System Settings", Settings.ACTION_SETTINGS)
        }

        // Open website
        if (lower.startsWith("open website ") || lower.startsWith("go to ") || lower.startsWith("browse ")) {
            val url = lower.removePrefix("open website ").removePrefix("go to ").removePrefix("browse ").trim()
            return DeviceAction.OpenUrl(url)
        }
        if (lower.endsWith(".com") || lower.endsWith(".org") || lower.endsWith(".net") || lower.startsWith("www.")) {
            return DeviceAction.OpenUrl(lower)
        }

        // Open App command
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("kholo ")) {
            val appQuery = lower.removePrefix("open ").removePrefix("launch ").removePrefix("kholo ").trim()
            val apps = getLaunchableApps()
            val match = apps.firstOrNull { it.name.lowercase() == appQuery }
                ?: apps.firstOrNull { it.name.lowercase().contains(appQuery) }
            if (match != null) {
                return DeviceAction.OpenApp(match.name, match.packageName)
            }
        }

        return null
    }
}
