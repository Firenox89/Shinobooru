package com.github.firenox89.shinobooru.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.github.kittinunf.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.Exception

suspend fun showConfirmationDialog(context: Context, titleRes: Int, msgRes: Int): Result<Unit, Exception> {
    val resChannel = Channel<Boolean>()
    withContext(Dispatchers.Main) {
        AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setOnDismissListener { resChannel.offer(false) }
                .setNegativeButton(android.R.string.cancel) { dialog, which -> resChannel.offer(false) }
                .setPositiveButton(android.R.string.ok) { dialog, which -> resChannel.offer(true) }
                .show()
    }
    return if (resChannel.receive()) {
        Timber.w("confirmed")
        Result.success(Unit)
    } else {
        Timber.w("denied")
        Result.error(Exception())
    }
}