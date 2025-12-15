package service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.web.client.RestTemplate
import snippets.dto.response.SnippetUserDto
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

@ExtendWith(MockitoExtension::class)
class SnippetServiceGetFilteredTest {
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
    fun `getFilteredSnippets should filter snippets that don't match name`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        whenever(snippetRepository.findAllById(any<Collection<Long>>())).thenReturn(listOf(snippet))
        whenever(assetService.exists(any(), any())).thenReturn(false)
        // assetService.get is not called when exists returns false

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 10, snippetsIds, "NonExistent", null, null, null)

        // Then
        assert(result.isEmpty())
        assert(count == 0L)
    }

    @Test
    fun `getFilteredSnippets should filter snippets that don't match language`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        whenever(snippetRepository.findAllById(any<Collection<Long>>())).thenReturn(listOf(snippet))
        whenever(assetService.exists(any(), any())).thenReturn(false)
        // assetService.get is not called when exists returns false

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, listOf(999L), null)

        // Then
        assert(result.isEmpty())
        assert(count == 0L)
    }

    @Test
    fun `getFilteredSnippets should filter snippets that don't match compliance`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        whenever(snippetRepository.findAllById(any<Collection<Long>>())).thenReturn(listOf(snippet))
        whenever(assetService.exists(any(), any())).thenReturn(false)
        // assetService.get is not called when exists returns false

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, listOf(Compliance.SUCCESS))

        // Then
        assert(result.isEmpty())
        assert(count == 0L)
    }

    @Test
    fun `getFilteredSnippets should handle multiple filters`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        whenever(snippetRepository.findAllById(any<Collection<Long>>())).thenReturn(listOf(snippet))
        whenever(assetService.exists(any(), any())).thenReturn(false)
        // assetService.get is not called when exists returns false

        // When
        val (result, count) =
            snippetService.getFilteredSnippets(
                0,
                10,
                snippetsIds,
                "Test",
                listOf("Owner"),
                listOf(1L),
                listOf(Compliance.PENDING),
            )

        // Then
        assert(result.size == 1)
        assert(count == 1L)
    }

    @Test
    fun `getFilteredSnippets should handle warnings when Search in not found message`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        whenever(snippetRepository.findAllById(any())).thenReturn(listOf(snippet))
        whenever(assetService.exists(any(), any())).thenReturn(true)
        whenever(assetService.get(any(), any())).thenReturn("Search in lint-warnings not found", "content")

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, null)

        // Then
        assert(result.size == 1)
        assert(result[0].lintWarnings.isEmpty())
    }
}
