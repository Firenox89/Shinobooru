package com.github.firenox89.shinobooru.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import rx.subjects.PublishSubject

class ScreenEventReceiver(val screenEventStream: PublishSubject<Intent>) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        screenEventStream.onNext(intent)
    }
}
