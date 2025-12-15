package service

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import snippets.dto.response.SnippetUserDto
import snippets.errors.SnippetNotFound
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet
import snippets.repositories.SnippetRepository
import snippets.service.AssetService
import snippets.service.AuthorizationServiceClient
import snippets.service.LanguageService
import snippets.service.RunnerServiceProducer
import snippets.service.SnippetService
import snippets.service.TestService
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SnippetServiceAdditionalTest {
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

    @Mock
    private lateinit var restTemplate: RestTemplate

    private lateinit var snippetService: SnippetService

    private lateinit var language: Language
    private lateinit var snippet: Snippet

    @BeforeEach
    fun setUp() {
        snippetService =
            SnippetService(
                snippetRepository,
                authorizationServiceClient,
                assetService,
                languageService,
                runnerServiceProducer,
                testService,
                restTemplate,
                "http://localhost:8000",
            )
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
    fun `getFilteredSnippets should return filtered snippets with roles`() {
        // Given
        val snippetsIds =
            listOf(
                SnippetUserDto(snippetId = 1L, role = "Owner"),
                SnippetUserDto(snippetId = 2L, role = "Guest"),
            )
        val snippet2 = snippet.copy(id = 2L, name = "Snippet 2")
        whenever(snippetRepository.findAllById(any<Collection<Long>>())).thenReturn(listOf(snippet, snippet2))
        whenever(assetService.exists(any(), any())).thenReturn(false)
        // assetService.get is not called when exists returns false

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, null)

        // Then
        assert(result.size == 2)
        assert(count == 2L)
    }

    @Test
    fun `getFilteredSnippets should filter by snippet name`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        whenever(snippetRepository.findAllById(any<Collection<Long>>())).thenReturn(listOf(snippet))
        whenever(assetService.exists(any(), any())).thenReturn(false)
        // assetService.get is not called when exists returns false

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 10, snippetsIds, "Test", null, null, null)

        // Then
        assert(result.size == 1)
        assert(count == 1L)
    }

    @Test
    fun `getFilteredSnippets should filter by roles`() {
        // Given
        val snippetsIds =
            listOf(
                SnippetUserDto(snippetId = 1L, role = "Owner"),
                SnippetUserDto(snippetId = 2L, role = "Guest"),
            )
        val snippet2 = snippet.copy(id = 2L)
        // Mock findAllById to return snippets based on the filtered IDs (only Owner role snippets, which is snippet with id=1L)
        whenever(snippetRepository.findAllById(any<Collection<Long>>())).thenAnswer { invocation ->
            val ids = invocation.arguments[0] as Collection<Long>
            // After filtering by roles, only snippet with id=1L (Owner) should be in the collection
            listOf(snippet).filter { it.id in ids }
        }
        whenever(assetService.exists(any(), any())).thenReturn(false)
        // assetService.get is not called when exists returns false

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, listOf("Owner"), null, null)

        // Then
        assert(result.size == 1)
        assert(result[0].role == "Owner")
    }

    @Test
    fun `getFilteredSnippets should return empty when no snippets match`() {
        // Given
        val snippetsIds = emptyList<SnippetUserDto>()

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, null)

        // Then
        assert(result.isEmpty())
        assert(count == 0L)
    }

    @Test
    fun `update should update snippet content`() {
        // Given
        val content = "updated content"
        whenever(snippetRepository.existsById(any())).thenReturn(true)
        whenever(snippetRepository.findById(any())).thenReturn(Optional.of(snippet))
        whenever(authorizationServiceClient.getUserRoleForSnippet(any(), any())).thenReturn("Owner")
        whenever(authorizationServiceClient.validate(any())).thenReturn(ResponseEntity.ok("user123"))
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")
        whenever(testService.getTestsBySnippetId(any())).thenReturn(emptyList())

        // When
        val result = snippetService.update(1L, content, "token")

        // Then
        verify(assetService).put(any(), any(), any())
        verify(runnerServiceProducer).publishSnippetEvent(any())
    }

    @Test
    fun `update should throw when user is Viewer`() {
        // Given
        whenever(snippetRepository.existsById(any())).thenReturn(true)
        whenever(authorizationServiceClient.getUserRoleForSnippet(any(), any())).thenReturn("Viewer")

        // When/Then
        try {
            snippetService.update(1L, "content", "token")
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("Viewer") == true)
        }
    }

    @Test
    fun `delete should delete snippet`() {
        // Given
        whenever(snippetRepository.existsById(any())).thenReturn(true)

        // When
        snippetService.delete("snippets", 1L)

        // Then
        verify(snippetRepository).deleteById(1L)
        verify(assetService).delete("snippets", 1L)
    }

    @Test
    fun `delete should throw when snippet not found`() {
        // Given
        whenever(snippetRepository.existsById(any())).thenReturn(false)

        // When/Then
        try {
            snippetService.delete("snippets", 1L)
            assert(false) { "Should have thrown SnippetNotFound" }
        } catch (e: SnippetNotFound) {
            // Expected
        }
    }

    @Test
    fun `checkIfExists should throw when snippet not found`() {
        // Given
        whenever(snippetRepository.existsById(any())).thenReturn(false)

        // When/Then
        try {
            snippetService.checkIfExists(1L, "test")
            assert(false) { "Should have thrown SnippetNotFound" }
        } catch (e: SnippetNotFound) {
            assert(e.message?.contains("test") == true)
        }
    }

    @Test
    fun `checkIfExists should not throw when snippet exists`() {
        // Given
        whenever(snippetRepository.existsById(any())).thenReturn(true)

        // When/Then - should not throw
        snippetService.checkIfExists(1L, "test")
    }

    @Test
    fun `countSnippets should return count when name provided`() {
        // Given
        whenever(snippetRepository.countByNameContainingIgnoreCase(any())).thenReturn(5L)

        // When
        val count = snippetService.countSnippets("test")

        // Then
        count shouldBeEqualTo 5L
        verify(snippetRepository).countByNameContainingIgnoreCase("test")
    }

    @Test
    fun `countSnippets should return total count when name is null`() {
        // Given
        whenever(snippetRepository.count()).thenReturn(10L)

        // When
        val count = snippetService.countSnippets(null)

        // Then
        count shouldBeEqualTo 10L
        verify(snippetRepository).count()
    }

    @Test
    fun `updateStatus should update snippet status`() {
        // Given
        val updatedSnippet = snippet.copy(status = Compliance.SUCCESS)
        whenever(snippetRepository.findById(any())).thenReturn(Optional.of(snippet))
        whenever(snippetRepository.save(any<Snippet>())).thenReturn(updatedSnippet)
        whenever(assetService.get(org.mockito.kotlin.eq("snippets"), any())).thenReturn("content")

        // When
        val result = snippetService.updateStatus(1L, Compliance.SUCCESS)

        // Then
        verify(snippetRepository).save(any<Snippet>())
        assert(result.status == Compliance.SUCCESS)
    }

    @Test
    fun `runSnippet should call runner service and return output`() {
        // Given
        val output = listOf("output1", "output2")
        whenever(snippetRepository.findById(any())).thenReturn(Optional.of(snippet))
        whenever(assetService.get(org.mockito.kotlin.eq("snippets"), any())).thenReturn("print('hello')")
        whenever(assetService.exists(org.mockito.kotlin.eq("lint-warnings"), any())).thenReturn(false)
        whenever(
            restTemplate.exchange(
                org.mockito.kotlin.any<String>(),
                org.mockito.kotlin.any<HttpMethod>(),
                org.mockito.kotlin.any<HttpEntity<*>>(),
                org.mockito.kotlin.any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(ResponseEntity.ok(output))

        // When
        val result = snippetService.runSnippet(1L, emptyList(), "token")

        // Then
        assert(result == output)
    }

    @Test
    fun `format should publish format event`() {
        // Given
        whenever(authorizationServiceClient.validate(any())).thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetRepository.findById(any())).thenReturn(Optional.of(snippet))
        whenever(assetService.get(org.mockito.kotlin.eq("snippets"), any())).thenReturn("content")
        whenever(assetService.exists(org.mockito.kotlin.eq("lint-warnings"), any())).thenReturn(false)

        // When
        snippetService.format(1L, "content", "token")

        // Then
        verify(authorizationServiceClient).validate("token")
        verify(runnerServiceProducer).publishSnippetEvent(any())
    }

    @Test
    fun `format should not publish when user not authenticated`() {
        // Given
        whenever(authorizationServiceClient.validate(any())).thenReturn(ResponseEntity.ok(null))

        // When
        snippetService.format(1L, "content", "token")

        // Then
        verify(authorizationServiceClient).validate("token")
        verify(runnerServiceProducer, org.mockito.kotlin.never()).publishSnippetEvent(any())
    }
}
