package com.charleshartmann.grocyfridge.ai

import com.charleshartmann.grocyfridge.model.FoodDetectionResult

interface FoodImageAnalyzer {
    suspend fun analyze(imagePath: String): FoodDetectionResult
}
