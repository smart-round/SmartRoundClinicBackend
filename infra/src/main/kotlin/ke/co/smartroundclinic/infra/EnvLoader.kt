package ke.co.smartroundclinic.infra

import io.ktor.util.logging.KtorSimpleLogger

object EnvLoader {
    private val logger = KtorSimpleLogger(this::class.simpleName!!)

    /**
     * System env vars with keys and values stripped of surrounding whitespace.
     * Handles the IntelliJ multiline-paste bug where keys arrive as "\nKEY_NAME".
     */
    private val systemEnv: Map<String, String> by lazy {
        System.getenv().entries.associate { (k, v) ->
            k.trim() to v.trim().trimEnd(';')
        }
    }

    private val fileEnv: Map<String, String> by lazy {
        val file = java.io.File(".env")
        if (!file.exists()) {
            logger.info(".env file not found, relying on system environment variables")
            return@lazy emptyMap()
        }
        logger.info("Loading environment from .env file")
        file.readLines()
            .filter { line -> line.isNotBlank() && !line.startsWith("#") && line.contains("=") }
            .associate { line ->
                val idx = line.indexOf("=")
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim().trimEnd(';').unquote()
                key to value
            }
    }

    /** Strips a single matching pair of surrounding double or single quotes, if present. */
    private fun String.unquote(): String =
        if (length >= 2 && ((startsWith("\"") && endsWith("\"")) || (startsWith("'") && endsWith("'")))) {
            substring(1, length - 1)
        } else {
            this
        }

    /** System environment variables take priority over the .env file. */
    fun get(key: String): String? = systemEnv[key]?.ifBlank { null } ?: fileEnv[key]
}
