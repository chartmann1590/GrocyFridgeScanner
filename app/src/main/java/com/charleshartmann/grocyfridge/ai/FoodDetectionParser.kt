package com.charleshartmann.grocyfridge.ai

import com.charleshartmann.grocyfridge.model.FoodDetectionResult
import kotlinx.serialization.json.Json

class FoodDetectionParser(
    private val logger: ParserLogger = ParserLogger.DEFAULT
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    fun parse(rawText: String): FoodDetectionResult {
        logger.d(TAG, "Raw LLM response (${rawText.length} chars): $rawText")

        val candidates = listOf(
            ::extractFromMarkdownFence,
            ::extractBalancedBraces,
            ::extractFirstJsonLikeBlock
        ).mapNotNull { extractor -> extractor(rawText) }

        for ((index, candidate) in candidates.withIndex()) {
            val sanitized = sanitizeJson(candidate)
            try {
                val result = json.decodeFromString(FoodDetectionResult.serializer(), sanitized)
                logger.d(TAG, "Parse succeeded on attempt ${index + 1}, found ${result.items.size} items")
                return result
            } catch (e: Exception) {
                logger.w(TAG, "Parse attempt ${index + 1} failed: ${e.message}")
                logger.d(TAG, "Attempted JSON: $sanitized")
            }
        }

        logger.w(TAG, "All parse attempts failed, returning empty result")
        return FoodDetectionResult()
    }

    private fun extractFromMarkdownFence(text: String): String? {
        val fenced = text.substringAfter("```json", text)
            .substringBefore("```")
            .trim()
        val start = fenced.indexOf('{')
        val end = fenced.lastIndexOf('}')
        if (start >= 0 && end > start) return fenced.substring(start, end + 1)
        return null
    }

    private fun extractBalancedBraces(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }

    private fun extractFirstJsonLikeBlock(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        val end = text.lastIndexOf('}')
        if (end > start) return text.substring(start, end + 1)
        return null
    }

    private fun sanitizeJson(text: String): String {
        var result = text.trim()

        result = result.replace(Regex(""",(\s*[}\]])""")) { it.groupValues[1] }

        if (!result.trimEnd().endsWith('}')) {
            var depth = 0
            for (ch in result) {
                when (ch) {
                    '{' -> depth++
                    '}' -> depth--
                }
            }
            repeat(depth.coerceAtLeast(0)) { result += "}" }
        }

        return result
    }

    companion object {
        private const val TAG = "FoodDetectionParser"
    }
}

class ParserLogger private constructor(private val delegate: (String, String) -> Unit) {
    fun d(tag: String, msg: String) = delegate(tag, msg)
    fun w(tag: String, msg: String) = delegate(tag, msg)

    companion object {
        val DEFAULT = ParserLogger { _, _ -> }
        val ANDROID = ParserLogger { tag, msg -> android.util.Log.d(tag, msg) }
    }
}
