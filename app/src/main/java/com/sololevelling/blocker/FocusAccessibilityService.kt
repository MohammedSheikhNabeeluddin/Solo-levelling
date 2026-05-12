package com.sololevelling.blocker

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityService : AccessibilityService() {
    private lateinit var repository: ConfigRepository

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = ConfigRepository(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val config = repository.getConfig()

        val packageName = event.packageName?.toString().orEmpty()
        if (packageName.isNotBlank()) {
            val appDecision = BlockingEngine.shouldBlockApp(config, packageName)
            if (appDecision.blocked) {
                BlockActions.blockNow(this, appDecision.reason)
                return
            }
        }

        val textSnapshot = buildString {
            event.text?.forEach { append(it).append(' ') }
            append(event.contentDescription ?: "")
        }

        if (textSnapshot.isNotBlank()) {
            val siteDecision = BlockingEngine.shouldBlockWebsite(config, textSnapshot)
            if (siteDecision.blocked) {
                BlockActions.blockNow(this, siteDecision.reason)
            }
        }
    }

    override fun onInterrupt() = Unit
}
