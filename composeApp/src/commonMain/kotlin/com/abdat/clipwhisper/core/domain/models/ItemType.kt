package com.abdat.clipwhisper.core.domain.models

enum class ItemType {
    TEXT;

    companion object {
        fun fromString(value: String): ItemType = valueOf(value.uppercase())

    }
}