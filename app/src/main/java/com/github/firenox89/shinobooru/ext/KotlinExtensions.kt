package com.starcooperation.bmw.common.crossconcern.ext

inline fun <reified T> Iterable<T>.sumBy(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <reified T: Number> T.toMB(): Float = this.toFloat() / (1024 * 1024)

inline fun <reified T: Number> T.prettyMB(): String = String.format("%.2f MB", this.toMB())