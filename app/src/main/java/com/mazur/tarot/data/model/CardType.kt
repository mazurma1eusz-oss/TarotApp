package com.mazur.tarot.data.model

enum class CardType(val jsonValue: String) {
    MAJOR("MAJOR"),
    MINOR("MINOR");

    companion object {
        fun fromJson(value: String): CardType =
            entries.firstOrNull { it.jsonValue == value } ?: MINOR
    }
}
