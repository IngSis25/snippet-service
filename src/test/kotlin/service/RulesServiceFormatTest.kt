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
class RulesServiceFormatTest {
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

    private val testRules =
        listOf(
            Rule(id = "rule1", name = "rule1", isActive = true, value = null),
            Rule(id = "rule2", name = "rule2", isActive = false, value = "test"),
        )

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
    fun `formatSnippet should throw when user not authenticated`() {
        // Given
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok(null))

        // When/Then
        try {
            rulesService.formatSnippet("token", 1L)
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("autenticado") == true)
        }
    }

    @Test
    fun `formatSnippet should throw when content is blank`() {
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
        val snippet = FullSnippet(snippetModel, "")

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)

        // When/Then
        try {
            rulesService.formatSnippet("token", 1L)
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("empty") == true)
        }
    }

    @Test
    fun `formatSnippet should format snippet with active rules`() {
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

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(null)
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(testRules)
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
    fun `formatSnippet should use version 1_1 when v11 only rules are active`() {
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
        val v11Rule = Rule(id = "same_line_for_if_brace", name = "same_line_for_if_brace", isActive = true, value = null)
        val rulesWithV11 = listOf(v11Rule)
        val formattedContent = "formatted"
        val responseBody = mapOf("formatted" to formattedContent)

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(null)
        whenever(formatterRulesFactory.getAvailableRules(eq("1.0"))).thenReturn(rulesWithV11)
        whenever(formatterRulesFactory.getAvailableRules(eq("1.1"))).thenReturn(rulesWithV11)
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
        rulesService.formatSnippet("token", 1L)

        // Then
        verify(formatterRulesFactory).getAvailableRules("1.1")
    }
}
