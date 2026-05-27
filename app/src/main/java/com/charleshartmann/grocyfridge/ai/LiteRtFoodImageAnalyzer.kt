package com.charleshartmann.grocyfridge.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.charleshartmann.grocyfridge.model.FoodDetectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LiteRtFoodImageAnalyzer(
    private val modelFile: File,
    private val parser: FoodDetectionParser = FoodDetectionParser(ParserLogger.ANDROID)
) : FoodImageAnalyzer {
    override suspend fun analyze(imagePath: String): FoodDetectionResult = withContext(Dispatchers.Default) {
        val config = EngineConfig(
            modelPath = modelFile.absolutePath,
            backend = Backend.GPU(),
            visionBackend = Backend.GPU(),
            cacheDir = modelFile.parentFile?.absolutePath
        )
        Engine(config).use { engine ->
            engine.initialize()
            engine.createConversation().use { conversation ->
                val response = conversation.sendMessage(
                    Contents.of(
                        Content.ImageFile(imagePath),
                        Content.Text(prompt)
                    )
                ).toString()
                parser.parse(response)
            }
        }
    }

    private companion object {
        val prompt = """
            Identify visible grocery/food inventory in this fridge or cupboard photo.
            Count physical retail units only, such as bags, boxes, cans, jars, bottles, cartons, and loose pieces.
            Do not guess hidden items. Ignore dishes, shelves, appliances, utensils, and non-food objects.
            Return only valid JSON with this exact shape:
            {"items":[{"name":"plain normalized food name","count":2,"container":"bag|box|bottle|can|jar|piece|unknown","confidence":0.8}]}
        """.trimIndent()
    }
}
