package com.github.firenox89.shinobooru.ui

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.github.kittinunf.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.Exception


suspend fun showToast(context: Context, msg: String) = withContext(Dispatchers.Main) {
    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}

suspend fun showConfirmationDialog(context: Context, titleRes: Int, msgRes: Int): Result<Unit, Exception> {
    val resChannel = Channel<Boolean>()
    withContext(Dispatchers.Main) {
        AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setMessage(msgRes)
                .setOnDismissListener { resChannel.offer(false) }
                .setNegativeButton(android.R.string.cancel) { _, _ -> resChannel.offer(false) }
                .setPositiveButton(android.R.string.ok) { _, _ -> resChannel.offer(true) }
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