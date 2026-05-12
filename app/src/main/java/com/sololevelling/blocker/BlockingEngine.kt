package com.sololevelling.blocker

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object BlockingEngine {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")

    fun parseSchedule(raw: String): List<TimeWindow> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { row ->
                val parts = row.split(',').map { it.trim() }
                if (parts.size < 2) return@mapNotNull null
                val start = runCatching { LocalTime.parse(parts[0], formatter) }.getOrNull() ?: return@mapNotNull null
                val end = runCatching { LocalTime.parse(parts[1], formatter) }.getOrNull() ?: return@mapNotNull null
                val type = if (parts.getOrNull(2)?.lowercase(Locale.US) == "break") WindowType.BREAK else WindowType.FOCUS
                TimeWindow(start = start, end = end, type = type)
            }
            .toList()
    }

    fun isFocusActive(now: LocalTime, windows: List<TimeWindow>): Boolean {
        val activeWindow = windows.firstOrNull { now inRange (it.start..it.end) }
        return activeWindow?.type == WindowType.FOCUS
    }

    fun shouldBlockApp(config: BlockConfig, packageName: String, now: LocalDateTime = LocalDateTime.now()): BlockDecision {
        if (packageName in config.alwaysAllowedPackages) return BlockDecision(false)

        if (config.hardLockUntilEpochMillis > now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() &&
            packageName in config.hardLockedPackages
        ) {
            return BlockDecision(true, "24-hour lock is active")
        }

        val isFocus = isFocusActive(now.toLocalTime(), config.schedule)
        if (isFocus && packageName in config.blockedPackages) {
            return BlockDecision(true, "Blocked by focus schedule")
        }

        return BlockDecision(false)
    }

    fun shouldBlockWebsite(config: BlockConfig, textSnapshot: String): BlockDecision {
        val lowered = textSnapshot.lowercase(Locale.US)
        val matched = config.blockedDomains.firstOrNull { lowered.contains(it.lowercase(Locale.US)) }
        return if (matched != null) BlockDecision(true, "Blocked site keyword: $matched") else BlockDecision(false)
    }

    private infix fun LocalTime.inRange(range: ClosedRange<LocalTime>): Boolean {
        return if (range.start <= range.endInclusive) {
            this >= range.start && this <= range.endInclusive
        } else {
            this >= range.start || this <= range.endInclusive
        }
    }
}
