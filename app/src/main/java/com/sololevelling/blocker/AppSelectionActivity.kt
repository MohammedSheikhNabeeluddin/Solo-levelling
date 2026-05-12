package com.sololevelling.blocker

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.sololevelling.blocker.databinding.ActivityAppSelectionBinding

class AppSelectionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAppSelectionBinding
    private lateinit var appInfos: List<AppInfo>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val selected = intent.getStringArrayListExtra(EXTRA_SELECTED_PACKAGES)?.toSet().orEmpty()
        appInfos = loadLaunchableApps()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_multiple_choice,
            appInfos.map { "${it.label} (${it.packageName})" },
        )
        binding.appsList.adapter = adapter
        binding.appsList.choiceMode = android.widget.ListView.CHOICE_MODE_MULTIPLE

        appInfos.forEachIndexed { index, appInfo ->
            if (appInfo.packageName in selected) {
                binding.appsList.setItemChecked(index, true)
            }
        }

        binding.saveSelectionButton.setOnClickListener {
            val chosen = mutableListOf<String>()
            appInfos.forEachIndexed { index, appInfo ->
                if (binding.appsList.isItemChecked(index)) {
                    chosen.add(appInfo.packageName)
                }
            }
            val result = Intent().putStringArrayListExtra(EXTRA_SELECTED_PACKAGES, ArrayList(chosen))
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    private fun loadLaunchableApps(): List<AppInfo> {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { app ->
                val label = pm.getApplicationLabel(app).toString()
                AppInfo(label = label, packageName = app.packageName)
            }
            .sortedBy { it.label.lowercase() }
        return apps
    }

    data class AppInfo(
        val label: String,
        val packageName: String,
    )

    companion object {
        const val EXTRA_SELECTED_PACKAGES = "extra_selected_packages"
    }
}
