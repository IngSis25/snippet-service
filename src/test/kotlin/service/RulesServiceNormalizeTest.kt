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
import snippets.dto.response.FullSnippet
import snippets.factories.FormatterRulesFactory
import snippets.factories.LinterRulesFactory
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet
import snippets.repositories.FormatterRulesStateRepository
import snippets.service.AssetService
import snippets.service.AuthorizationServiceClient
import snippets.service.RulesService
import snippets.service.RunnerServiceProducer
import snippets.service.SnippetService

@ExtendWith(MockitoExtension::class)
class RulesServiceNormalizeTest {
    @Mock
    private lateinit var rulesStateRepository: FormatterRulesStateRepository

    @Mock
    private lateinit var formatterRulesFactory: FormatterRulesFactory

    @Mock
    private lateinit var linterRulesFactory: LinterRulesFactory

    @Mock
    private lateinit var assetService: AssetService

    @Mock
    private lateinit var authorizationServiceClient: AuthorizationServiceClient

    @Mock
    private lateinit var runnerServiceProducer: RunnerServiceProducer

    @Mock
    private lateinit var snippetService: SnippetService

    @Mock
    private lateinit var restTemplate: RestTemplate

    private lateinit var rulesService: RulesService

    @BeforeEach
    fun setUp() {
        rulesService =
            RulesService(
                rulesStateRepository,
                formatterRulesFactory,
                linterRulesFactory,
                assetService,
                authorizationServiceClient,
                runnerServiceProducer,
                snippetService,
                restTemplate,
                "http://runner-service",
            )

        whenever(authorizationServiceClient.getSnippetsOfUser(any(), any())).thenReturn(emptyList())
    }

    @Test
    fun `formatSnippet should handle formatter service errors`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippetModel =
            Snippet(
                id = 1L,
                name = "Test",
                owner = "user",
                status = Compliance.PENDING,
                language = language,
            )
        val snippet = FullSnippet(snippetModel, "print('hello')")
        val responseBody = mapOf<String, Any>()

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(null)
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(emptyList())
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(ResponseEntity.ok(responseBody))

        // When/Then
        try {
            rulesService.formatSnippet("token", 1L)
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("formatted") == true || e.message?.contains("Empty") == true)
        }
    }

    @Test
    fun `formatSnippet should handle formatter service connection errors`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippetModel =
            Snippet(
                id = 1L,
                name = "Test",
                owner = "user",
                status = Compliance.PENDING,
                language = language,
            )
        val snippet = FullSnippet(snippetModel, "print('hello')")

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(null)
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(emptyList())
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenThrow(org.springframework.web.client.ResourceAccessException("Connection refused"))

        // When/Then
        try {
            rulesService.formatSnippet("token", 1L)
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("connect") == true || e.message?.contains("Connection") == true)
        }
    }

    @Test
    fun `formatSnippet should handle formatter service HTTP errors`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippetModel =
            Snippet(
                id = 1L,
                name = "Test",
                owner = "user",
                status = Compliance.PENDING,
                language = language,
            )
        val snippet = FullSnippet(snippetModel, "print('hello')")

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(null)
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(emptyList())
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenThrow(
            org.springframework.web.client.HttpClientErrorException.create(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Bad Request",
                org.springframework.http.HttpHeaders(),
                "Error".toByteArray(),
                java.nio.charset.Charset.defaultCharset(),
            ),
        )

        // When/Then
        try {
            rulesService.formatSnippet("token", 1L)
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("HTTP error") == true || e.message?.contains("400") == true)
        }
    }

    @Test
    fun `formatSnippet should handle formatter service server errors`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippetModel =
            Snippet(
                id = 1L,
                name = "Test",
                owner = "user",
                status = Compliance.PENDING,
                language = language,
            )
        val snippet = FullSnippet(snippetModel, "print('hello')")

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(null)
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(emptyList())
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenThrow(
            org.springframework.web.client.HttpServerErrorException.create(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                org.springframework.http.HttpHeaders(),
                "Error".toByteArray(),
                java.nio.charset.Charset.defaultCharset(),
            ),
        )

        // When/Then
        try {
            rulesService.formatSnippet("token", 1L)
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("server error") == true || e.message?.contains("500") == true)
        }
    }

    @Test
    fun `lintSnippetSync should handle linter service connection errors`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippetModel =
            Snippet(
                id = 1L,
                name = "Test",
                owner = "user",
                status = Compliance.PENDING,
                language = language,
            )
        val snippet = FullSnippet(snippetModel, "print('hello')")

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(linterRulesFactory.getAvailableRules(any())).thenReturn(emptyList())
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenThrow(org.springframework.web.client.ResourceAccessException("Connection refused"))
        // updateStatus won't be called when exception is thrown

        // When/Then
        try {
            rulesService.lintSnippetSync("token", 1L)
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("connect") == true || e.message?.contains("Connection") == true)
        }
    }

    @Test
    fun `lintSnippetSync should handle linter service HTTP errors`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippetModel =
            Snippet(
                id = 1L,
                name = "Test",
                owner = "user",
                status = Compliance.PENDING,
                language = language,
            )
        val snippet = FullSnippet(snippetModel, "print('hello')")

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(linterRulesFactory.getAvailableRules(any())).thenReturn(emptyList())
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenThrow(
            org.springframework.web.client.HttpClientErrorException.create(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Bad Request",
                org.springframework.http.HttpHeaders(),
                "Error".toByteArray(),
                java.nio.charset.Charset.defaultCharset(),
            ),
        )

        // When/Then
        try {
            rulesService.lintSnippetSync("token", 1L)
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("HTTP error") == true || e.message?.contains("400") == true)
        }
    }

    @Test
    fun `lintSnippetSync should handle linter service server errors`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippetModel =
            Snippet(
                id = 1L,
                name = "Test",
                owner = "user",
                status = Compliance.PENDING,
                language = language,
            )
        val snippet = FullSnippet(snippetModel, "print('hello')")

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(linterRulesFactory.getAvailableRules(any())).thenReturn(emptyList())
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenThrow(
            org.springframework.web.client.HttpServerErrorException.create(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                org.springframework.http.HttpHeaders(),
                "Error".toByteArray(),
                java.nio.charset.Charset.defaultCharset(),
            ),
        )

        // When/Then
        try {
            rulesService.lintSnippetSync("token", 1L)
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("server error") == true || e.message?.contains("500") == true)
        }
    }
}
