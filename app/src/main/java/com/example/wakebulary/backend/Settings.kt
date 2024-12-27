package com.example.wakebulary.backend

enum class WordDelay(val delay: Int) {
    ON_TAP(0),
    ONE_SECOND(1),
    TWO_SECONDS(2),
    THREE_SECONDS(3),
    FIVE_SECONDS(5),
    TEN_SECONDS(10),
}

object Settings {
    var alpha = 8
    var wordDelay = WordDelay.ON_TAP
    var sampleSize = 30
}