package com.example.commandcenter.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.commandcenter.data.Rule
import com.example.commandcenter.data.RuleRepository
import com.example.commandcenter.domain.RuleEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TextReplacementAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var activeRules: List<Rule> = emptyList()

    override fun onServiceConnected() {
        super.onServiceConnected()
        val repository = RuleRepository(applicationContext)
        serviceScope.launch {
            repository.initializeIfEmpty()
            repository.rules.collectLatest { activeRules = it.filter(Rule::enabled) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        // Safety: only inspect the active window supplied by Accessibility.
        // No screen recording, no screenshots, no network upload.
        val root = rootInActiveWindow ?: return
        processNode(root)
    }

    private fun processNode(node: AccessibilityNodeInfo) {
        if (isSensitive(node)) return

        if (node.isEditable && node.text != null) {
            val before = node.text.toString()
            val after = RuleEngine.replace(before, activeRules)
            if (before != after) {
                node.performAction(
                    AccessibilityNodeInfo.ACTION_SET_TEXT,
                    android.os.Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            after
                        )
                    }
                )
            }
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                try {
                    processNode(child)
                } finally {
                    child.recycle()
                }
            }
        }
    }

    private fun isSensitive(node: AccessibilityNodeInfo): Boolean {
        val text = listOfNotNull(
            node.hintText?.toString(),
            node.viewIdResourceName,
            node.contentDescription?.toString()
        ).joinToString(" ").lowercase()

        val sensitive = listOf(
            "password", "passwd", "pin", "otp", "one-time",
            "verification", "auth", "authentication", "cvv",
            "security code", "card number", "رمز عبور", "رمز",
            "پین", "رمز یکبار مصرف", "کد تایید", "بانک", "کارت"
        )
        return sensitive.any(text::contains)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
