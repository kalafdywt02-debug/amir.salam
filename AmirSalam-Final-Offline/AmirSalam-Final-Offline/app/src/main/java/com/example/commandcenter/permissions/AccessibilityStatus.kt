package com.example.commandcenter.permissions

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.example.commandcenter.accessibility.TextReplacementAccessibilityService

object AccessibilityStatus {
    fun isEnabled(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return manager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any { info ->
            info.resolveInfo.serviceInfo.packageName == context.packageName &&
                info.resolveInfo.serviceInfo.name == TextReplacementAccessibilityService::class.java.name
        }
    }
}
