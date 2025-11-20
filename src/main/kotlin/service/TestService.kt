package service

import config.TestMessage
import dto.response.TestDTO
import errors.SnippetNotFound
import errors.TestNotFound
import kotlinx.coroutines.runBlocking
import model.Test
import repositories.SnippetRepository
import repositories.TestRepository
import org.springframework.stereotype.Service

@Service
class TestService(
    private val testRepository: TestRepository,
    private val snippetRepository: SnippetRepository,
    private val runnerServiceProducer: RunnerServiceProducer
) {

    fun getTestsBySnippetId(snippetId: Long): List<Test> {
        return testRepository.findBySnippetId(snippetId)
    }

    fun getTestById(id: Long): Test {
        return testRepository.findById(id)
            .orElseThrow { TestNotFound("Test not found") }
    }

    fun addTestToSnippet(snippetId: Long, name: String, input: List<String>, output: List<String>): TestDTO {
        val snippet = snippetRepository.findById(snippetId)
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

    fun executeTest(token: String, testId: Long, userId: Long) {
        val test = getTestById(testId)
        val snippet = test.snippet
        
        // Publicar mensaje en Redis para que runner-service ejecute el test
        runBlocking {
            runnerServiceProducer.publishEvent(
                TestMessage(
                    testId = testId,
                    snippetId = snippet.id,
                    userId = userId,
                    version = snippet.language.version,
                    jwtToken = token,
                    inputs = test.input,
                    outputs = test.output
                )
            )
        }
    }

    fun executeAllSnippetTests(token: String, snippetId: Long, userId: Long) {
        val tests = getTestsBySnippetId(snippetId)
        val snippet = snippetRepository.findById(snippetId)
            .orElseThrow { SnippetNotFound("Snippet not found") }
        
        // Publicar mensaje en Redis para cada test
        runBlocking {
            tests.forEach { test ->
                runnerServiceProducer.publishEvent(
                    TestMessage(
                        testId = test.id,
                        snippetId = snippetId,
                        userId = userId,
                        version = snippet.language.version,
                        jwtToken = token,
                        inputs = test.input,
                        outputs = test.output
                    )
                )
            }
        }
    }
}

