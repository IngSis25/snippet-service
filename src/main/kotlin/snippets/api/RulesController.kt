package snippets.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import snippets.dto.request.DiagnosticDto
import snippets.dto.request.Rule
import snippets.dto.request.SaveRulesReq
import snippets.service.RulesService
import snippets.service.SnippetService

@RestController
@RequestMapping("/api/rules")
class RulesController(
    private val rulesService: RulesService,
    private val snippetService: SnippetService,
) {
    /**
     * GET /api/rules/format
     * Obtiene las reglas de formato del usuario autenticado
     */
    @GetMapping("/format")
    fun getFormatRules(
        @RequestHeader("Authorization") token: String,
        @RequestParam(defaultValue = "1.1") version: String,
    ): ResponseEntity<List<Rule>> {
        val rules = rulesService.getFormatRules(token, version)
        return ResponseEntity.ok(rules)
    }

    /**
     * GET /api/rules/lint
     * Obtiene las reglas de linting del usuario autenticado
     */
    @GetMapping("/lint")
    fun getLintRules(
        @RequestHeader("Authorization") token: String,
        @RequestParam(defaultValue = "1.1") version: String,
    ): ResponseEntity<List<Rule>> {
        val rules = rulesService.getLintRules(token, version)
        return ResponseEntity.ok(rules)
    }

    /**
     * POST /api/rules/format
     * Guarda y publica reglas de formato (body: SaveRulesReq)
     */
    @PostMapping("/format")
    fun saveFormatRules(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: SaveRulesReq,
    ): ResponseEntity<List<Rule>> {
        val rules = rulesService.saveFormatRules(token, request)
        return ResponseEntity.ok(rules)
    }

    /**
     * POST /api/rules/lint
     * Guarda y publica reglas de linting (body: SaveRulesReq)
     */
    @PostMapping("/lint")
    fun saveLintRules(
        @RequestHeader("Authorization") token: String,
        @RequestBody request: SaveRulesReq,
    ): ResponseEntity<List<Rule>> {
        val rules = rulesService.saveLintRules(token, request)
        return ResponseEntity.ok(rules)
    }
}

@RestController
@RequestMapping("/api/snippets")
class SnippetRulesController(
    private val rulesService: RulesService,
    private val snippetService: SnippetService,
) {
    /**
     * POST /api/snippets/run/{id}/format
     * Formatea un snippet usando las reglas del usuario (síncrono)
     */
    @PostMapping("/run/{id}/format")
    fun formatSnippet(
        @PathVariable id: Long,
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<Map<String, String>> {
        return try {
            val formattedContent = rulesService.formatSnippet(token, id)
            ResponseEntity.ok(mapOf("content" to formattedContent))
        } catch (e: Exception) {
            println("ERROR in formatSnippet endpoint: ${e.message}")
            e.printStackTrace()
            ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }

    /**
     * POST /api/snippets/run/{id}/lint
     * Lint de un snippet usando las reglas del usuario (síncrono)
     * Devuelve SnippetDetailDto con lintCount, isValid, compliance
     */
    @PostMapping("/run/{id}/lint")
    fun lintSnippet(
        @PathVariable id: Long,
        @RequestHeader("Authorization") token: String,
    ): ResponseEntity<snippets.dto.response.SnippetDetailDto> {
        return try {
            val snippetDetail = rulesService.lintSnippetSync(token, id)
            ResponseEntity.ok(snippetDetail)
        } catch (e: Exception) {
            println("ERROR in lintSnippet endpoint: ${e.message}")
            e.printStackTrace()
            ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .build()
        }
    }
}

@RestController
@RequestMapping("/api/internal/snippets")
class InternalSnippetRulesController(
    private val rulesService: RulesService,
) {
    /**
     * POST /api/internal/snippets/{id}/format
     * Guarda resultado de formato (usado por workers asíncronos)
     */
    @PostMapping("/{id}/format")
    fun saveFormatResult(
        @PathVariable id: Long,
        @RequestBody body: Map<String, String>,
    ): ResponseEntity<Map<String, String>> {
        val formattedContent =
            body["content"]
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "Content field is required"))

        rulesService.saveFormatResult(id, formattedContent)
        return ResponseEntity.ok(mapOf("message" to "Format result saved"))
    }

    /**
     * POST /api/internal/snippets/{id}/lint
     * Guarda resultado de lint (usado por workers asíncronos)
     * Acepta List<DiagnosticDto> o Map<String, String> para compatibilidad
     */
    @PostMapping("/{id}/lint")
    fun saveLintResult(
        @PathVariable id: Long,
        @RequestBody body: Any,
    ): ResponseEntity<Map<String, String>> {
        val warningsJson: String =
            try {
                when (body) {
                    is List<*> -> {
                        // Si es una lista de DiagnosticDto, convertir a JSON
                        val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                        objectMapper.writeValueAsString(body)
                    }
                    is Map<*, *> -> {
                        // Compatibilidad con formato anterior
                        val map = body as Map<String, Any>
                        map["warnings"] as? String
                            ?: map["content"] as? String
                            ?: return ResponseEntity.badRequest()
                                .body(mapOf("error" to "Warnings field is required"))
                    }
                    else -> {
                        return ResponseEntity.badRequest()
                            .body(mapOf("error" to "Invalid request body format"))
                    }
                }
            } catch (e: Exception) {
                return ResponseEntity.badRequest()
                    .body(mapOf("error" to "Error processing request: ${e.message}"))
            }

        rulesService.saveLintResult(id, warningsJson)
        return ResponseEntity.ok(mapOf("message" to "Lint result saved"))
    }
}
