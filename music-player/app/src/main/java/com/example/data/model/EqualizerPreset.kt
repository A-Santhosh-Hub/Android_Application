package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "equalizer_presets")
data class EqualizerPreset(
    @PrimaryKey val name: String,
    val band60Hz: Float = 0f,    // -15f to +15f dB
    val band230Hz: Float = 0f,
    val band910Hz: Float = 0f,
    val band4kHz: Float = 0f,
    val band14kHz: Float = 0f,
    val isCustom: Boolean = false
)
