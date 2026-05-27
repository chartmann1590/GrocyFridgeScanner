package com.charleshartmann.grocyfridge.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun parsesMultipleItems() {
        val result = parser.parse(
            """{"items":[{"name":"milk","count":2,"container":"bottle","confidence":0.9},{"name":"eggs","count":6,"container":"box","confidence":0.85},{"name":"butter","count":1,"container":"piece","confidence":0.7}]}"""
        )

        assertEquals(3, result.items.size)
        assertEquals("milk", result.items[0].name)
        assertEquals("eggs", result.items[1].name)
        assertEquals("butter", result.items[2].name)
        assertEquals(6.0, result.items[1].count, 0.0)
    }

    @Test
    fun parsesEmptyItemsList() {
        val result = parser.parse("""{"items":[]}""")

        assertTrue(result.items.isEmpty())
    }

    @Test
    fun parsesJsonWithSurroundingConversationText() {
        val result = parser.parse(
            """
            I analyzed the photo and found the following items:
            ```json
            {"items":[{"name":"yogurt","count":3,"container":"cup","confidence":0.75}]}
            ```
            Let me know if you need anything else.
            """.trimIndent()
        )

        assertEquals(1, result.items.size)
        assertEquals("yogurt", result.items.first().name)
        assertEquals(3.0, result.items.first().count, 0.0)
    }

    @Test
    fun parsesItemWithAllContainerTypes() {
        val containers = listOf("bag", "box", "bottle", "can", "jar", "piece", "unknown")
        containers.forEach { container ->
            val result = parser.parse(
                """{"items":[{"name":"item","count":1,"container":"$container","confidence":0.5}]}"""
            )
            assertEquals(container, result.items.first().container)
        }
    }

    @Test
    fun parsesFractionalCount() {
        val result = parser.parse(
            """{"items":[{"name":"milk","count":1.5,"container":"bottle","confidence":0.9}]}"""
        )

        assertEquals(1.5, result.items.first().count, 0.001)
    }

    @Test
    fun defaultsMissingOptionalFields() {
        val result = parser.parse(
            """{"items":[{"name":"bread","count":2}]}"""
        )

        assertEquals("bread", result.items.first().name)
        assertEquals(2.0, result.items.first().count, 0.0)
        assertEquals("unknown", result.items.first().container)
        assertEquals(0.0, result.items.first().confidence, 0.0)
    }

    @Test
    fun handlesDeeplyNestedMarkdownResponse() {
        val result = parser.parse(
            """
            Sure! Here's what I found:

            ```json
            {
                "items": [
                    {"name": "cheese", "count": 2, "container": "piece", "confidence": 0.88},
                    {"name": "ham", "count": 1, "container": "pack", "confidence": 0.92}
                ]
            }
            ```

            Hope that helps!
            """.trimIndent()
        )

        assertEquals(2, result.items.size)
        assertEquals("cheese", result.items[0].name)
        assertEquals("ham", result.items[1].name)
    }

    @Test
    fun handlesTrailingCommaInItemsArray() {
        val result = parser.parse(
            """{"items":[{"name":"milk","count":1,"container":"bottle","confidence":0.9},]}"""
        )

        assertEquals(1, result.items.size)
        assertEquals("milk", result.items.first().name)
    }

    @Test
    fun handlesTrailingCommaInObject() {
        val result = parser.parse(
            """{"items":[{"name":"milk","count":1,"container":"bottle","confidence":0.9,}]}"""
        )

        assertEquals(1, result.items.size)
        assertEquals("milk", result.items.first().name)
    }

    @Test
    fun handlesMissingClosingBrace() {
        val result = parser.parse(
            """{"items":[{"name":"milk","count":1,"container":"bottle","confidence":0.9}]}"""
        )

        assertEquals(1, result.items.size)
    }

    @Test
    fun returnsEmptyResultForGarbledText() {
        val result = parser.parse(
            "I can see a fridge with some items but I'm not sure what they are exactly."
        )

        assertTrue(result.items.isEmpty())
    }

    @Test
    fun returnsEmptyResultForEmptyString() {
        val result = parser.parse("")

        assertTrue(result.items.isEmpty())
    }

    @Test
    fun returnsEmptyResultForCompletelyInvalidJson() {
        val result = parser.parse("{not valid json at all!!!")

        assertTrue(result.items.isEmpty())
    }

    @Test
    fun handlesJsonWithoutMarkdownFence() {
        val result = parser.parse(
            """
            The analysis found:
            {"items":[{"name":"eggs","count":12,"container":"box","confidence":0.95}]}
            That's all.
            """.trimIndent()
        )

        assertEquals(1, result.items.size)
        assertEquals("eggs", result.items.first().name)
        assertEquals(12.0, result.items.first().count, 0.0)
    }

    @Test
    fun handlesTrailingCommasAndMissingBrace() {
        val result = parser.parse(
            """{"items":[{"name":"milk","count":1,"container":"bottle","confidence":0.9,},{"name":"eggs","count":6,"container":"box","confidence":0.8,},]}"""
        )

        assertEquals(2, result.items.size)
        assertEquals("milk", result.items[0].name)
        assertEquals("eggs", result.items[1].name)
    }

    @Test
    fun handlesNestedJsonWithoutFence() {
        val result = parser.parse(
            """Here is the result: {"items":[{"name":"bread","count":2,"container":"bag","confidence":0.85}]} done"""
        )

        assertEquals(1, result.items.size)
        assertEquals("bread", result.items.first().name)
    }

    @Test
    fun handlesBareArrayInMarkdownFence() {
        val input = """
            ```json
            [
              {"name": "Chicken Corn Chowder Soup", "count": 1, "container": "can", "confidence": 0.95}
            ]
            ```
            """.trimIndent()
        val result = parser.parse(input)

        assertEquals(1, result.items.size)
        assertEquals("Chicken Corn Chowder Soup", result.items.first().name)
        assertEquals(1.0, result.items.first().count, 0.0)
    }

    @Test
    fun handlesBareArrayWithoutFence() {
        val result = parser.parse(
            """[{"name":"milk","count":2,"container":"bottle","confidence":0.9},{"name":"eggs","count":6,"container":"box","confidence":0.8}]"""
        )

        assertEquals(2, result.items.size)
        assertEquals("milk", result.items[0].name)
        assertEquals("eggs", result.items[1].name)
    }

    @Test
    fun handlesBareArrayWithTrailingComma() {
        val result = parser.parse(
            """
            ```json
            [
              {"name": "yogurt", "count": 3, "container": "cup", "confidence": 0.75},
            ]
            ```
            """.trimIndent()
        )

        assertEquals(1, result.items.size)
        assertEquals("yogurt", result.items.first().name)
    }
}
