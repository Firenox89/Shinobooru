package com.github.firenox89.shinobooru.repo.db

import io.realm.RealmObject

open class DBTag: RealmObject() {
    var id: Long = 0L
    var name: String = ""
    var board: String = ""
    var type: Int = -1
}