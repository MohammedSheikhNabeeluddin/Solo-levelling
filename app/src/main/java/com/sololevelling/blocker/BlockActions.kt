package com.sololevelling.blocker

import android.content.Context
import android.content.Intent

object BlockActions {
    fun blockNow(context: Context, reason: String) {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)

        val blockIntent = Intent(context, BlockingActivity::class.java).apply {
            putExtra(BlockingActivity.EXTRA_REASON, reason)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(blockIntent)
    }
}
