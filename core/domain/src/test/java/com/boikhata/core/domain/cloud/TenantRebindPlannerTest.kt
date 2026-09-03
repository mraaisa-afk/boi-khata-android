package com.boikhata.core.domain.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D41: TenantRebindPlanner unit tests — rebind row-migration list.
 */
class TenantRebindPlannerTest {

    @Test
    fun `should build plan with old and new tenantId`() {
        val plan = TenantRebindPlanner.plan("t_1", "tenant_abc")
        assertThat(plan.oldTenantId).isEqualTo("t_1")
        assertThat(plan.newTenantId).isEqualTo("tenant_abc")
    }

    @Test
    fun `should include all tenant tables in plan`() {
        val plan = TenantRebindPlanner.plan("t_1", "tenant_abc")
        assertThat(plan.tables).hasSize(TenantRebindPlanner.ALL_TENANT_TABLES.size)
    }

    @Test
    fun `should include bills and books in table list`() {
        val plan = TenantRebindPlanner.plan("t_1", "tenant_abc")
        val tableNames = plan.tables.map { it.tableName }
        assertThat(tableNames).containsAtLeast("bills", "books", "khata_customers", "expenses")
    }

    @Test
    fun `should include P3b tables in list`() {
        val plan = TenantRebindPlanner.plan("t_1", "tenant_abc")
        val tableNames = plan.tables.map { it.tableName }
        assertThat(tableNames).containsAtLeast("period_locks", "recurring_expenses", "budgets")
    }

    @Test
    fun `should include cloud_sync_state in list`() {
        val plan = TenantRebindPlanner.plan("t_1", "tenant_abc")
        val tableNames = plan.tables.map { it.tableName }
        assertThat(tableNames).contains("cloud_sync_state")
    }

    @Test
    fun `shouldRebind should be true when tenantIds differ`() {
        assertThat(TenantRebindPlanner.shouldRebind("t_1", "tenant_abc")).isTrue()
    }

    @Test
    fun `shouldRebind should be false when tenantIds match`() {
        assertThat(TenantRebindPlanner.shouldRebind("tenant_abc", "tenant_abc")).isFalse()
    }

    @Test
    fun `shouldRebind should be false when both are t_1`() {
        assertThat(TenantRebindPlanner.shouldRebind("t_1", "t_1")).isFalse()
    }

    @Test
    fun `all tenant tables should have at least 19 core tables`() {
        // CONVENTIONS §3 defines 19 core tables; P3b added 3 = 22.
        // master_catalog excluded (shared, no tenantId) = 21 tenant-scoped tables.
        assertThat(TenantRebindPlanner.ALL_TENANT_TABLES.size).isAtLeast(21)
    }

    @Test
    fun `should not include master_catalog in rebind list`() {
        // master_catalog is a shared catalog (no tenantId) per Firestore rules
        val plan = TenantRebindPlanner.plan("t_1", "tenant_abc")
        val tableNames = plan.tables.map { it.tableName }
        assertThat(tableNames).doesNotContain("master_catalog")
    }
}
