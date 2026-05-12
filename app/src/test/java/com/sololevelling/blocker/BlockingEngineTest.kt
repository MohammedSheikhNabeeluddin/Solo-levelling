package com.sololevelling.blocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class BlockingEngineTest {
    @Test
    fun `blocks configured app during focus window`() {
        val config = BlockConfig(
            blockedPackages = setOf("com.instagram.android"),
            schedule = listOf(TimeWindow(LocalTime.of(9, 0), LocalTime.of(10, 0), WindowType.FOCUS)),
        )

        val decision = BlockingEngine.shouldBlockApp(config, "com.instagram.android", LocalDateTime.of(2025, 1, 1, 9, 30))
        assertTrue(decision.blocked)
    }

    @Test
    fun `does not block youtube during focus window`() {
        val config = BlockConfig(
            blockedPackages = setOf("com.google.android.youtube", "com.instagram.android"),
            schedule = listOf(TimeWindow(LocalTime.of(9, 0), LocalTime.of(10, 0), WindowType.FOCUS)),
        )

        val decision = BlockingEngine.shouldBlockApp(config, "com.google.android.youtube", LocalDateTime.of(2025, 1, 1, 9, 30))
        assertFalse(decision.blocked)
    }

    @Test
    fun `blocks app when 24 hour lock active`() {
        val now = LocalDateTime.of(2025, 1, 1, 12, 0)
        val nowEpoch = now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        val config = BlockConfig(
            hardLockedPackages = setOf("com.instagram.android"),
            hardLockUntilEpochMillis = nowEpoch + 5_000,
        )

        val decision = BlockingEngine.shouldBlockApp(config, "com.instagram.android", now)
        assertTrue(decision.blocked)
    }

    @Test
    fun `blocks website keywords always`() {
        val config = BlockConfig(blockedDomains = setOf("porn", "xnxx"))
        val decision = BlockingEngine.shouldBlockWebsite(config, "Visit xnxx.com now")
        assertTrue(decision.blocked)
    }
}
