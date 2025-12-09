package snippets.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import snippets.config.SnippetMessage
import snippets.config.TestMessage
import snippets.dto.response.FullSnippet
import snippets.dto.response.SnippetUserDto
import snippets.dto.response.SnippetWithRoleAndWarnings
import snippets.errors.SnippetNotFound
import snippets.model.Compliance
import snippets.model.Snippet
import snippets.repositories.SnippetRepository
import kotlin.collections.forEach

@Service
class SnippetService(
    private val snippetRepository: SnippetRepository,
    @Lazy private val authorizationServiceClient: AuthorizationServiceClient,
    private val assetService: AssetService,
    private val languageService: LanguageService,
    private val runnerServiceProducer: RunnerServiceProducer,
    @Lazy private val testService: TestService,
    private val restTemplate: RestTemplate,
    @Value("\${runner.service.url}") private val runnerServiceUrl: String,
) : SnippetServiceRoutes {
    override fun create(
        name: String,
        content: String,
        languageId: String,
        owner: String,
        token: String,
    ): FullSnippet {
        val language =
            languageService.getLanguageById(languageId.toLongOrNull())
        val snippet = Snippet(name = name, language = language, owner = owner)
        snippetRepository.save(snippet)
        assetService.put("snippets", snippet.id, content)
        authorizationServiceClient.addSnippetToUser(token, owner, snippet.id, "Owner")

        // Publicar mensaje en Redis para que runner-service valide el snippet
        val userId = authorizationServiceClient.validate(token).body
        if (userId != null) {
            runnerServiceProducer.publishSnippetEvent(
                SnippetMessage(
                    snippetId = snippet.id,
                    userId = userId,
                    version = snippet.language.version,
                    jwtToken = token,
                ),
            )
        }

        return FullSnippet(snippet, content, emptyList())
    }

    override fun get(id: Long): FullSnippet {
        val snippet =
            snippetRepository.findById(id)
                .orElseThrow { SnippetNotFound("Snippet not found when trying to get it") }
        val content = assetService.get("snippets", id)

        val warningsJson: String =
            try {
                if (assetService.exists("lint-warnings", snippet.id)) {
                    val json = assetService.get("lint-warnings", snippet.id)
                    if (json.isNotBlank() && json != "[]" && json != "Search in lint-warnings not found") {
                        json
                    } else {
                        ""
                    }
                } else {
                    ""
                }
            } catch (e: Exception) {
                // No hay warnings o el asset no existe - esto es normal, no loguear como error
                ""
            }

        val warnings =
            if (warningsJson.isBlank()) {
                emptyList<String>()
            } else {
                try {
                    jacksonObjectMapper().readValue<List<String>>(
                        warningsJson,
                        object : TypeReference<List<String>>() {},
                    )
                } catch (e: Exception) {
                    // JSON inválido o vacío - solo loguear si realmente hay contenido pero está mal formado
                    if (warningsJson.isNotBlank() && warningsJson != "[]") {
                        println(
                            "Error deserializing warnings for snippet ${snippet.id}: ${e.message} (content: $warningsJson)",
                        )
                    }
                    emptyList<String>()
                }
            }

        return FullSnippet(snippet, content, warnings)
    }

    fun getFilteredSnippets(
        page: Int,
        pageSize: Int,
        snippetsIds: List<SnippetUserDto>,
        snippetName: String?,
        roles: List<String>?,
        languages: List<Long>?,
        compliance: List<Compliance>?,
    ): Pair<List<SnippetWithRoleAndWarnings>, Long> {
        val snippetIdToRoleMap = snippetsIds.associateBy({ it.snippetId }, { it.role })
        if (snippetIdToRoleMap.isEmpty()) return Pair(emptyList(), 0)
        val filteredSnippetIdToRoleMap =
            if (!roles.isNullOrEmpty()) {
                snippetIdToRoleMap.filter { entry -> roles.contains(entry.value) }
            } else {
                snippetIdToRoleMap
            }

        val snippets = snippetRepository.findAllById(filteredSnippetIdToRoleMap.keys)

        val snippetsWithWarnings =
            snippets.map { snippet ->
                val warningsJson: String =
                    try {
                        if (assetService.exists("lint-warnings", snippet.id)) {
                            val json = assetService.get("lint-warnings", snippet.id)
                            if (json.isNotBlank() && json != "[]" && json != "Search in lint-warnings not found") {
                                json
                            } else {
                                ""
                            }
                        } else {
                            ""
                        }
                    } catch (e: Exception) {
                        // No hay warnings o el asset no existe - esto es normal, no loguear como error
                        ""
                    }

                val warnings =
                    if (warningsJson.isBlank()) {
                        emptyList<String>()
                    } else {
                        try {
                            jacksonObjectMapper().readValue<List<String>>(
                                warningsJson,
                                object : TypeReference<List<String>>() {},
                            )
                        } catch (e: Exception) {
                            // JSON inválido o vacío - solo loguear si realmente hay contenido pero está mal formado
                            if (warningsJson.isNotBlank() && warningsJson != "[]") {
                                println(
                                    "Error deserializing warnings for snippet ${snippet.id}: ${e.message} (content: $warningsJson)",
                                )
                            }
                            emptyList<String>()
                        }
                    }

                SnippetWithRoleAndWarnings(
                    snippet = snippet,
                    role = snippetIdToRoleMap[snippet.id] ?: "Default",
                    warnings = warnings,
                )
            }

        val snippetIdToWarnings = snippetsWithWarnings.associateBy({ it.id }, { it.lintWarnings })

        val filteredSnippets =
            snippets.filter { snippet ->
                (snippetName == null || snippet.name.contains(snippetName, ignoreCase = true)) &&
                    (languages.isNullOrEmpty() || languages.contains(snippet.language.id)) &&
                    (compliance.isNullOrEmpty() || compliance.contains(snippet.status))
            }

        val totalCount = filteredSnippets.size.toLong()

        val pagedSnippets =
            filteredSnippets
                .drop(page * pageSize)
                .take(pageSize)
                .map { snippet ->
                    val role = filteredSnippetIdToRoleMap[snippet.id]!!
                    val warnings = snippetIdToWarnings[snippet.id]!!
                    SnippetWithRoleAndWarnings(snippet, role, warnings)
                }

        return Pair(pagedSnippets, totalCount)
    }

    override fun update(
        id: Long,
        content: String,
        token: String,
    ): FullSnippet {
        checkIfExists(id, "edit")

        // Verificar permisos de edición: Viewer no puede editar
        val userRole = authorizationServiceClient.getUserRoleForSnippet(token, id)
        if (userRole == "Viewer") {
            throw RuntimeException(
                "No tenés permisos para editar este snippet. Solo tenés permisos de lectura (Viewer).",
            )
        }

        val snippet = snippetRepository.findById(id).get()
        assetService.put("snippets", id, content)

        // Publicar mensaje en Redis para que runner-service valide el snippet actualizado
        val userId = authorizationServiceClient.validate(token).body
        if (userId != null) {
            runnerServiceProducer.publishSnippetEvent(
                SnippetMessage(
                    snippetId = snippet.id,
                    userId = userId,
                    version = snippet.language.version,
                    jwtToken = token,
                ),
            )

            // User Story #16: Testing automático - publicar mensaje para ejecutar todos los tests
            try {
                val tests = testService.getTestsBySnippetId(snippet.id)
                tests.forEach { test ->
                    runnerServiceProducer.publishTestEvent(
                        TestMessage(
                            testId = test.id,
                            snippetId = snippet.id,
                            userId = userId,
                            version = snippet.language.version,
                            jwtToken = token,
                            inputs = test.input,
                            outputs = test.output,
                        ),
                    )
                }
            } catch (e: Exception) {
                // Si no hay tests o hay error, continuar sin problemas
                println("No se pudieron ejecutar tests automáticamente: ${e.message}")
            }
        }

        return FullSnippet(snippet, content)
    }

    override fun delete(
        directory: String,
        id: Long,
    ) {
        checkIfExists(id, "delete")
        snippetRepository.deleteById(id)
        assetService.delete(directory, id)
    }

    override fun checkIfExists(
        id: Long,
        operation: String,
    ) {
        if (!snippetRepository.existsById(id)) {
            throw SnippetNotFound("Snippet not found when trying to $operation it")
        }
    }

    fun countSnippets(snippetName: String?): Long {
        return if (!snippetName.isNullOrEmpty()) {
            snippetRepository.countByNameContainingIgnoreCase(snippetName)
        } else {
            snippetRepository.count()
        }
    }

    fun updateStatus(
        id: Long,
        status: Compliance,
    ): FullSnippet {
        val snippet =
            snippetRepository.findById(id).orElseThrow {
                RuntimeException("Snippet with ID $id not found")
            }
        snippet.status = status
        println("HIT snippet service: snippet $id status was updated to $status")

        val updatedSnippet = snippetRepository.save(snippet)
        return FullSnippet(updatedSnippet, assetService.get("snippets", id))
    }

    fun runSnippet(
        id: Long,
        inputs: List<String>,
        token: String,
    ): List<String> {
        val snippet = get(id)

        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", token)
            }

        val body =
            mapOf(
                "version" to snippet.version,
                "code" to snippet.content,
            )

        val entity: HttpEntity<Map<String, Any>> = HttpEntity(body, headers)
        val url = "$runnerServiceUrl/api/printscript/interpret"

        val response =
            restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<List<String>>() {},
            )

        return response.body ?: emptyList()
    }

    fun format(
        id: Long,
        content: String,
        token: String,
    ) {
        val userId = authorizationServiceClient.validate(token).body
        if (userId != null) {
            val snippet = get(id)

            // Publicar mensaje en Redis para que runner-service formatee el snippet
            runnerServiceProducer.publishSnippetEvent(
                SnippetMessage(
                    snippetId = snippet.id,
                    userId = userId,
                    version = snippet.version,
                    jwtToken = token,
                ),
            )
        }
    }
}
