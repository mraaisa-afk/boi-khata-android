package com.boikhata.shared.receipt

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MonthlyDataCopyBuilderTest {
    @Test
    fun `should build Bengali csv with escaped values`() {
        val csv = MonthlyDataCopyBuilder.build(
            shopName = "রাইসা, ট্রেডিং",
            year = 2026,
            month = 9,
            rows = listOf(MonthlyDataCopyBuilder.Row("বিক্রি", "বই, বাংলা", 2, 240.0)),
        )
        assertThat(csv).contains("\"রাইসা, ট্রেডিং\"")
        assertThat(csv).contains("\"বই, বাংলা\",2,240.00")
        assertThat(csv).contains("বই খাতা ডেটা কপি")
    }
}
