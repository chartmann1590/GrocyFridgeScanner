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
            ::extractBalancedBrackets,
            ::extractFirstJsonLikeBlock
        ).mapNotNull { extractor -> extractor(rawText) }

        for ((index, candidate) in candidates.withIndex()) {
            val normalized = normalizeToJson(candidate)
            val sanitized = sanitizeJson(normalized)
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

    private fun normalizeToJson(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("[")) return "{\"items\":$trimmed}"
        return trimmed
    }

    private fun extractFromMarkdownFence(text: String): String? {
        val fenced = text.substringAfter("```json", text)
            .substringBefore("```")
            .trim()
        val arrStart = fenced.indexOf('[')
        val arrEnd = fenced.lastIndexOf(']')
        if (arrStart >= 0 && arrEnd > arrStart) return fenced.substring(arrStart, arrEnd + 1)
        val objStart = fenced.indexOf('{')
        val objEnd = fenced.lastIndexOf('}')
        if (objStart >= 0 && objEnd > objStart) return fenced.substring(objStart, objEnd + 1)
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

    private fun extractBalancedBrackets(text: String): String? {
        val start = text.indexOf('[')
        if (start < 0) return null
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }

    private fun extractFirstJsonLikeBlock(text: String): String? {
        val objStart = text.indexOf('{')
        val objEnd = text.lastIndexOf('}')
        if (objStart >= 0 && objEnd > objStart) return text.substring(objStart, objEnd + 1)
        val arrStart = text.indexOf('[')
        val arrEnd = text.lastIndexOf(']')
        if (arrStart >= 0 && arrEnd > arrStart) return text.substring(arrStart, arrEnd + 1)
        return null
    }

    private fun sanitizeJson(text: String): String {
        var result = text.trim()

        result = result.replace(Regex(""",(\s*[}\]])""")) { it.groupValues[1] }

        if (!result.trimEnd().endsWith('}') && !result.trimEnd().endsWith(']')) {
            val lastClose = result.lastIndexOfAny(charArrayOf('}', ']'))
            if (lastClose >= 0) result = result.substring(0, lastClose + 1)
        }

        if (result.startsWith("{") && !result.trimEnd().endsWith('}')) {
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
