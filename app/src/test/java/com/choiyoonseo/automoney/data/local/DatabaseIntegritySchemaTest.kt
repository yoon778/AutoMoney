package com.choiyoonseo.automoney.data.local

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

class DatabaseIntegritySchemaTest {
    @Test
    fun transactionsHaveUniqueSourceNotificationHashIndex() {
        val entities = File("src/main/java/com/choiyoonseo/automoney/data/local/entity/Entities.kt")
            .readText()

        assertThat(entities).contains("Index(value = [\"sourceNotificationHash\"], unique = true)")
    }

    @Test
    fun reviewItemsReferenceTransactionOnceAndCascadeOnDelete() {
        val entities = File("src/main/java/com/choiyoonseo/automoney/data/local/entity/Entities.kt")
            .readText()

        assertThat(entities).contains("Index(value = [\"transactionId\"], unique = true)")
        assertThat(entities).contains("entity = TransactionEntity::class")
        assertThat(entities).contains("parentColumns = [\"id\"]")
        assertThat(entities).contains("childColumns = [\"transactionId\"]")
        assertThat(entities).contains("onDelete = ForeignKey.CASCADE")
    }

    @Test
    fun registeredMigrationsAreContinuousThroughCurrentSchema() {
        assertThat(AppDatabase.MIGRATIONS.map { it.startVersion to it.endVersion })
            .containsExactly(
                1 to 2,
                2 to 3,
                3 to 4,
                4 to 5,
                5 to 6,
                6 to 7,
                7 to 8,
                8 to 9,
                9 to 10,
                10 to 11,
                11 to 12,
                12 to 13,
                13 to 14
            )
            .inOrder()
    }

    @Test
    fun roomSchemaExportIsEnabledForMigrationValidation() {
        val database = File("src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt")
            .readText()
        val buildFile = File("build.gradle.kts").readText()

        assertThat(database).contains("exportSchema = true")
        assertThat(buildFile).contains("arg(\"room.schemaLocation\", \"\$projectDir/schemas\")")
        assertThat(buildFile)
            .contains("getByName(\"androidTest\").assets.directories += \"\$projectDir/schemas\"")
    }

    @Test
    fun currentRoomSchemaFileIsGenerated() {
        val schema = File("schemas/com.choiyoonseo.automoney.data.local.AppDatabase/14.json")

        assertThat(schema.exists()).isTrue()
    }

    @Test
    fun walletSchemaSurfaceIsRemovedInCurrentDatabase() {
        val entities = File("src/main/java/com/choiyoonseo/automoney/data/local/entity/Entities.kt")
            .readText()
        val database = File("src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt")
            .readText()
        val schema = File("schemas/com.choiyoonseo.automoney.data.local.AppDatabase/5.json")

        assertThat(File("src/main/java/com/choiyoonseo/automoney/data/local/dao/WalletDao.kt").exists()).isFalse()
        assertThat(entities).doesNotContain("WalletEntity")
        assertThat(database).doesNotContain("WalletEntity::class")
        assertThat(database).doesNotContain("walletDao()")
        assertThat(database).contains("DROP TABLE IF EXISTS wallets")
        assertThat(schema.readText()).doesNotContain("\"tableName\": \"wallets\"")
    }

    @Test
    fun roomSchemaFilesAreUtf8WithoutBom() {
        val schemaDir = File("schemas/com.choiyoonseo.automoney.data.local.AppDatabase")
        val schemas = schemaDir.listFiles { file -> file.extension == "json" }
            ?.toList()
            .orEmpty()

        assertThat(schemas).isNotEmpty()
        schemas.forEach { schema ->
            val bytes = schema.readBytes()
            val hasBom = bytes.size >= 3 &&
                bytes[0] == 0xEF.toByte() &&
                bytes[1] == 0xBB.toByte() &&
                bytes[2] == 0xBF.toByte()

            assertThat(hasBom).isFalse()
        }
    }

    @Test
    fun legacyRoomSchemaFileSupportsMigrationValidation() {
        val schema = File("schemas/com.choiyoonseo.automoney.data.local.AppDatabase/2.json")

        assertThat(schema.exists()).isTrue()
    }

    @Test
    fun performanceIndexesSupportMainQueries() {
        val entities = File("src/main/java/com/choiyoonseo/automoney/data/local/entity/Entities.kt")
            .readText()
        val database = File("src/main/java/com/choiyoonseo/automoney/data/local/AppDatabase.kt")
            .readText()

        assertThat(entities).contains("Index(value = [\"monthKey\", \"occurredAt\"])")
        assertThat(entities).contains("Index(value = [\"occurredAt\"])")
        assertThat(entities).contains("Index(value = [\"resolvedAt\", \"createdAt\"])")
        assertThat(entities).contains("Index(value = [\"enabled\"])")
        assertThat(database).contains("index_transactions_monthKey_occurredAt")
        assertThat(database).contains("index_transactions_occurredAt")
        assertThat(database).contains("index_review_items_resolvedAt_createdAt")
        assertThat(database).contains("index_rules_enabled")
    }

    @Test
    fun balanceMetadataUsesLinkedAccountIndex() {
        val entities = File("src/main/java/com/choiyoonseo/automoney/data/local/entity/Entities.kt")
            .readText()

        assertThat(entities).contains("Index(value = [\"linkedAssetAccountId\"])")
    }

    @Test
    fun fixedExpensesReferenceAccountsByNullableForeignKey() {
        val entities = File("src/main/java/com/choiyoonseo/automoney/data/local/entity/Entities.kt")
            .readText()

        assertThat(entities).contains("childColumns = [\"accountId\"]")
        assertThat(entities).contains("onDelete = ForeignKey.SET_NULL")
        assertThat(entities).contains("Index(value = [\"accountId\"])")
    }

    @Test
    fun enabledRulesUseStableApplicationOrder() {
        val ruleDao = File("src/main/java/com/choiyoonseo/automoney/data/local/dao/RuleDao.kt")
            .readText()

        assertThat(ruleDao).contains("SELECT * FROM rules WHERE enabled = 1 ORDER BY id ASC")
    }

    @Test
    fun openReviewCountUsesSqlCountQuery() {
        val reviewDao = File("src/main/java/com/choiyoonseo/automoney/data/local/dao/ReviewItemDao.kt")
            .readText()
        val repository = File("src/main/java/com/choiyoonseo/automoney/data/repository/RoomMoneyRepository.kt")
            .readText()

        assertThat(reviewDao).contains("SELECT COUNT(*) FROM review_items WHERE resolvedAt IS NULL")
        assertThat(repository).contains("observeOpenItemCount()")
    }

    @Test
    fun appContainerExposesAtomicReviewUseCases() {
        val appContainer = File("src/main/java/com/choiyoonseo/automoney/di/AppContainer.kt")
            .readText()

        assertThat(appContainer).contains("ResolveReviewUseCase")
        assertThat(appContainer).contains("ResolveAccountTransferUseCase")
        assertThat(appContainer).contains("val resolveReviewUseCase = ResolveReviewUseCase(repository)")
        assertThat(appContainer).contains("val resolveAccountTransferUseCase = ResolveAccountTransferUseCase(repository)")
    }
}
