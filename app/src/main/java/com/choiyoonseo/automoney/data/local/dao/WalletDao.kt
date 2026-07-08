package com.choiyoonseo.automoney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.choiyoonseo.automoney.data.local.entity.WalletEntity

@Dao
interface WalletDao {
    @Insert
    suspend fun insert(entity: WalletEntity): Long

    @Update
    suspend fun update(entity: WalletEntity)

    @Query("SELECT * FROM wallets WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): WalletEntity?
}

