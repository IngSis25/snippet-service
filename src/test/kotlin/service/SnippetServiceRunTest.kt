package service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
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
class SnippetServiceRunTest {
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
    fun `runSnippet should return empty list when response body is null`() {
        // Given
        val outputs = emptyList<String>()
        whenever(snippetRepository.findById(any())).thenReturn(Optional.of(snippet))
        whenever(assetService.get(any(), any())).thenReturn("print('hello')")
        whenever(assetService.exists(any(), any())).thenReturn(false)
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(ResponseEntity.ok(null))

        // When
        val result = snippetService.runSnippet(1L, emptyList(), "token")

        // Then
        assert(result.isEmpty())
    }

    @Test
    fun `runSnippet should return outputs from response`() {
        // Given
        val outputs = listOf("output1", "output2")
        whenever(snippetRepository.findById(any())).thenReturn(Optional.of(snippet))
        whenever(assetService.get(any(), any())).thenReturn("print('hello')")
        whenever(assetService.exists(any(), any())).thenReturn(false)
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(ResponseEntity.ok(outputs))

        // When
        val result = snippetService.runSnippet(1L, listOf("input1"), "token")

        // Then
        assert(result == outputs)
    }
}
