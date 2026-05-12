package com.sololevelling.blocker

import java.time.LocalDate
import java.time.LocalTime

data class TimeWindow(
    val start: LocalTime,
    val end: LocalTime,
    val type: WindowType,
)

enum class WindowType {
    FOCUS,
    BREAK,
}

data class TaskEntry(
    val id: String,
    val title: String,
    val time: LocalTime,
    val date: LocalDate,
)

data class BlockConfig(
    val blockedPackages: Set<String> = emptySet(),
    val alwaysAllowedPackages: Set<String> = setOf("com.google.android.youtube"),
    val blockedDomains: Set<String> = emptySet(),
    val schedule: List<TimeWindow> = emptyList(),
    val hardLockedPackages: Set<String> = emptySet(),
    val hardLockUntilEpochMillis: Long = 0L,
    val checkInMinutes: Int = 30,
    val tasks: List<TaskEntry> = emptyList(),
)

data class BlockDecision(
    val blocked: Boolean,
    val reason: String = "",
)
