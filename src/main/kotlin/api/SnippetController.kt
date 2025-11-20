package api

import dto.request.ContentRequest
import dto.request.ShareRequest
import dto.request.SnippetRequest
import dto.response.FullSnippet
import model.Compliance
import service.AuthorizationServiceClient
import service.SnippetService
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

@RestController
@RequestMapping("/api/snippets")
class SnippetController(
    private val snippetService: SnippetService,
    private val authorizationServiceClient: AuthorizationServiceClient
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
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Map<String, Any>> {
        val snippetsIds = authorizationServiceClient.getSnippetsOfUser(token, userId)
        val (snippets, totalCount) = snippetService.getFilteredSnippets(
            page, pageSize, snippetsIds, snippetName, roles, languages, compliance
        )
        return ResponseEntity.ok(mapOf("snippets" to snippets, "count" to totalCount))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<FullSnippet> {
        val fullSnippet = snippetService.get(id)
        return ResponseEntity.ok(fullSnippet)
    }

    @PostMapping("/")
    fun create(
        @RequestBody snippetRequest: SnippetRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<FullSnippet> {
        val fullSnippet = snippetService.create(
            snippetRequest.name,
            snippetRequest.content,
            snippetRequest.languageId,
            snippetRequest.owner,
            token
        )
        return ResponseEntity.ok(fullSnippet)
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @RequestBody req: ContentRequest,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<FullSnippet> {
        val response = snippetService.update(id, req.content, token)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/delete/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        snippetService.delete("snippets", id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/share/{id}")
    fun share(
        @RequestHeader("Authorization") token: String,
        @PathVariable id: Long,
        @RequestBody emails: ShareRequest
    ): ResponseEntity<FullSnippet> {
        val snippet = try {
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
        @RequestBody body: Map<String, String>
    ): ResponseEntity<String> {
        val content = body["content"] ?: return ResponseEntity.badRequest().body("Content field is required.")
        snippetService.format(id, content, token)
        return ResponseEntity.ok("Format request published to runner-service")
    }

    @PutMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody status: Compliance
    ): ResponseEntity<Void> {
        try {
            snippetService.updateStatus(id, status)
            return ResponseEntity.noContent().build()
        } catch (e: Exception) {
            println("Error updating snippet status: ${e.message}")
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}

