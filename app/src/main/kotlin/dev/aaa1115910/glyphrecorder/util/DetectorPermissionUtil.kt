package dev.aaa1115910.glyphrecorder.util

import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import dev.aaa1115910.glyphrecorder.BuildConfig
import dev.aaa1115910.glyphrecorder.services.IngressAccessibilityService

object DetectorPermissionUtil {
    private val accessibilityServiceId =
        "${BuildConfig.APPLICATION_ID}/${IngressAccessibilityService::class.java.name}"

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        val serviceEnabledByManager = enabledServices.any { info ->
            val serviceInfo = info.resolveInfo?.serviceInfo ?: return@any false
            serviceInfo.packageName == context.packageName &&
                    serviceInfo.name == IngressAccessibilityService::class.java.name
        }
        if (serviceEnabledByManager) return true

        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':').apply {
            setString(settingValue)
        }
        while (splitter.hasNext()) {
            val id = splitter.next()
            if (id.equals(accessibilityServiceId, ignoreCase = true)) return true
        }
        return false
    }

    fun currentInputMethodPackage(context: Context): String? {
        val inputMethod = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: return null
        val slashIndex = inputMethod.indexOf('/')
        if (slashIndex <= 0) return null
        return inputMethod.substring(0, slashIndex)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun openBatteryOptimizationSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    fun requestIgnoreBatteryOptimization(context: Context) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure {
            openBatteryOptimizationSettings(context)
        }
    }
}
