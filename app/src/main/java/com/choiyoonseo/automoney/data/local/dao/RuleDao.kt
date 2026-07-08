package com.choiyoonseo.automoney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.choiyoonseo.automoney.data.local.entity.RuleEntity

@Dao
interface RuleDao {
    @Insert
    suspend fun insert(entity: RuleEntity): Long

    @Query("SELECT * FROM rules WHERE enabled = 1")
    suspend fun enabledRules(): List<RuleEntity>
}

