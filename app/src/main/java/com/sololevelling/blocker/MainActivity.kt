package com.sololevelling.blocker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import com.sololevelling.blocker.databinding.ActivityMainBinding
import com.sololevelling.blocker.databinding.ItemScheduleRowBinding
import com.sololevelling.blocker.databinding.ItemTaskRowBinding
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: ConfigRepository
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private var selectedPackages: Set<String> = emptySet()
    private var isInitialBindingComplete = false

    private val appSelectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                selectedPackages = result.data
                    ?.getStringArrayListExtra(AppSelectionActivity.EXTRA_SELECTED_PACKAGES)
                    ?.toSet()
                    .orEmpty()
                updateSelectedAppsSummary(selectedPackages)
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                showStatus("Notifications are disabled; reminders may not appear.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = ConfigRepository(this)

        binding.addScheduleButton.setOnClickListener { addScheduleRow() }
        binding.addTaskButton.setOnClickListener { addTaskRow() }
        binding.copyTomorrowButton.setOnClickListener { copyTasksToTomorrow() }
        binding.selectAppsButton.setOnClickListener { openAppSelector() }

        binding.saveButton.setOnClickListener { saveConfiguration() }
        binding.lockTodayButton.setOnClickListener { lockSelectedApps() }
        binding.openAccessibilityButton.setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        binding.openUsageAccessButton.setOnClickListener { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }

        if (!isIgnoringBatteryOptimizations()) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        }

        requestNotificationPermission()
        bindInitialState()
        isInitialBindingComplete = true
    }

    override fun onResume() {
        super.onResume()
        if (isInitialBindingComplete) {
            updateStatus(repository.getConfig())
        }
    }

    private fun bindInitialState() {
        val config = repository.getConfig()
        binding.blockedSitesInput.setText(config.blockedDomains.joinToString(", "))
        binding.checkInInput.setText(config.checkInMinutes.toString())

        selectedPackages = config.blockedPackages
        updateSelectedAppsSummary(selectedPackages)

        binding.scheduleContainer.removeAllViews()
        if (config.schedule.isEmpty()) {
            addScheduleRow()
        } else {
            config.schedule.forEach { addScheduleRow(it) }
        }

        val today = LocalDate.now()
        val todaysTasks = config.tasks.filter { it.date == today }
        binding.tasksContainer.removeAllViews()
        if (todaysTasks.isEmpty()) {
            addTaskRow()
        } else {
            todaysTasks.forEach { addTaskRow(it) }
        }

        updateStatus(config)
    }

    private fun saveConfiguration() {
        val schedule = collectScheduleRows() ?: return
        if (BlockingEngine.hasOverlappingWindows(schedule)) {
            showStatus("Schedule windows overlap. Adjust times so they do not overlap.")
            return
        }

        val today = LocalDate.now()
        val tasks = collectTaskRows(today) ?: return
        val checkInMinutes = (binding.checkInInput.text?.toString()?.toIntOrNull() ?: 30).coerceAtLeast(15)

        val current = repository.getConfig()
        val updated = current.copy(
            blockedPackages = selectedPackages,
            blockedDomains = parseCsv(binding.blockedSitesInput.text?.toString()),
            schedule = schedule,
            checkInMinutes = checkInMinutes,
            tasks = current.tasks.filter { it.date != today } + tasks,
        )
        val saved = repository.saveConfig(updated)
        if (!saved) {
            showStatus("24-hour lock is active, settings cannot be changed.")
            return
        }

        ReminderScheduler.scheduleCheckIns(this, updated.checkInMinutes)
        ReminderScheduler.scheduleTaskReminders(this, updated.tasks)
        showStatus("Configuration saved.")
    }

    private fun lockSelectedApps() {
        if (selectedPackages.isEmpty()) {
            showStatus("Select at least one app to lock first.")
            return
        }
        repository.activate24HourLock(selectedPackages)
        showStatus("24-hour lock enabled for selected apps.")
    }

    private fun copyTasksToTomorrow() {
        val today = LocalDate.now()
        val tasks = collectTaskRows(today) ?: return
        if (tasks.isEmpty()) {
            showStatus("Add at least one task to copy.")
            return
        }

        val tomorrow = today.plusDays(1)
        val copied = tasks.map { task ->
            task.copy(id = UUID.randomUUID().toString(), date = tomorrow)
        }

        val current = repository.getConfig()
        val updated = current.copy(
            tasks = current.tasks.filterNot { it.date == tomorrow } + copied,
        )
        val saved = repository.saveConfig(updated)
        if (!saved) {
            showStatus("24-hour lock is active, settings cannot be changed.")
            return
        }
        ReminderScheduler.scheduleTaskReminders(this, updated.tasks)
        showStatus("Copied tasks to tomorrow.")
    }

    private fun addScheduleRow(window: TimeWindow? = null) {
        val rowBinding = ItemScheduleRowBinding.inflate(layoutInflater, binding.scheduleContainer, false)
        rowBinding.startTimeInput.setText(window?.start?.format(timeFormatter).orEmpty())
        rowBinding.endTimeInput.setText(window?.end?.format(timeFormatter).orEmpty())
        rowBinding.windowTypeSpinner.setSelection(if (window?.type == WindowType.BREAK) 1 else 0)
        rowBinding.removeScheduleButton.setOnClickListener {
            binding.scheduleContainer.removeView(rowBinding.root)
        }
        binding.scheduleContainer.addView(rowBinding.root)
    }

    private fun addTaskRow(task: TaskEntry? = null) {
        val rowBinding = ItemTaskRowBinding.inflate(layoutInflater, binding.tasksContainer, false)
        rowBinding.root.tag = task?.id ?: UUID.randomUUID().toString()
        rowBinding.taskTimeInput.setText(task?.time?.format(timeFormatter).orEmpty())
        rowBinding.taskTitleInput.setText(task?.title.orEmpty())
        rowBinding.removeTaskButton.setOnClickListener {
            binding.tasksContainer.removeView(rowBinding.root)
        }
        binding.tasksContainer.addView(rowBinding.root)
    }

    private fun collectScheduleRows(): List<TimeWindow>? {
        val windows = mutableListOf<TimeWindow>()
        for (child in binding.scheduleContainer.children) {
            val rowBinding = ItemScheduleRowBinding.bind(child)
            val startText = rowBinding.startTimeInput.text?.toString()?.trim().orEmpty()
            val endText = rowBinding.endTimeInput.text?.toString()?.trim().orEmpty()
            if (startText.isBlank() && endText.isBlank()) {
                continue
            }
            if (startText.isBlank() || endText.isBlank()) {
                showStatus("Every schedule row needs a start and end time.")
                return null
            }
            val start = runCatching { LocalTime.parse(startText, timeFormatter) }.getOrNull()
            val end = runCatching { LocalTime.parse(endText, timeFormatter) }.getOrNull()
            if (start == null || end == null) {
                showStatus("Use 24-hour time format like 09:30 for schedule rows.")
                return null
            }
            if (start == end) {
                showStatus("Start and end times must be different for schedule rows.")
                return null
            }
            val type = if (rowBinding.windowTypeSpinner.selectedItemPosition == 1) WindowType.BREAK else WindowType.FOCUS
            windows.add(TimeWindow(start = start, end = end, type = type))
        }
        return windows
    }

    private fun collectTaskRows(date: LocalDate): List<TaskEntry>? {
        val tasks = mutableListOf<TaskEntry>()
        for (child in binding.tasksContainer.children) {
            val rowBinding = ItemTaskRowBinding.bind(child)
            val title = rowBinding.taskTitleInput.text?.toString()?.trim().orEmpty()
            val timeText = rowBinding.taskTimeInput.text?.toString()?.trim().orEmpty()
            if (title.isBlank() && timeText.isBlank()) {
                continue
            }
            if (title.isBlank() || timeText.isBlank()) {
                showStatus("Every task needs a time and description.")
                return null
            }
            val time = runCatching { LocalTime.parse(timeText, timeFormatter) }.getOrNull()
            if (time == null) {
                showStatus("Use 24-hour time format like 14:00 for tasks.")
                return null
            }
            val id = child.tag as? String ?: UUID.randomUUID().toString()
            tasks.add(TaskEntry(id = id, title = title, time = time, date = date))
        }
        return tasks.sortedBy { it.time }
    }

    private fun openAppSelector() {
        val intent = Intent(this, AppSelectionActivity::class.java).apply {
            putStringArrayListExtra(AppSelectionActivity.EXTRA_SELECTED_PACKAGES, ArrayList(selectedPackages))
        }
        appSelectionLauncher.launch(intent)
    }

    private fun updateSelectedAppsSummary(packages: Set<String>) {
        if (packages.isEmpty()) {
            binding.selectedAppsSummary.text = getString(R.string.no_apps_selected)
            return
        }
        val pm = packageManager
        val names = packages.map { pkg ->
            runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
        }.sorted()
        binding.selectedAppsSummary.text = names.joinToString(", ")
    }

    private fun parseCsv(raw: String?): Set<String> =
        raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()

    private fun requestNotificationPermission() {
        // POST_NOTIFICATIONS was introduced in Android 13 (API 33).
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun showStatus(message: String) {
        binding.statusText.text = "Status: $message"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus(config: BlockConfig) {
        if (config.hardLockUntilEpochMillis > System.currentTimeMillis()) {
            showStatus(
                "24-hour lock active until: ${
                    java.text.DateFormat.getDateTimeInstance().format(Date(config.hardLockUntilEpochMillis))
                }"
            )
        } else {
            showStatus("Ready. Add your schedule, tasks, and blocked apps.")
        }
    }
}
