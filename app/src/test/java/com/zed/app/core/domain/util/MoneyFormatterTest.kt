package com.zed.app.core.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    @Test
    fun `parses decimal point`() {
        assertEquals(123456L, MoneyFormatter.parseToMinor("1234.56"))
    }

    @Test
    fun `parses decimal comma`() {
        assertEquals(123456L, MoneyFormatter.parseToMinor("1234,56"))
    }

    @Test
    fun `parses space-separated groups`() {
        assertEquals(100000L, MoneyFormatter.parseToMinor("1 000"))
    }

    @Test
    fun `rejects zero`() {
        assertEquals(null, MoneyFormatter.parseToMinor("0"))
    }

    @Test
    fun `rejects garbage`() {
        assertEquals(null, MoneyFormatter.parseToMinor("abc"))
    }
}
