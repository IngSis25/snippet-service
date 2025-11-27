package service

import snippets.config.SnippetMessage
import snippets.dto.response.SnippetUserDto
import snippets.errors.SnippetNotFound
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.ResponseEntity
import snippets.repositories.SnippetRepository
import snippets.service.AssetService
import snippets.service.AuthorizationServiceClient
import snippets.service.LanguageService
import snippets.service.RunnerServiceProducer
import snippets.service.SnippetService
import snippets.service.TestService
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SnippetServiceTest {
    @Mock
    private lateinit var snippetRepository: SnippetRepository

    @Mock
    private lateinit var authorizationServiceClient: AuthorizationServiceClient

    @Mock
    private lateinit var assetService: AssetService

    @Mock
    private lateinit var languageService: LanguageService

    @Mock
    private lateinit var runnerServiceProducer: RunnerServiceProducer

    @Mock
    private lateinit var testService: TestService

    @InjectMocks
    private lateinit var snippetService: SnippetService

    private lateinit var language: Language
    private lateinit var snippet: Snippet

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
    }

    @Test
    fun `get should return full snippet with content and warnings`() {
        // Given
        val snippetId = 1L
        val content = "print('Hello')"
        val warningsJson = """["warning1", "warning2"]"""

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(assetService.get("snippets", snippetId)).thenReturn(content)
        whenever(assetService.exists("lint-warnings", snippetId)).thenReturn(true)
        whenever(assetService.get("lint-warnings", snippetId)).thenReturn(warningsJson)

        // When
        val result = snippetService.get(snippetId)

        // Then
        result.id shouldBeEqualTo snippet.id
        result.content shouldBeEqualTo content
        result.errors.size shouldBeEqualTo 2
        verify(snippetRepository).findById(snippetId)
        verify(assetService).get("snippets", snippetId)
    }

    @Test
    fun `get should return full snippet without warnings when warnings do not exist`() {
        // Given
        val snippetId = 1L
        val content = "print('Hello')"

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(assetService.get("snippets", snippetId)).thenReturn(content)
        whenever(assetService.exists("lint-warnings", snippetId)).thenReturn(false)

        // When
        val result = snippetService.get(snippetId)

        // Then
        result.id shouldBeEqualTo snippet.id
        result.content shouldBeEqualTo content
        result.errors.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `get should throw SnippetNotFound when snippet not found`() {
        // Given
        val snippetId = 999L
        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.empty())

        // When/Then
        try {
            snippetService.get(snippetId)
            org.junit.jupiter.api.Assertions.fail("Should have thrown SnippetNotFound")
        } catch (e: SnippetNotFound) {
            // Expected
        }
        verify(snippetRepository).findById(snippetId)
    }

    @Test
    fun `checkIfExists should throw SnippetNotFound when snippet does not exist`() {
        // Given
        val snippetId = 999L
        whenever(snippetRepository.existsById(snippetId)).thenReturn(false)

        // When/Then
        try {
            snippetService.checkIfExists(snippetId, "test")
            org.junit.jupiter.api.Assertions.fail("Should have thrown SnippetNotFound")
        } catch (e: SnippetNotFound) {
            // Expected
        }
        verify(snippetRepository).existsById(snippetId)
    }

    @Test
    fun `checkIfExists should not throw when snippet exists`() {
        // Given
        val snippetId = 1L
        whenever(snippetRepository.existsById(snippetId)).thenReturn(true)

        // When/Then - should not throw
        snippetService.checkIfExists(snippetId, "test")
        verify(snippetRepository).existsById(snippetId)
    }

    @Test
    fun `delete should delete snippet and asset`() {
        // Given
        val snippetId = 1L
        val directory = "snippets"
        whenever(snippetRepository.existsById(snippetId)).thenReturn(true)

        // When
        snippetService.delete(directory, snippetId)

        // Then
        verify(snippetRepository).existsById(snippetId)
        verify(snippetRepository).deleteById(snippetId)
        verify(assetService).delete(directory, snippetId)
    }

    @Test
    fun `countSnippets should return count by name when name provided`() {
        // Given
        val snippetName = "Test"
        val expectedCount = 5L
        whenever(snippetRepository.countByNameContainingIgnoreCase(snippetName))
            .thenReturn(expectedCount)

        // When
        val result = snippetService.countSnippets(snippetName)

        // Then
        result shouldBeEqualTo expectedCount
        verify(snippetRepository).countByNameContainingIgnoreCase(snippetName)
    }

    @Test
    fun `countSnippets should return total count when name is null`() {
        // Given
        val expectedCount = 10L
        whenever(snippetRepository.count()).thenReturn(expectedCount)

        // When
        val result = snippetService.countSnippets(null)

        // Then
        result shouldBeEqualTo expectedCount
        verify(snippetRepository).count()
    }

    @Test
    fun `countSnippets should return total count when name is empty`() {
        // Given
        val expectedCount = 10L
        whenever(snippetRepository.count()).thenReturn(expectedCount)

        // When
        val result = snippetService.countSnippets("")

        // Then
        result shouldBeEqualTo expectedCount
        verify(snippetRepository).count()
    }

    @Test
    fun `getFilteredSnippets should return empty list when snippetsIds is empty`() {
        // Given
        val snippetsIds = emptyList<SnippetUserDto>()

        // When
        val result = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, null)

        // Then
        result.first.isEmpty() shouldBeEqualTo true
        result.second shouldBeEqualTo 0L
        verify(snippetRepository, never()).findAllById(any())
    }

    @Test
    fun `update should update snippet and publish events`() {
        // Given
        val snippetId = 1L
        val content = "print('Updated')"
        val token = "test-token"
        val userId = 1L

        whenever(snippetRepository.existsById(snippetId)).thenReturn(true)
        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(authorizationServiceClient.validate(token))
            .thenReturn(ResponseEntity.ok(userId))
        whenever(testService.getTestsBySnippetId(snippet.id)).thenReturn(emptyList())

        // When
        val result = snippetService.update(snippetId, content, token)

        // Then
        result.content shouldBeEqualTo content
        verify(snippetRepository).existsById(snippetId)
        verify(assetService).put("snippets", snippetId, content)
        verify(runnerServiceProducer).publishSnippetEvent(any<SnippetMessage>())
    }

    @Test
    fun `format should publish format event`() {
        // Given
        val snippetId = 1L
        val content = "print('Hello')"
        val token = "test-token"
        val userId = 1L

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(assetService.get("snippets", snippetId)).thenReturn(content)
        whenever(assetService.exists("lint-warnings", snippetId)).thenReturn(false)
        whenever(authorizationServiceClient.validate(token))
            .thenReturn(ResponseEntity.ok(userId))

        // When
        snippetService.format(snippetId, content, token)

        // Then
        verify(authorizationServiceClient).validate(token)
        verify(runnerServiceProducer).publishSnippetEvent(any<SnippetMessage>())
    }
}
