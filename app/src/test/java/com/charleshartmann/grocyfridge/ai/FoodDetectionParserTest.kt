package com.charleshartmann.grocyfridge.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodDetectionParserTest {
    private val parser = FoodDetectionParser()

    @Test
    fun parsesPlainJson() {
        val result = parser.parse(
            """{"items":[{"name":"chips","count":2,"container":"bag","confidence":0.9}]}"""
        )

        assertEquals(1, result.items.size)
        assertEquals("chips", result.items.first().name)
        assertEquals(2.0, result.items.first().count, 0.0)
    }

    @Test
    fun parsesJsonFenceWithExtraText() {
        val result = parser.parse(
            """
            Here is the result:
            ```json
            {"items":[{"name":"milk","count":1,"container":"carton","confidence":0.8}]}
            ```
            """.trimIndent()
        )

        assertEquals("milk", result.items.first().name)
    }
}
