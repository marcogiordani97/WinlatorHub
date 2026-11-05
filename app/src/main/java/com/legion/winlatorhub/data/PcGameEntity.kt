package com.legion.winlatorhub.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pc_games")
data class PcGameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val exeName: String,
    val path: String = ""
)
