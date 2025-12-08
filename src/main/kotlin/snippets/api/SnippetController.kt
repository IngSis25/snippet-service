package snippets.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import snippets.dto.request.ContentRequest
import snippets.dto.request.ShareRequest
import snippets.dto.request.SnippetRequest
import snippets.dto.response.FullSnippet
import snippets.errors.LanguageNotFound
import snippets.model.Compliance
import snippets.service.AuthorizationServiceClient
import snippets.service.LanguageService
import snippets.service.SnippetService

@RestController
@RequestMapping("/api/snippets")
class SnippetController(
    private val snippetService: SnippetService,
    private val authorizationServiceClient: AuthorizationServiceClient,
    private val languageService: LanguageService,
) {
    @GetMapping("/user")
    fun getSnippetsOfUser(
        @RequestParam page: Int = 0,
        @RequestParam pageSize: Int = 10,
        @RequestParam userId: String,
        @RequestParam(required = false) snippetName: String? = null,
        @RequestParam(required = false) roles: List<String>? = null,
        @RequestParam(required = false) languages: List<Long>? = null,
        @RequestParam(required = false) compliance: List<Compliance>? = null,
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<Map<String, Any>> {
        val snippetsIds = authorizationServiceClient.getSnippetsOfUser(token, userId)
        val (snippets, totalCount) =
            snippetService.getFilteredSnippets(
                page,
                pageSize,
                snippetsIds,
                snippetName,
                roles,
                languages,
                compliance,
            )
        return ResponseEntity.ok(mapOf("snippets" to snippets, "count" to totalCount))
    }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): ResponseEntity<FullSnippet> {
        val fullSnippet = snippetService.get(id)
        return ResponseEntity.ok(fullSnippet)
    }

    @PostMapping
    fun create(
        @RequestBody snippetRequest: SnippetRequest,
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<FullSnippet> {
        println("===== SnippetController.create() =====")
        println("Request recibido: $snippetRequest")

        // Resolver languageId si no viene directamente
        val languageId =
            snippetRequest.languageId ?: run {
                println("languageId no vino en el request, intentando resolver por nombre o extensión...")

                val language =
                    when {
                        !snippetRequest.language.isNullOrBlank() -> {
                            println("Buscando lenguaje por nombre: '${snippetRequest.language}'")
                            languageService.getLanguageByName(snippetRequest.language)
                                ?: throw LanguageNotFound("Language with name '${snippetRequest.language}' not found")
                        }
                        !snippetRequest.extension.isNullOrBlank() -> {
                            println("Buscando lenguaje por extension: '${snippetRequest.extension}'")
                            languageService.getLanguageByExtension(snippetRequest.extension)
                                ?: throw LanguageNotFound(
                                    "Language with extension '${snippetRequest.extension}' not found",
                                )
                        }
                        else -> {
                            throw LanguageNotFound("Either languageId, language, or extension must be provided")
                        }
                    }

                println("Lenguaje encontrado: id=${language.id}, name=${language.name}, ext=${language.extension}")
                language.id.toString()
            }

        println("languageId final resuelto = $languageId")
        println("Llamando a SnippetService.create()...")

        val fullSnippet =
            snippetService.create(
                snippetRequest.name,
                snippetRequest.content,
                languageId,
                snippetRequest.owner,
                token,
            )

        println("Snippet creado correctamente: id=${fullSnippet.id}")

        return ResponseEntity.ok(fullSnippet)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody req: ContentRequest,
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<FullSnippet> {
        val response = snippetService.update(id, req.content, token)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/delete/{id}")
    fun delete(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        snippetService.delete("snippets", id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/share/{id}")
    fun share(
        @RequestHeader("Authorization") token: String,
        @PathVariable id: Long,
        @RequestBody emails: ShareRequest,
    ): ResponseEntity<FullSnippet> {
        val snippet =
            try {
                snippetService.get(id)
            } catch (e: Exception) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .header("Share-Status", "Snippet not found while trying to share it")
                    .body(FullSnippet())
            }

        return authorizationServiceClient.shareSnippet(token, id, emails.fromEmail, emails.toEmail, snippet)
    }

    @PostMapping("/format/{id}")
    fun format(
        @RequestHeader("Authorization") token: String,
        @PathVariable id: Long,
        @RequestBody body: Map<String, String>,
    ): ResponseEntity<String> {
        val content = body["content"] ?: return ResponseEntity.badRequest().body("Content field is required.")
        snippetService.format(id, content, token)
        return ResponseEntity.ok("Format request published to runner-service")
    }

    @PutMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody status: Compliance,
    ): ResponseEntity<Void> {
        try {
            snippetService.updateStatus(id, status)
            return ResponseEntity.noContent().build()
        } catch (e: Exception) {
            println("Error updating snippet status: ${e.message}")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @GetMapping("/{id}/download")
    fun downloadSnippet(
        @PathVariable id: Long,
    ): ResponseEntity<Map<String, String>> {
        val snippet = snippetService.get(id)
        return ResponseEntity.ok(
            mapOf(
                "name" to snippet.name,
                "content" to snippet.content,
                "language" to snippet.language,
                "version" to snippet.version,
            ),
        )
    }

    @GetMapping("/{id}/download/formatted")
    fun downloadFormattedSnippet(
        @PathVariable id: Long,
    ): ResponseEntity<Map<String, String>> {
        // Por ahora devuelve el mismo contenido, el formateo se hace en runner-service
        // y debería actualizarse en el snippet cuando runner-service termine
        val snippet = snippetService.get(id)
        return ResponseEntity.ok(
            mapOf(
                "name" to snippet.name,
                "content" to snippet.content,
                "language" to snippet.language,
                "version" to snippet.version,
            ),
        )
    }

    /**
     * Endpoint de prueba para testing de New Relic.
     * Devuelve un error HTTP 500 para poder disparar alertas.
     */
    @GetMapping("/test-error")
    fun testError(): ResponseEntity<Map<String, String>> {
        throw RuntimeException("Error simulado para testing de New Relic - HTTP 500")
    }
}
