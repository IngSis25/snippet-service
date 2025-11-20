package api

import dto.response.TestDTO
import service.TestService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tests")
class TestController(
    private val testService: TestService
) {
    @GetMapping("/snippet/{snippetId}")
    fun getTestsBySnippetId(@PathVariable snippetId: Long): ResponseEntity<List<TestDTO>> {
        val tests = testService.getTestsBySnippetId(snippetId)
        return ResponseEntity.ok(tests.map { TestDTO(it) })
    }

    @PostMapping("/snippet/{snippetId}")
    fun addTestToSnippet(
        @PathVariable snippetId: Long,
        @RequestBody testBody: Map<String, Any>
    ): ResponseEntity<TestDTO> {
        val testDTO = testService.addTestToSnippet(
            snippetId,
            name = testBody["name"] as? String ?: "",
            input = testBody["input"] as? List<String> ?: listOf(),
            output = testBody["output"] as? List<String> ?: listOf()
        )
        return ResponseEntity.ok(testDTO)
    }

    @DeleteMapping("/{id}")
    fun deleteTestById(@PathVariable id: Long): ResponseEntity<Void> {
        testService.deleteTestById(id)
        return ResponseEntity.noContent().build()
    }
}

