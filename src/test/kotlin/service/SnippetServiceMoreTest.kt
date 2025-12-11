package service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.ResponseEntity
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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class SnippetServiceMoreTest {
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
    fun `getFilteredSnippets should filter by languages`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        whenever(snippetRepository.findAllById(any<Collection<Long>>())).thenReturn(listOf(snippet))
        whenever(assetService.exists(any(), any())).thenReturn(false)
        // assetService.get is not called when exists returns false

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, listOf(1L), null)

        // Then
        assert(result.size == 1)
        assert(count == 1L)
    }

    @Test
    fun `getFilteredSnippets should filter by compliance`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        whenever(snippetRepository.findAllById(any<Collection<Long>>())).thenReturn(listOf(snippet))
        whenever(assetService.exists(any(), any())).thenReturn(false)
        // assetService.get is not called when exists returns false

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, listOf(Compliance.PENDING))

        // Then
        assert(result.size == 1)
        assert(count == 1L)
    }

    @Test
    fun `getFilteredSnippets should paginate results`() {
        // Given
        val snippetsIds =
            listOf(
                SnippetUserDto(snippetId = 1L, role = "Owner"),
                SnippetUserDto(snippetId = 2L, role = "Guest"),
                SnippetUserDto(snippetId = 3L, role = "Owner"),
            )
        val snippet2 = snippet.copy(id = 2L)
        val snippet3 = snippet.copy(id = 3L)
        whenever(snippetRepository.findAllById(any<Collection<Long>>())).thenReturn(listOf(snippet, snippet2, snippet3))
        whenever(assetService.exists(any(), any())).thenReturn(false)
        // assetService.get is not called when exists returns false

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 2, snippetsIds, null, null, null, null)

        // Then
        assert(result.size == 2)
        assert(count == 3L)
    }

    @Test
    fun `getFilteredSnippets should handle warnings json parsing errors`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        whenever(snippetRepository.findAllById(any())).thenReturn(listOf(snippet))
        whenever(assetService.exists(any(), any())).thenReturn(true)
        whenever(assetService.get(any(), any())).thenReturn("invalid json", "content")

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, null)

        // Then
        assert(result.size == 1)
        assert(result[0].lintWarnings.isEmpty())
    }

    @Test
    fun `getFilteredSnippets should handle empty warnings json`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        whenever(snippetRepository.findAllById(any())).thenReturn(listOf(snippet))
        whenever(assetService.exists(any(), any())).thenReturn(true)
        whenever(assetService.get(any(), any())).thenReturn("[]", "content")

        // When
        val (result, count) = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, null)

        // Then
        assert(result.size == 1)
        assert(result[0].lintWarnings.isEmpty())
    }

    @Test
    fun `get should handle warnings when asset exists`() {
        // Given
        val warningsJson = """["warning1", "warning2"]"""
        whenever(snippetRepository.findById(any())).thenReturn(Optional.of(snippet))
        whenever(assetService.get(any(), any())).thenReturn("content", warningsJson)
        whenever(assetService.exists(any(), any())).thenReturn(true)

        // When
        val result = snippetService.get(1L)

        // Then
        assert(result.errors.size == 2)
        assert(result.errors[0] == "warning1")
    }

    @Test
    fun `get should handle warnings json parsing error gracefully`() {
        // Given
        whenever(snippetRepository.findById(any())).thenReturn(Optional.of(snippet))
        whenever(assetService.get(any(), any())).thenReturn("content", "invalid json")
        whenever(assetService.exists(any(), any())).thenReturn(true)

        // When
        val result = snippetService.get(1L)

        // Then
        assert(result.errors.isEmpty())
    }

    @Test
    fun `get should handle exception when getting warnings`() {
        // Given
        whenever(snippetRepository.findById(any())).thenReturn(Optional.of(snippet))
        whenever(assetService.get(any(), any())).thenReturn("content")
        whenever(assetService.exists(any(), any())).thenThrow(RuntimeException("Error"))

        // When
        val result = snippetService.get(1L)

        // Then
        assert(result.errors.isEmpty())
    }

    @Test
    fun `update should publish test events when tests exist`() {
        // Given
        val test = snippets.model.Test(id = 1L, name = "Test", input = listOf("input"), output = listOf("output"), snippet = snippet)
        whenever(snippetRepository.existsById(any())).thenReturn(true)
        whenever(snippetRepository.findById(any())).thenReturn(Optional.of(snippet))
        whenever(authorizationServiceClient.getUserRoleForSnippet(any(), any())).thenReturn("Owner")
        whenever(authorizationServiceClient.validate(any())).thenReturn(ResponseEntity.ok("user123"))
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")
        whenever(testService.getTestsBySnippetId(any())).thenReturn(listOf(test))

        // When
        snippetService.update(1L, "content", "token")

        // Then
        verify(runnerServiceProducer).publishSnippetEvent(any())
        verify(runnerServiceProducer).publishTestEvent(any())
    }

    @Test
    fun `update should handle test service errors gracefully`() {
        // Given
        whenever(snippetRepository.existsById(any())).thenReturn(true)
        whenever(snippetRepository.findById(any())).thenReturn(Optional.of(snippet))
        whenever(authorizationServiceClient.getUserRoleForSnippet(any(), any())).thenReturn("Owner")
        whenever(authorizationServiceClient.validate(any())).thenReturn(ResponseEntity.ok("user123"))
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")
        whenever(testService.getTestsBySnippetId(any())).thenThrow(RuntimeException("Error"))

        // When
        val result = snippetService.update(1L, "content", "token")

        // Then
        verify(runnerServiceProducer).publishSnippetEvent(any())
        // Should not throw, should continue
    }

    @Test
    fun `create should publish snippet event when user validated`() {
        // Given
        val savedSnippet = snippet.copy(id = 2L)
        whenever(languageService.getLanguageById(any())).thenReturn(language)
        whenever(snippetRepository.save(any<Snippet>())).thenReturn(savedSnippet)
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")
        // addSnippetToUser is void, no need to mock return
        whenever(authorizationServiceClient.validate(any())).thenReturn(ResponseEntity.ok("user123"))

        // When
        val result = snippetService.create("Test", "content", "1", "owner", "token")

        // Then
        verify(runnerServiceProducer).publishSnippetEvent(any())
        // The result uses the original snippet (id=0), not the saved one
        assert(result.id == 0L)
    }

    @Test
    fun `create should not publish snippet event when user not validated`() {
        // Given
        val savedSnippet = snippet.copy(id = 2L)
        whenever(languageService.getLanguageById(any())).thenReturn(language)
        whenever(snippetRepository.save(any<Snippet>())).thenReturn(savedSnippet)
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")
        whenever(authorizationServiceClient.validate(any())).thenReturn(ResponseEntity.ok(null))
        // No need to mock addSnippetToUser or publishSnippetEvent since they won't be called
        // No need to mock addSnippetToUser or publishSnippetEvent since they won't be called

        // When
        val result = snippetService.create("Test", "content", "1", "owner", "token")

        // Then
        verify(runnerServiceProducer, org.mockito.kotlin.never()).publishSnippetEvent(any())
        // The result uses the original snippet (id=0), not the saved one
        assert(result.id == 0L)
    }
}
