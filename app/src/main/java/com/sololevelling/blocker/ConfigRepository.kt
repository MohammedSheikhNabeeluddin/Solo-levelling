package com.sololevelling.blocker

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class ConfigRepository(context: Context) {
    private val prefs = context.getSharedPreferences("solo_levelling", Context.MODE_PRIVATE)

    fun getConfig(): BlockConfig {
        val json = prefs.getString("config", null) ?: return BlockConfig()
        return runCatching {
            val root = JSONObject(json)
            BlockConfig(
                blockedPackages = root.getJSONArrayOrEmpty("blockedPackages").toStringSet(),
                alwaysAllowedPackages = root.getJSONArrayOrEmpty("alwaysAllowedPackages").toStringSet().ifEmpty {
                    setOf("com.google.android.youtube")
                },
                blockedDomains = root.getJSONArrayOrEmpty("blockedDomains").toStringSet(),
                schedule = root.getJSONArrayOrEmpty("schedule").let { arr ->
                    buildList {
                        for (i in 0 until arr.length()) {
                            val item = arr.optJSONObject(i) ?: continue
                            val start = runCatching { java.time.LocalTime.parse(item.getString("start")) }.getOrNull() ?: continue
                            val end = runCatching { java.time.LocalTime.parse(item.getString("end")) }.getOrNull() ?: continue
                            val type = runCatching { WindowType.valueOf(item.getString("type")) }.getOrDefault(WindowType.FOCUS)
                            add(TimeWindow(start, end, type))
                        }
                    }
                },
                hardLockedPackages = root.getJSONArrayOrEmpty("hardLockedPackages").toStringSet(),
                hardLockUntilEpochMillis = root.optLong("hardLockUntilEpochMillis", 0L),
                checkInMinutes = root.optInt("checkInMinutes", root.optInt("reminderMinutes", 30)).coerceAtLeast(15),
                tasks = root.getJSONArrayOrEmpty("tasks").let { arr ->
                    buildList {
                        for (i in 0 until arr.length()) {
                            val item = arr.optJSONObject(i) ?: continue
                            val title = item.optString("title").trim()
                            if (title.isBlank()) continue
                            val time = runCatching { LocalTime.parse(item.getString("time")) }.getOrNull() ?: continue
                            val date = runCatching { LocalDate.parse(item.getString("date")) }.getOrNull() ?: continue
                            val id = item.optString("id").ifBlank { UUID.randomUUID().toString() }
                            add(TaskEntry(id = id, title = title, time = time, date = date))
                        }
                    }
                },
            )
        }.getOrDefault(BlockConfig())
    }

    fun saveConfig(config: BlockConfig): Boolean {
        if (isHardLockActive(config)) return false

        val root = JSONObject().apply {
            put("blockedPackages", JSONArray(config.blockedPackages.toList()))
            put("alwaysAllowedPackages", JSONArray(config.alwaysAllowedPackages.toList()))
            put("blockedDomains", JSONArray(config.blockedDomains.toList()))
            put("schedule", JSONArray().apply {
                config.schedule.forEach { window ->
                    put(
                        JSONObject().apply {
                            put("start", window.start.toString())
                            put("end", window.end.toString())
                            put("type", window.type.name)
                        }
                    )
                }
            })
            put("hardLockedPackages", JSONArray(config.hardLockedPackages.toList()))
            put("hardLockUntilEpochMillis", config.hardLockUntilEpochMillis)
            put("checkInMinutes", config.checkInMinutes)
            put("reminderMinutes", config.checkInMinutes)
            put("tasks", JSONArray().apply {
                config.tasks.forEach { task ->
                    put(
                        JSONObject().apply {
                            put("id", task.id)
                            put("title", task.title)
                            put("time", task.time.toString())
                            put("date", task.date.toString())
                        }
                    )
                }
            })
        }

        return prefs.edit().putString("config", root.toString()).commit()
    }

    fun activate24HourLock(packagesToLock: Set<String>): Boolean {
        val existing = getConfig()
        val updated = existing.copy(
            hardLockedPackages = packagesToLock,
            hardLockUntilEpochMillis = System.currentTimeMillis() + DAY_MILLIS,
        )
        return prefs.edit().putString("config", serialize(updated)).commit()
    }

    private fun serialize(config: BlockConfig): String {
        val root = JSONObject().apply {
            put("blockedPackages", JSONArray(config.blockedPackages.toList()))
            put("alwaysAllowedPackages", JSONArray(config.alwaysAllowedPackages.toList()))
            put("blockedDomains", JSONArray(config.blockedDomains.toList()))
            put("schedule", JSONArray().apply {
                config.schedule.forEach { window ->
                    put(
                        JSONObject().apply {
                            put("start", window.start.toString())
                            put("end", window.end.toString())
                            put("type", window.type.name)
                        }
                    )
                }
            })
            put("hardLockedPackages", JSONArray(config.hardLockedPackages.toList()))
            put("hardLockUntilEpochMillis", config.hardLockUntilEpochMillis)
            put("checkInMinutes", config.checkInMinutes)
            put("reminderMinutes", config.checkInMinutes)
            put("tasks", JSONArray().apply {
                config.tasks.forEach { task ->
                    put(
                        JSONObject().apply {
                            put("id", task.id)
                            put("title", task.title)
                            put("time", task.time.toString())
                            put("date", task.date.toString())
                        }
                    )
                }
            })
        }
        return root.toString()
    }

    private fun JSONObject.getJSONArrayOrEmpty(key: String): JSONArray = optJSONArray(key) ?: JSONArray()

    private fun JSONArray.toStringSet(): Set<String> = buildSet {
        for (i in 0 until length()) {
            val value = optString(i)
            if (value.isNotBlank()) add(value.trim())
        }
    }

    private fun isHardLockActive(config: BlockConfig): Boolean =
        config.hardLockUntilEpochMillis > System.currentTimeMillis() && config.hardLockedPackages.isNotEmpty()

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}
