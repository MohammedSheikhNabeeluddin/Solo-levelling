package com.sololevelling.blocker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sololevelling.blocker.databinding.ActivityMainBinding
import java.util.Date
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: ConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = ConfigRepository(this)

        bindInitialState()

        binding.saveButton.setOnClickListener {
            val current = repository.getConfig()
            val updated = current.copy(
                blockedPackages = parseCsv(binding.blockedAppsInput.text?.toString()),
                blockedDomains = parseCsv(binding.blockedSitesInput.text?.toString()),
                schedule = BlockingEngine.parseSchedule(binding.scheduleInput.text?.toString().orEmpty()),
                reminderMinutes = (binding.reminderInput.text?.toString()?.toIntOrNull() ?: 30).coerceAtLeast(15),
            )
            val saved = repository.saveConfig(updated)
            if (!saved) {
                showStatus("24-hour lock is active, settings cannot be changed.")
                return@setOnClickListener
            }
            scheduleReminder(updated.reminderMinutes)
            showStatus("Configuration saved.")
        }

        binding.lockTodayButton.setOnClickListener {
            val packages = parseCsv(binding.blockedAppsInput.text?.toString())
            if (packages.isEmpty()) {
                showStatus("Add at least one package name first.")
                return@setOnClickListener
            }
            repository.activate24HourLock(packages)
            showStatus("24-hour lock enabled for selected apps.")
        }

        binding.openAccessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.openUsageAccessButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        if (!isIgnoringBatteryOptimizations()) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    override fun onResume() {
        super.onResume()
        bindInitialState()
    }

    private fun bindInitialState() {
        val config = repository.getConfig()
        binding.blockedAppsInput.setText(config.blockedPackages.joinToString(", "))
        binding.blockedSitesInput.setText(config.blockedDomains.joinToString(", "))
        binding.scheduleInput.setText(config.schedule.joinToString("\n") { "${it.start},${it.end},${it.type.name.lowercase()}" })
        binding.reminderInput.setText(config.reminderMinutes.toString())

        if (config.hardLockUntilEpochMillis > System.currentTimeMillis()) {
            showStatus("24-hour lock active until: ${java.text.DateFormat.getDateTimeInstance().format(Date(config.hardLockUntilEpochMillis))}")
        } else {
            showStatus("Ready. Save your schedule and block list.")
        }
    }

    private fun scheduleReminder(minutes: Int) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(minutes.toLong(), TimeUnit.MINUTES)
            .addTag(ReminderWorker.WORK_NAME)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun parseCsv(raw: String?): Set<String> =
        raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun showStatus(message: String) {
        binding.statusText.text = "Status: $message"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
