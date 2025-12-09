package snippets.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import snippets.dto.response.TestDTO
import snippets.service.AuthorizationServiceClient
import snippets.service.TestService

@RestController
@RequestMapping("/api/tests")
class TestController(
    private val testService: TestService,
    private val authorizationServiceClient: AuthorizationServiceClient,
) {
    @GetMapping("/snippet/{snippetId}")
    fun getTestsBySnippetId(
        @PathVariable snippetId: Long,
    ): ResponseEntity<List<TestDTO>> {
        val tests = testService.getTestsBySnippetId(snippetId)
        return ResponseEntity.ok(tests.map { TestDTO(it) })
    }

    @PostMapping("/snippet/{snippetId}")
    fun addTestToSnippet(
        @PathVariable snippetId: Long,
        @RequestBody testBody: Map<String, Any>,
    ): ResponseEntity<TestDTO> {
        val testDTO =
            testService.addTestToSnippet(
                snippetId,
                name = testBody["name"] as? String ?: "",
                input = testBody["input"] as? List<String> ?: listOf(),
                output = testBody["output"] as? List<String> ?: listOf(),
            )
        return ResponseEntity.ok(testDTO)
    }

    @DeleteMapping("/{id}")
    fun deleteTestById(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        testService.deleteTestById(id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/run")
    fun runTest(
        @RequestHeader("Authorization") token: String,
        @PathVariable id: Long,
    ): ResponseEntity<String> {
        val userId =
            authorizationServiceClient.validate(token).body
                ?: return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body("fail")
        try {
            val test = testService.getTestById(id)
            val snippetId = test.snippet.id

            // Publicar el test para ejecución
            testService.executeTest(token, id, userId)

            // Esperar y obtener el resultado real del test
            val result = testService.getTestResult(id, snippetId, maxWaitSeconds = 10)
            return ResponseEntity.ok(result)
        } catch (e: Exception) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body("fail")
        }
    }

    @PostMapping("/snippet/{snippetId}/run-all")
    fun runAllTests(
        @RequestHeader("Authorization") token: String,
        @PathVariable snippetId: Long,
    ): ResponseEntity<String> {
        val userId =
            authorizationServiceClient.validate(token).body
                ?: return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body("Invalid token")
        testService.executeAllSnippetTests(token, snippetId, userId)
        return ResponseEntity.ok("All tests execution requests published to runner-service")
    }
}
