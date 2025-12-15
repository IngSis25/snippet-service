package snippets.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
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

data class TestExecutionRequestDTO(
    val version: String,
    val code: String,
    val inputs: List<String>? = null,
    val expectedOutputs: List<String>? = null,
)

data class TestExecutionResultDTO(
    val status: String,
    val errors: List<String> = emptyList(),
)

data class TestRunResult(
    val status: String,
    val errors: List<String> = emptyList(),
)

@Service
class TestService(
    private val testRepository: TestRepository,
    private val snippetRepository: SnippetRepository,
    private val runnerServiceProducer: RunnerServiceProducer,
    private val assetService: AssetService,
    private val restTemplate: RestTemplate,
    @Value("\${runner.service.url}") private val runnerServiceUrl: String,
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

    /**
     * Execute a test synchronously using the new direct endpoint in runner-service.
     * This method replaces the old async flow with Redis Streams and polling.
     *
     * @param testId the ID of the test to execute
     * @param token the JWT token for authentication with runner-service
     * @return TestRunResult with status ("success" or "fail") and error messages
     */
    fun runTestSync(
        testId: Long,
        token: String,
    ): TestRunResult {
        val test = getTestById(testId)
        val snippet = test.snippet

        // Get the snippet code from asset-service
        val code = assetService.get("snippets", snippet.id)
        if (code.isBlank()) {
            return TestRunResult(
                status = "fail",
                errors = listOf("Snippet code is empty"),
            )
        }

        // Prepare request for runner-service
        val request =
            TestExecutionRequestDTO(
                version = snippet.language.version,
                code = code,
                inputs = test.input,
                expectedOutputs = test.output,
            )

        // Call runner-service synchronously with authentication
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", token)
            }

        val entity = HttpEntity(request, headers)
        val url = "$runnerServiceUrl/internal/tests/run"

        try {
            val response =
                restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    TestExecutionResultDTO::class.java,
                )

            val result = response.body
            if (result != null) {
                return TestRunResult(
                    status = if (result.status == "PASSED") "success" else "fail",
                    errors = result.errors,
                )
            } else {
                return TestRunResult(
                    status = "fail",
                    errors = listOf("No response from runner-service"),
                )
            }
        } catch (e: Exception) {
            return TestRunResult(
                status = "fail",
                errors = listOf("Error calling runner-service: ${e.message}"),
            )
        }
    }

    /**
     * Execute a test asynchronously using Redis Streams (for run-all tests).
     * This method is kept for backward compatibility and for run-all functionality.
     */
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

    /**
     * Get test result from asset-service (used for async flow).
     * This method is kept for backward compatibility with run-all tests.
     */
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
            }
            Thread.sleep(500)
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
