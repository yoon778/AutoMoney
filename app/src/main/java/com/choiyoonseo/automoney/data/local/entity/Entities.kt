package com.choiyoonseo.automoney.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.choiyoonseo.automoney.domain.assets.AssetAccountKind
import com.choiyoonseo.automoney.domain.assets.MonthlyPlanItemType
import com.choiyoonseo.automoney.domain.model.Category
import com.choiyoonseo.automoney.domain.model.ReviewReason
import com.choiyoonseo.automoney.domain.model.RuleAction
import com.choiyoonseo.automoney.domain.model.RuleMatchType
import com.choiyoonseo.automoney.domain.model.TransactionDirection
import com.choiyoonseo.automoney.domain.model.TransactionStatus
import com.choiyoonseo.automoney.domain.model.TransactionType
import java.time.Instant

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["sourceNotificationHash"], unique = true),
        Index(value = ["monthKey", "occurredAt"]),
        Index(value = ["occurredAt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurredAt: Instant,
    val amountWon: Long,
    val direction: TransactionDirection,
    val type: TransactionType,
    val category: Category?,
    val paymentMethod: String?,
    val merchant: String?,
    val counterparty: String?,
    val memo: String?,
    val sourceApp: String?,
    val sourceType: String,
    val sourceNotificationHash: String?,
    val status: TransactionStatus,
    val confidence: Double,
    val monthKey: String
)

@Entity(
    tableName = "review_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"], unique = true),
        Index(value = ["resolvedAt", "createdAt"])
    ]
)
data class ReviewItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val reason: ReviewReason,
    val createdAt: Instant,
    val resolvedAt: Instant?
)

data class ReviewItemWithTransaction(
    @Embedded val reviewItem: ReviewItemEntity,
    @Relation(
        parentColumn = "transactionId",
        entityColumn = "id"
    )
    val transaction: TransactionEntity
)

@Entity(
    tableName = "rules",
    indices = [
        Index(value = ["enabled"])
    ]
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchType: RuleMatchType,
    val matchValue: String,
    val action: RuleAction,
    val targetValue: String,
    val enabled: Boolean
)

@Entity(tableName = "asset_accounts")
data class AssetAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balanceWon: Long,
    val kind: AssetAccountKind
)

@Entity(tableName = "fixed_expenses")
data class FixedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountWon: Long,
    val withdrawalDay: Int,
    val accountName: String,
    val active: Boolean
)

@Entity(tableName = "monthly_plan_items")
data class MonthlyPlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val amountWon: Long,
    val type: MonthlyPlanItemType
)
