package snippets.service

import org.springframework.stereotype.Service
import snippets.config.TestMessage
import snippets.dto.response.TestDTO
import snippets.errors.SnippetNotFound
import snippets.errors.TestNotFound
import snippets.model.Test
import snippets.repositories.SnippetRepository
import snippets.repositories.TestRepository

@Service
class TestService(
    private val testRepository: TestRepository,
    private val snippetRepository: SnippetRepository,
    private val runnerServiceProducer: RunnerServiceProducer,
) {
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
