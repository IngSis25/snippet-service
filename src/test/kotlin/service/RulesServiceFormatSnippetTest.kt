package service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import snippets.dto.request.Rule
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
class RulesServiceFormatSnippetTest {
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
    }

    @Test
    fun `formatSnippet should format with active rules`() {
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
        val formattedContent = "print( 'hello' )"
        val responseBody = mapOf("formatted" to formattedContent)
        val rules =
            listOf(
                Rule(id = "rule1", name = "space_around_equals", isActive = true, value = null),
            )

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(null)
        whenever(formatterRulesFactory.getAvailableRules(eq("1.0"))).thenReturn(rules)
        whenever(formatterRulesFactory.getAvailableRules(eq("1.1"))).thenReturn(rules)
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(ResponseEntity.ok(responseBody))
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")

        // When
        val result = rulesService.formatSnippet("token", 1L)

        // Then
        assert(result == formattedContent)
        verify(assetService).put("snippets", 1L, formattedContent)
    }

    @Test
    fun `formatSnippet should handle rules with numeric values`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.1", extension = "ps")
        val snippetModel =
            Snippet(
                id = 1L,
                name = "Test",
                owner = "user",
                status = Compliance.PENDING,
                language = language,
            )
        val snippet = FullSnippet(snippetModel, "print('hello')")
        val formattedContent = "formatted"
        val responseBody = mapOf("formatted" to formattedContent)
        val rules =
            listOf(
                Rule(id = "rule1", name = "number_of_spaces_indentation", isActive = true, value = 4),
            )

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(null)
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(rules)
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(ResponseEntity.ok(responseBody))
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")

        // When
        val result = rulesService.formatSnippet("token", 1L)

        // Then
        assert(result == formattedContent)
    }

    @Test
    fun `formatSnippet should handle rules with boolean values`() {
        // Given
        val language = Language(id = 1L, name = "PrintScript", version = "1.1", extension = "ps")
        val snippetModel =
            Snippet(
                id = 1L,
                name = "Test",
                owner = "user",
                status = Compliance.PENDING,
                language = language,
            )
        val snippet = FullSnippet(snippetModel, "print('hello')")
        val formattedContent = "formatted"
        val responseBody = mapOf("formatted" to formattedContent)
        val rules =
            listOf(
                Rule(id = "rule1", name = "same_line_for_if_brace", isActive = true, value = true),
            )

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(null)
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(rules)
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(ResponseEntity.ok(responseBody))
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")

        // When
        val result = rulesService.formatSnippet("token", 1L)

        // Then
        assert(result == formattedContent)
    }
}
