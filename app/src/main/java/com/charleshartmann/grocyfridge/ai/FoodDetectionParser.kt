package com.charleshartmann.grocyfridge.ai

import com.charleshartmann.grocyfridge.model.FoodDetectionResult
import kotlinx.serialization.json.Json

class FoodDetectionParser {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun parse(rawText: String): FoodDetectionResult {
        val jsonText = rawText.substringAfter("```json", rawText)
            .substringBefore("```")
            .let { block ->
                val start = block.indexOf('{')
                val end = block.lastIndexOf('}')
                if (start >= 0 && end > start) block.substring(start, end + 1) else block
            }
            .trim()
        return json.decodeFromString(FoodDetectionResult.serializer(), jsonText)
    }
}
