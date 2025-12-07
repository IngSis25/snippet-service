package service

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import snippets.config.TestMessage
import snippets.errors.SnippetNotFound
import snippets.errors.TestNotFound
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet
import snippets.repositories.SnippetRepository
import snippets.repositories.TestRepository
import snippets.service.RunnerServiceProducer
import snippets.service.TestService
import java.util.Optional
import snippets.model.Test as TestModel

@ExtendWith(MockitoExtension::class)
class TestServiceTest {
    @Mock
    private lateinit var testRepository: TestRepository

    @Mock
    private lateinit var snippetRepository: SnippetRepository

    @Mock
    private lateinit var runnerServiceProducer: RunnerServiceProducer

    @InjectMocks
    private lateinit var testService: TestService

    private lateinit var language: Language
    private lateinit var snippet: Snippet
    private lateinit var test: TestModel

    @BeforeEach
    fun setUp() {
        language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        snippet =
            Snippet(
                id = 1L,
                name = "Test Snippet",
                owner = "test@example.com",
                status = Compliance.PENDING,
                language = language,
            )
        test =
            TestModel(
                id = 1L,
                name = "Test 1",
                input = listOf("input1"),
                output = listOf("output1"),
                snippet = snippet,
            )
    }

    @Test
    fun `getTestsBySnippetId should return list of tests`() {
        // Given
        val snippetId = 1L
        val tests = listOf(test)
        whenever(testRepository.findBySnippetId(snippetId)).thenReturn(tests)

        // When
        val result = testService.getTestsBySnippetId(snippetId)

        // Then
        result shouldBeEqualTo tests
        verify(testRepository).findBySnippetId(snippetId)
    }

    @Test
    fun `getTestById should return test when found`() {
        // Given
        val testId = 1L
        whenever(testRepository.findById(testId)).thenReturn(Optional.of(test))

        // When
        val result = testService.getTestById(testId)

        // Then
        result shouldBeEqualTo test
        verify(testRepository).findById(testId)
    }

    @Test
    fun `getTestById should throw TestNotFound when test not found`() {
        // Given
        val testId = 999L
        whenever(testRepository.findById(testId)).thenReturn(Optional.empty())

        // When/Then
        try {
            testService.getTestById(testId)
            org.junit.jupiter.api.Assertions.fail("Should have thrown TestNotFound")
        } catch (e: TestNotFound) {
            // Expected
        }
        verify(testRepository).findById(testId)
    }

    @Test
    fun `addTestToSnippet should throw SnippetNotFound when snippet not found`() {
        // Given
        val snippetId = 999L
        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.empty())

        // When/Then
        try {
            testService.addTestToSnippet(snippetId, "Test", listOf(), listOf())
            org.junit.jupiter.api.Assertions.fail("Should have thrown SnippetNotFound")
        } catch (e: SnippetNotFound) {
            // Expected
        }
        verify(snippetRepository).findById(snippetId)
        verify(testRepository, never()).save(any())
    }

    @Test
    fun `deleteTestById should delete test when exists`() {
        // Given
        val testId = 1L
        whenever(testRepository.existsById(testId)).thenReturn(true)

        // When
        testService.deleteTestById(testId)

        // Then
        verify(testRepository).existsById(testId)
        verify(testRepository).deleteById(testId)
    }

    @Test
    fun `deleteTestById should throw TestNotFound when test does not exist`() {
        // Given
        val testId = 999L
        whenever(testRepository.existsById(testId)).thenReturn(false)

        // When/Then
        try {
            testService.deleteTestById(testId)
            org.junit.jupiter.api.Assertions.fail("Should have thrown TestNotFound")
        } catch (e: TestNotFound) {
            // Expected
        }
        verify(testRepository).existsById(testId)
        verify(testRepository, never()).deleteById(testId)
    }

    @Test
    fun `executeTest should publish test event`() {
        // Given
        val testId = 1L
        val token = "test-token"
        val userId = "auth0|123"

        whenever(testRepository.findById(testId)).thenReturn(Optional.of(test))

        // When
        testService.executeTest(token, testId, userId)

        // Then
        verify(testRepository).findById(testId)
        verify(runnerServiceProducer).publishTestEvent(any<TestMessage>())
    }

    @Test
    fun `executeAllSnippetTests should publish test events for all tests`() {
        // Given
        val snippetId = 1L
        val token = "test-token"
        val userId = "auth0|123"
        val tests = listOf(test, test.copy(id = 2L))

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(testRepository.findBySnippetId(snippetId)).thenReturn(tests)

        // When
        testService.executeAllSnippetTests(token, snippetId, userId)

        // Then
        verify(snippetRepository).findById(snippetId)
        verify(testRepository).findBySnippetId(snippetId)
        verify(runnerServiceProducer, times(2)).publishTestEvent(any<TestMessage>())
    }

    @Test
    fun `executeAllSnippetTests should throw SnippetNotFound when snippet not found`() {
        // Given
        val snippetId = 999L
        val token = "test-token"
        val userId = "auth0|123"

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.empty())

        // When/Then
        try {
            testService.executeAllSnippetTests(token, snippetId, userId)
            org.junit.jupiter.api.Assertions.fail("Should have thrown SnippetNotFound")
        } catch (e: SnippetNotFound) {
            // Expected
        }
        verify(snippetRepository).findById(snippetId)
        verify(runnerServiceProducer, never()).publishTestEvent(any<TestMessage>())
    }
}
