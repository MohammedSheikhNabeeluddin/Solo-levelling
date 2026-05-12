package com.sololevelling.blocker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sololevelling.blocker.databinding.ActivityBlockingBinding

class BlockingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBlockingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val reason = intent.getStringExtra(EXTRA_REASON).orEmpty().ifBlank { "Blocked while focus is active" }
        binding.blockReason.text = reason
    }

    companion object {
        const val EXTRA_REASON = "extra_reason"
    }
}
