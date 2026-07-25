package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.proxy.DcPreset

@Entity(tableName = "dc_presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dcNumber: Int,
    val location: String,
    val wsUrl: String,
    val flag: String,
    val isCustom: Boolean = false
) {
    fun toDcPreset(pingMs: Long? = null): DcPreset {
        return DcPreset(
            id = id,
            name = name,
            dcNumber = dcNumber,
            location = location,
            wsUrl = wsUrl,
            flag = flag,
            pingMs = pingMs
        )
    }

    companion object {
        fun fromDcPreset(preset: DcPreset, isCustom: Boolean = false): PresetEntity {
            return PresetEntity(
                id = preset.id,
                name = preset.name,
                dcNumber = preset.dcNumber,
                location = preset.location,
                wsUrl = preset.wsUrl,
                flag = preset.flag,
                isCustom = isCustom
            )
        }
    }
}
