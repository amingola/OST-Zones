package com.example.ostzones

//intRange represents the 2-digit component of the 8-digit hex ARGB hex value
enum class SeekBarColor(val intRange: IntRange) {
    ALPHA(0..1),
    RED(2..3),
    GREEN(4..5),
    BLUE(6..7)
}