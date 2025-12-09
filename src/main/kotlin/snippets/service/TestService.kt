package snippets.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.stereotype.Service
import snippets.config.TestMessage
import snippets.dto.response.TestDTO
import snippets.errors.SnippetNotFound
import snippets.errors.TestNotFound
import snippets.model.Test
import snippets.repositories.SnippetRepository
import snippets.repositories.TestRepository
import java.time.Duration
import java.time.Instant

data class TestResult(
    val testId: Long,
    val status: String,
    val errors: List<String>,
    val executedAt: Instant,
)

@Service
class TestService(
    private val testRepository: TestRepository,
    private val snippetRepository: SnippetRepository,
    private val runnerServiceProducer: RunnerServiceProducer,
    private val assetService: AssetService,
) {
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    fun getTestsBySnippetId(snippetId: Long): List<Test> {
        return testRepository.findBySnippetId(snippetId)
    }

    fun getTestById(id: Long): Test {
        return testRepository.findById(id)
            .orElseThrow { TestNotFound("Test not found") }
    }

    fun addTestToSnippet(
        snippetId: Long,
        name: String,
        input: List<String>,
        output: List<String>,
    ): TestDTO {
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { SnippetNotFound("Snippet not found") }
        val test = Test(name = name, input = input, output = output, snippet = snippet)
        testRepository.save(test)
        return TestDTO(test)
    }

    fun deleteTestById(id: Long) {
        if (!testRepository.existsById(id)) {
            throw TestNotFound("Test not found")
        }
        testRepository.deleteById(id)
    }

    fun executeTest(
        token: String,
        testId: Long,
        userId: String,
    ) {
        val test = getTestById(testId)
        val snippet = test.snippet

        // Publicar mensaje en Redis para que runner-service ejecute el test
        runnerServiceProducer.publishTestEvent(
            TestMessage(
                testId = testId,
                snippetId = snippet.id,
                userId = userId,
                version = snippet.language.version,
                jwtToken = token,
                inputs = test.input,
                outputs = test.output,
            ),
        )
    }

    fun getTestResult(
        testId: Long,
        snippetId: Long,
        maxWaitSeconds: Int = 10,
    ): String {
        val startTime = Instant.now()
        val maxWait = Duration.ofSeconds(maxWaitSeconds.toLong())

        while (Duration.between(startTime, Instant.now()) < maxWait) {
            try {
                val resultsJson = assetService.get("test-results", snippetId)
                // Verificar que la respuesta sea válida (no vacía, no mensaje de error)
                if (resultsJson.isNotBlank() &&
                    resultsJson != "[]" &&
                    !resultsJson.contains("not found") &&
                    !resultsJson.contains("Search in")
                ) {
                    val results: List<TestResult> =
                        objectMapper.readValue(
                            resultsJson,
                            object : TypeReference<List<TestResult>>() {},
                        )
                    val testResult = results.find { it.testId == testId }
                    if (testResult != null) {
                        return if (testResult.status == "PASSED") "success" else "fail"
                    }
                }
            } catch (e: Exception) {
                // Continuar intentando - el resultado aún no está disponible
            }
            Thread.sleep(500) // Esperar 500ms antes de intentar de nuevo
        }

        // Si no se encontró el resultado después del timeout, retornar fail
        return "fail"
    }

    fun executeAllSnippetTests(
        token: String,
        snippetId: Long,
        userId: String,
    ) {
        val tests = getTestsBySnippetId(snippetId)
        val snippet =
            snippetRepository.findById(snippetId)
                .orElseThrow { SnippetNotFound("Snippet not found") }

        // Publicar mensaje en Redis para cada test
        tests.forEach { test ->
            runnerServiceProducer.publishTestEvent(
                TestMessage(
                    testId = test.id,
                    snippetId = snippetId,
                    userId = userId,
                    version = snippet.language.version,
                    jwtToken = token,
                    inputs = test.input,
                    outputs = test.output,
                ),
            )
        }
    }
}
