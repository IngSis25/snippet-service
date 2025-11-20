package service

import dto.response.TestDTO
import errors.SnippetNotFound
import errors.TestNotFound
import model.Test
import repositories.SnippetRepository
import repositories.TestRepository
import org.springframework.stereotype.Service

@Service
class TestService(
    private val testRepository: TestRepository,
    private val snippetRepository: SnippetRepository
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
}

