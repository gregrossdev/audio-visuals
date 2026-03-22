package ui

import audio.ReactivityConfig
import kotlinx.serialization.json.*
import java.io.File

object PresetManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val presetsDir: File by lazy {
        val dir = File(System.getProperty("user.home"), ".audio-visuals/presets")
        dir.mkdirs()
        dir
    }

    fun save(preset: Preset) {
        val file = File(presetsDir, "${sanitizeFileName(preset.name)}.json")
        file.writeText(json.encodeToString(Preset.serializer(), preset))
    }

    fun load(name: String): Preset? {
        val file = File(presetsDir, "${sanitizeFileName(name)}.json")
        if (!file.exists()) return null
        return try {
            val text = file.readText()
            val jsonObj = json.parseToJsonElement(text).jsonObject

            if ("layers" in jsonObj) {
                // New multi-layer format
                json.decodeFromString(Preset.serializer(), text)
            } else if ("vizMode" in jsonObj) {
                // Old single-layer format — migrate
                migrateOldPreset(jsonObj, name)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun list(): List<String> {
        if (!presetsDir.exists()) return emptyList()
        return presetsDir.listFiles { f -> f.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    val jsonObj = json.parseToJsonElement(file.readText()).jsonObject
                    jsonObj["name"]?.jsonPrimitive?.content
                } catch (_: Exception) {
                    null
                }
            }
            ?.sorted()
            ?: emptyList()
    }

    fun delete(name: String): Boolean {
        val file = File(presetsDir, "${sanitizeFileName(name)}.json")
        return file.delete()
    }

    private fun migrateOldPreset(jsonObj: JsonObject, fallbackName: String): Preset? {
        return try {
            val name = jsonObj["name"]?.jsonPrimitive?.content ?: fallbackName
            val vizMode = json.decodeFromJsonElement(VisualizationMode.serializer(), jsonObj["vizMode"]!!)
            val themePreset = json.decodeFromJsonElement(ThemePreset.serializer(), jsonObj["themePreset"]!!)
            val config = json.decodeFromJsonElement(VisualizerConfig.serializer(), jsonObj["config"]!!)
            val reactivity = jsonObj["reactivity"]?.let {
                json.decodeFromJsonElement(ReactivityConfig.serializer(), it)
            } ?: ReactivityConfig()

            val layer = VisualizerLayer(
                id = generateLayerId(),
                vizMode = vizMode,
                config = config,
                opacity = 1.0f,
                blendMode = LayerBlendMode.NORMAL
            )

            Preset(name = name, layers = listOf(layer), themePreset = themePreset, reactivity = reactivity)
        } catch (_: Exception) {
            null
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(64)
    }
}
