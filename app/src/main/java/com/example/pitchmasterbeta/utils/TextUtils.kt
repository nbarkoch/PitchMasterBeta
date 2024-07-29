package com.example.pitchmasterbeta.utils

fun isRTL(text: String): Boolean {
    val filteredText = text.filter { Character.isLetterOrDigit(it) }
    return filteredText.firstOrNull()?.let {
        Character.getDirectionality(it) in Character.DIRECTIONALITY_RIGHT_TO_LEFT..Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
    } ?: false
}