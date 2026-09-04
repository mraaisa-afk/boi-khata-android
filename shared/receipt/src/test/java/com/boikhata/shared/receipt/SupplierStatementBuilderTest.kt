package com.boikhata.shared.receipt

import com.boikhata.core.domain.aging.AgingBucket
import com.boikhata.core.domain.enums.SupplierEntryType
import com.boikhata.core.domain.model.Supplier
import com.boikhata.core.domain.model.SupplierStatement
import com.boikhata.core.domain.model.SupplierStatementLine
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * D54: Supplier settlement statement builder — plain-text, WhatsApp-shareable.
 */
class SupplierStatementBuilderTest {

    private val supplier = Supplier(
        id = "s1", tenantId = "t_1", nameBn = "রাইসা প্রকাশনী",
        phone = "017...", settlementCycle = "30", notes = null,
    )

    private fun statement() = SupplierStatement(
        shopName = "রাইসা ট্রেডিং হাউজ",
        supplier = supplier,
        startDate = null,
        endDate = 1000L,
        entries = listOf(
            SupplierStatementLine(0L, SupplierEntryType.OPENING, "উদ্বোধনী দেনা", 100.0, 100.0),
            SupplierStatementLine(1L, SupplierEntryType.PAYMENT, "পেমেন্ট (trxID: xyz)", 40.0, 60.0),
        ),
        totalPayable = 60.0,
        ageDays = 5L,
        bucket = AgingBucket.GREEN,
    )

    private val fmtAmount: (Double) -> String = { "৳" + it }
    private val fmtDate: (Long) -> String = { "05/09" }

    @Test
    fun `should include shop name and supplier name`() {
        val text = SupplierStatementBuilder.buildStatementText(statement(), fmtAmount, fmtDate)
        assertThat(text).contains("রাইসা ট্রেডিং হাউজ")
        assertThat(text).contains("রাইসা প্রকাশনী")
    }

    @Test
    fun `should include total payable and aging`() {
        val text = SupplierStatementBuilder.buildStatementText(statement(), fmtAmount, fmtDate)
        assertThat(text).contains("মোট দেনা (payable): ৳60.0")
        assertThat(text).contains("🟢 <১৫ দিন")
    }

    @Test
    fun `should mark payment with minus sign`() {
        val text = SupplierStatementBuilder.buildStatementText(statement(), fmtAmount, fmtDate)
        assertThat(text).contains("-৳40.0")
    }

    @Test
    fun `should output type labels in bangla`() {
        val text = SupplierStatementBuilder.buildStatementText(statement(), fmtAmount, fmtDate)
        assertThat(SupplierStatementBuilder.typeLabel(SupplierEntryType.CONSIGNMENT)).isEqualTo("কনসাইনমেন্ট গ্রহণ")
        assertThat(SupplierStatementBuilder.typeLabel(SupplierEntryType.PAYMENT)).isEqualTo("পেমেন্ট")
        assertThat(text).contains("উদ্বোধনী দেনা")
    }

    @Test
    fun `should handle empty entries gracefully`() {
        val empty = statement().copy(entries = emptyList(), totalPayable = 0.0)
        val text = SupplierStatementBuilder.buildStatementText(empty, fmtAmount, fmtDate)
        assertThat(text).contains("কোনো লেনদেন নেই।")
    }
}
