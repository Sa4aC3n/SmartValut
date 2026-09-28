package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "net_worth_snapshots")
data class NetWorthSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val dateKey: String = "", // e.g. "2026-09-24" for daily snapshot deduplication
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val vaultsTotal: Double = 0.0,
    val cashSavingsTotal: Double = 0.0,
    val goldValueTotal: Double = 0.0,
    val debtsOwedToMeTotal: Double = 0.0,
    val debtsIOweTotal: Double = 0.0
)
