package snippets.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import snippets.dto.request.Rule
import snippets.dto.request.SaveRulesReq
import snippets.enums.RulesType
import snippets.factories.FormatterRulesFactory
import snippets.factories.LinterRulesFactory
import snippets.model.FormatterRulesState
import snippets.repositories.FormatterRulesStateRepository

@Service
class RulesService(
    private val rulesStateRepository: FormatterRulesStateRepository,
    private val formatterRulesFactory: FormatterRulesFactory,
    private val linterRulesFactory: LinterRulesFactory,
    private val assetService: AssetService,
    private val authorizationServiceClient: AuthorizationServiceClient,
    private val runnerServiceProducer: RunnerServiceProducer,
    private val snippetService: SnippetService,
    private val restTemplate: RestTemplate,
    @Value("\${runner.service.url}") private val runnerServiceUrl: String,
) {
    private val objectMapper = jacksonObjectMapper()

    /**
     * Obtiene las reglas de formato del usuario autenticado.
     * Si no tiene reglas guardadas, devuelve las reglas por defecto según la versión.
     */
    fun getFormatRules(
        token: String,
        version: String,
    ): List<Rule> {
        val userId =
            authorizationServiceClient.validate(token).body
                ?: return getDefaultRules(RulesType.FORMATTER, version)

        val rulesState =
            rulesStateRepository.findByTypeAndOwnerId(RulesType.FORMATTER, userId)
                ?: rulesStateRepository.findByTypeAndOwnerIdIsNull(RulesType.FORMATTER)
                ?: return getDefaultRules(RulesType.FORMATTER, version)

        return getRulesFromState(rulesState, version, RulesType.FORMATTER)
    }

    /**
     * Obtiene las reglas de linting del usuario autenticado.
     * Por ahora devuelve lista vacía hasta implementar linter rules.
     */
    fun getLintRules(
        token: String,
        version: String,
    ): List<Rule> {
        val userId = authorizationServiceClient.validate(token).body ?: return emptyList()

        val rulesState =
            rulesStateRepository.findByTypeAndOwnerId(RulesType.LINTER, userId)
                ?: rulesStateRepository.findByTypeAndOwnerIdIsNull(RulesType.LINTER)
                ?: return getDefaultRules(RulesType.LINTER, version).also {
                    persistRulesToAsset(RulesType.LINTER, userId, it)
                }

        val rules = getRulesFromState(rulesState, version, RulesType.LINTER)
        persistRulesToAsset(RulesType.LINTER, userId, rules)
        return rules
    }

    /**
     * Guarda y publica reglas de formato del usuario.
     */
    fun saveFormatRules(
        token: String,
        request: SaveRulesReq,
    ): List<Rule> {
        val userId =
            authorizationServiceClient.validate(token).body
                ?: throw RuntimeException("Usuario no autenticado")

        val enabledIds = request.rules.filter { it.isActive }.map { it.id }
        val optionsMap =
            request.rules
                .filter { it.isActive && it.value != null }
                .associate { it.name to it.value }

        val rulesState =
            rulesStateRepository.findByTypeAndOwnerId(RulesType.FORMATTER, userId)
                ?: FormatterRulesState(
                    type = RulesType.FORMATTER,
                    ownerId = userId,
                )

        rulesState.enabledJson = enabledIds
        rulesState.optionsJson = optionsMap.ifEmpty { null }
        rulesState.configText = request.configText
        rulesState.configFormat = request.configFormat

        val savedState = rulesStateRepository.save(rulesState)

        persistRulesToAsset(RulesType.FORMATTER, userId, request.rules)

        return getRulesFromState(savedState, "1.1", RulesType.FORMATTER) // Asumimos versión 1.1 por defecto
    }

    /**
     * Guarda y publica reglas de linting del usuario.
     */
    fun saveLintRules(
        token: String,
        request: SaveRulesReq,
    ): List<Rule> {
        val userId =
            authorizationServiceClient.validate(token).body
                ?: throw RuntimeException("Usuario no autenticado")

        val enabledIds = request.rules.filter { it.isActive }.map { it.id }
        val optionsMap =
            request.rules
                .filter { it.isActive && it.value != null }
                .associate { it.name to it.value }

        val rulesState =
            rulesStateRepository.findByTypeAndOwnerId(RulesType.LINTER, userId)
                ?: FormatterRulesState(
                    type = RulesType.LINTER,
                    ownerId = userId,
                )

        rulesState.enabledJson = enabledIds
        rulesState.optionsJson = optionsMap.ifEmpty { null }
        rulesState.configText = request.configText
        rulesState.configFormat = request.configFormat

        val savedState = rulesStateRepository.save(rulesState)

        persistRulesToAsset(RulesType.LINTER, userId, request.rules)

        return getRulesFromState(savedState, "1.1", RulesType.LINTER) // Asumimos versión 1.1 por defecto
    }

    /**
     * Formatea un snippet usando las reglas del usuario (síncrono).
     */
    fun formatSnippet(
        token: String,
        snippetId: Long,
    ): String {
        val userId =
            authorizationServiceClient.validate(token).body
                ?: throw RuntimeException("Usuario no autenticado")

        val snippet = snippetService.get(snippetId)
        val rawVersion = snippet.version
        val content = snippet.content

        if (content.isBlank()) {
            throw RuntimeException("Snippet content is empty")
        }

        // Obtener reglas primero para verificar si hay reglas que requieren 1.1
        val tempRules = getFormatRules(token, rawVersion)

        // Reglas que SOLO funcionan con versión 1.1
        val v11OnlyRules =
            setOf(
                "same_line_for_if_brace",
                "same_line_for_else_brace",
                "new_line_for_if_brace",
                "single_space_separation",
                "number_of_spaces_indentation",
            )

        // Verificar si alguna regla que requiere 1.1 está activa
        val hasV11OnlyRule = tempRules.any { it.isActive && it.name in v11OnlyRules }

        // HARDCODED: Siempre usar versión 1.1 para el formatter
        // porque el parser de 1.0 no soporta llaves {} y estas reglas específicas
        val normalizedVersion = "1.1"

        // Re-obtener reglas con la versión correcta (1.1)
        val rules = getFormatRules(token, normalizedVersion)

        println("=== DEBUG: Formatting snippet ===")
        println("SnippetId: $snippetId")
        println("Raw version from snippet: '$rawVersion'")
        println("Has v1.1-only rules active: $hasV11OnlyRule")
        println("Using hardcoded version: '$normalizedVersion' (required for if/else and brace rules)")
        println("Content length: ${content.length}")

        val rulesMap = mutableMapOf<String, Any?>()
        rules.forEach { rule ->
            if (rule.isActive) {
                val normalizedValue = normalizeRuleValue(rule.name, rule.value)
                rulesMap[rule.name] = normalizedValue
            }
        }

        println("Active rules: ${rulesMap.keys}")
        println("Rules map: $rulesMap")
        rulesMap.forEach { (key, value) ->
            val typeName = value?.javaClass?.simpleName ?: "null"
            println("Rule '$key' = $value (type: $typeName)")
        }

        // Verificar reglas específicas que pueden causar problemas
        val problematicRules =
            setOf(
                "same_line_for_if_brace",
                "same_line_for_else_brace",
                "new_line_for_if_brace",
                "single_space_separation",
                "number_of_spaces_indentation",
            )
        val hasProblematicRules = rulesMap.keys.any { it in problematicRules }
        if (hasProblematicRules) {
            println("⚠️ WARNING: Active rules that require if/else structures or specific code format:")
            rulesMap.filterKeys { it in problematicRules }.forEach { (key, value) ->
                val typeName = value?.javaClass?.simpleName ?: "null"
                println("  - $key = $value ($typeName)")
            }
            println("Make sure the code contains if/else statements if using brace-related rules")
        }

        val formattedContent = callFormatterService(token, normalizedVersion, content, rulesMap)
        assetService.put("snippets", snippetId, formattedContent)

        return formattedContent
    }

    /**
     * Normaliza la versión del snippet para asegurar que se use el formato correcto.
     * Convierte formatos como "v1.1", "1.1", "1" a "1.1" o "1.0" según corresponda.
     */
    private fun normalizeVersion(version: String): String {
        val normalized = version.trim().lowercase().removePrefix("v")
        return when (normalized) {
            "1", "1.0" -> "1.0"
            "1.1" -> "1.1"
            else -> {
                println("Warning: Unknown version format '$version', defaulting to '1.1'")
                "1.1"
            }
        }
    }

    /**
     * Normaliza el valor de una regla según su tipo esperado.
     * - Reglas numéricas → Int
     *   - number_of_spaces_indentation: valor por defecto 2
     *   - newline_after_println: valor por defecto 1
     *   - newline_before_println: valor por defecto 1
     * - Reglas booleanas (todas las demás) → Boolean
     */
    private fun normalizeRuleValue(
        ruleName: String,
        value: Any?,
    ): Any {
        // Reglas numéricas
        val numericRules =
            setOf(
                "number_of_spaces_indentation",
                "newline_after_println",
                "newline_before_println",
            )

        if (ruleName in numericRules) {
            val defaultValue =
                when (ruleName) {
                    "number_of_spaces_indentation" -> 2
                    "newline_after_println", "newline_before_println" -> 1
                    else -> 1
                }

            return when (value) {
                is Number -> {
                    var intValue = value.toInt()
                    // Validar que el valor sea positivo y razonable
                    if (ruleName == "number_of_spaces_indentation") {
                        if (intValue < 1) {
                            println("Warning: Invalid indentation value $intValue, using minimum 1")
                            intValue = 1
                        } else if (intValue > 20) {
                            println("Warning: Indentation value $intValue is very large, capping at 20")
                            intValue = 20
                        }
                    }
                    println("Normalized $ruleName: $value (${value::class.simpleName}) -> $intValue (Int)")
                    intValue
                }
                is String -> {
                    // Intentar convertir string a número
                    try {
                        var intValue = value.toInt()
                        // Validar que el valor sea positivo y razonable
                        if (ruleName == "number_of_spaces_indentation") {
                            if (intValue < 1) {
                                println("Warning: Invalid indentation value $intValue, using minimum 1")
                                intValue = 1
                            } else if (intValue > 20) {
                                println("Warning: Indentation value $intValue is very large, capping at 20")
                                intValue = 20
                            }
                        }
                        println("Normalized $ruleName: '$value' (String) -> $intValue (Int)")
                        intValue
                    } catch (e: NumberFormatException) {
                        // Si es "true" o "false", usar valor por defecto
                        println("Warning: Invalid number value for $ruleName: '$value', using default $defaultValue")
                        defaultValue
                    }
                }
                null -> defaultValue
                else -> {
                    println("Warning: Unexpected type for $ruleName: ${value::class.simpleName}, using default $defaultValue")
                    defaultValue
                }
            }
        }

        // Reglas booleanas: todas las demás
        // Estas reglas (same_line_for_if_brace, same_line_for_else_brace, new_line_for_if_brace, single_space_separation)
        // son flags simples: si están presentes y no son "false", se aplican
        return when (value) {
            is Boolean -> {
                println("Normalized $ruleName: $value (Boolean) -> $value (Boolean)")
                value
            }
            is String -> {
                // Convertir strings "true"/"false" a booleanos
                val boolValue =
                    when (value.lowercase().trim()) {
                        "true", "1" -> true
                        "false", "0" -> false
                        else -> {
                            println("Warning: Invalid boolean value for $ruleName: '$value', using default true")
                            true
                        }
                    }
                println("Normalized $ruleName: '$value' (String) -> $boolValue (Boolean)")
                boolValue
            }
            is Number -> {
                val boolValue = value.toInt() != 0 // Convertir número a boolean (0 = false, otros = true)
                println("Normalized $ruleName: $value (Number) -> $boolValue (Boolean)")
                boolValue
            }
            null -> {
                // Para reglas booleanas, si está activa pero sin valor, asumir true
                println("Normalized $ruleName: null -> true (Boolean, default)")
                true
            }
            else -> {
                println("Warning: Unexpected type for $ruleName: ${value::class.simpleName}, using default true")
                true
            }
        }
    }

    private fun callFormatterService(
        token: String,
        version: String,
        content: String,
        rulesMap: Map<String, Any?>,
    ): String {
        val url = "$runnerServiceUrl/v1/formatter/format"

        try {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set("Authorization", token)
                }

            val body =
                mapOf(
                    "version" to version,
                    "source" to content,
                    "config" to rulesMap,
                )

            println("=== DEBUG: Request to formatter ===")
            println("Version: $version")
            println("Content preview (first 200 chars): ${content.take(200)}")
            println("Config: $rulesMap")
            println("Config types: ${rulesMap.mapValues { it.value?.javaClass?.simpleName ?: "null" }}")

            val entity = HttpEntity(body, headers)

            val response =
                restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    object : ParameterizedTypeReference<Map<String, Any>>() {},
                )

            if (!response.statusCode.is2xxSuccessful) {
                throw RuntimeException("Formatter service error: ${response.statusCode}")
            }

            val responseBody = response.body ?: throw RuntimeException("Empty response from formatter")
            val formatted =
                responseBody["formatted"] as? String
                    ?: throw RuntimeException("No formatted content in response: $responseBody")

            return formatted
        } catch (e: org.springframework.web.client.ResourceAccessException) {
            throw RuntimeException("Cannot connect to formatter service at $url: ${e.message}", e)
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            val errorMessage = e.responseBodyAsString
            println("=== ERROR: Formatter service returned ${e.statusCode} ===")
            println("Error message: $errorMessage")
            println("Request body that caused error:")
            println("  Version: $version")
            println("  Content: ${content.take(500)}")
            println("  Config: $rulesMap")
            throw RuntimeException("Formatter service HTTP error (${e.statusCode}): $errorMessage", e)
        } catch (e: org.springframework.web.client.HttpServerErrorException) {
            throw RuntimeException("Formatter service server error (${e.statusCode}): ${e.responseBodyAsString}", e)
        } catch (e: Exception) {
            throw RuntimeException("Error calling formatter service: ${e.message}", e)
        }
    }

    /**
     * Lint de un snippet usando las reglas del usuario (asíncrono).
     */
    fun lintSnippet(
        token: String,
        snippetId: Long,
    ) {
        val userId =
            authorizationServiceClient.validate(token).body
                ?: throw RuntimeException("Usuario no autenticado")

        // Obtener snippet para obtener la versión
        val snippet = snippetService.get(snippetId)
        val rawVersion = snippet.version
        val normalizedVersion = normalizeVersion(rawVersion)

        // Publicar evento al runner-service para lint
        runnerServiceProducer.publishLintEvent(
            snippets.config.SnippetMessage(
                snippetId = snippetId,
                userId = userId,
                version = normalizedVersion,
                jwtToken = token,
            ),
        )
    }

    /**
     * Lint de un snippet usando las reglas del usuario (síncrono).
     * Devuelve el snippet con información de lint actualizada.
     */
    fun lintSnippetSync(
        token: String,
        snippetId: Long,
    ): snippets.dto.response.SnippetDetailDto {
        val userId =
            authorizationServiceClient.validate(token).body
                ?: throw RuntimeException("Usuario no autenticado")

        val snippet = snippetService.get(snippetId)
        val rawVersion = snippet.version
        val normalizedVersion = normalizeVersion(rawVersion)
        val content = snippet.content

        if (content.isBlank()) {
            throw RuntimeException("Snippet content is empty")
        }

        // Obtener reglas de linting
        val rules = getLintRules(token, normalizedVersion)

        val rulesMap = mutableMapOf<String, Any?>()
        rules.forEach { rule ->
            if (rule.isActive) {
                val normalizedValue = normalizeRuleValue(rule.name, rule.value)
                rulesMap[rule.name] = normalizedValue
            }
        }

        // Llamar al servicio de lint síncronamente
        val warnings = callLinterService(token, normalizedVersion, content, rulesMap)

        // Guardar warnings en asset service
        val warningsJson = objectMapper.writeValueAsString(warnings)
        assetService.put("lint-warnings", snippetId, warningsJson)

        // Actualizar estado del snippet
        val success = warnings.isEmpty()
        val newStatus = if (success) snippets.model.Compliance.SUCCESS else snippets.model.Compliance.FAILED
        snippetService.updateStatus(snippetId, newStatus)

        // Obtener snippet actualizado
        val updatedSnippet = snippetService.get(snippetId)

        return snippets.dto.response.SnippetDetailDto(
            id = updatedSnippet.id,
            name = updatedSnippet.name,
            owner = updatedSnippet.owner,
            language = updatedSnippet.language,
            extension = updatedSnippet.extension,
            version = updatedSnippet.version,
            content = updatedSnippet.content,
            compliance = updatedSnippet.status,
            lintCount = warnings.size,
            isValid = success,
            warnings = warnings,
        )
    }

    private fun callLinterService(
        token: String,
        version: String,
        content: String,
        rulesMap: Map<String, Any?>,
    ): List<String> {
        val url = "$runnerServiceUrl/v1/lint"

        try {
            val headers =
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set("Authorization", token)
                }

            val body =
                mapOf(
                    "version" to version,
                    "code" to content,
                    "rules" to rulesMap,
                )

            val entity = HttpEntity(body, headers)

            val response =
                restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    object : ParameterizedTypeReference<List<String>>() {},
                )

            if (!response.statusCode.is2xxSuccessful) {
                throw RuntimeException("Linter service error: ${response.statusCode}")
            }

            return response.body ?: emptyList()
        } catch (e: org.springframework.web.client.ResourceAccessException) {
            throw RuntimeException("Cannot connect to linter service at $url: ${e.message}", e)
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            val errorMessage = e.responseBodyAsString
            throw RuntimeException("Linter service HTTP error (${e.statusCode}): $errorMessage", e)
        } catch (e: org.springframework.web.client.HttpServerErrorException) {
            throw RuntimeException("Linter service server error (${e.statusCode}): ${e.responseBodyAsString}", e)
        } catch (e: Exception) {
            throw RuntimeException("Error calling linter service: ${e.message}", e)
        }
    }

    /**
     * Guarda resultado de formato (usado por workers asíncronos).
     */
    fun saveFormatResult(
        snippetId: Long,
        formattedContent: String,
    ) {
        assetService.put("snippets", snippetId, formattedContent)
    }

    /**
     * Guarda resultado de lint (usado por workers asíncronos).
     */
    fun saveLintResult(
        snippetId: Long,
        lintWarnings: String,
    ) {
        assetService.put("lint-warnings", snippetId, lintWarnings)
    }

    private fun getDefaultRules(
        type: RulesType,
        version: String,
    ): List<Rule> {
        return when (type) {
            RulesType.FORMATTER -> formatterRulesFactory.getAvailableRules(version)
            RulesType.LINTER -> linterRulesFactory.getAvailableRules(version)
        }
    }

    private fun getRulesFromState(
        rulesState: FormatterRulesState,
        version: String,
        type: RulesType,
    ): List<Rule> {
        val defaultRules = getDefaultRules(type, version)
        val enabledIds = rulesState.enabledJson.toSet()
        val optionsMap = rulesState.optionsJson ?: emptyMap()

        return defaultRules.map { defaultRule ->
            val isActive = defaultRule.id in enabledIds
            val value = optionsMap[defaultRule.name] ?: defaultRule.value

            Rule(
                id = defaultRule.id,
                name = defaultRule.name,
                isActive = isActive,
                value = value,
            )
        }
    }

    private fun persistRulesToAsset(
        type: RulesType,
        userId: String,
        rules: List<Rule>,
    ) {
        val rulesJson = objectMapper.writeValueAsString(rules)
        val userIdLong = userId.hashCode().toLong().and(0x7FFFFFFF)
        val bucket =
            when (type) {
                RulesType.FORMATTER -> "format-rules"
                RulesType.LINTER -> "lint-rules"
            }
        try {
            assetService.put(bucket, userIdLong, rulesJson)
        } catch (e: Exception) {
            println("Error saving $bucket to asset service: ${e.message}")
        }
    }
}
