package service

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
import snippets.dto.request.Rule
import snippets.dto.response.FullSnippet
import snippets.enums.RulesType
import snippets.factories.FormatterRulesFactory
import snippets.factories.LinterRulesFactory
import snippets.model.Compliance
import snippets.model.FormatterRulesState
import snippets.model.Language
import snippets.model.Snippet
import snippets.repositories.FormatterRulesStateRepository
import snippets.service.AssetService
import snippets.service.AuthorizationServiceClient
import snippets.service.RulesService
import snippets.service.RunnerServiceProducer
import snippets.service.SnippetService

@ExtendWith(MockitoExtension::class)
class RulesServiceAdditionalTest {
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
    }

    @Test
    fun `getFormatRules should return rules from global state when user state not found`() {
        // Given
        val globalState =
            FormatterRulesState(
                id = null,
                type = RulesType.FORMATTER,
                ownerId = null,
                enabledJson = listOf("rule1"),
                optionsJson = emptyMap(),
                configText = null,
                configFormat = null,
            )
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(globalState)
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(testRules)

        // When
        val rules = rulesService.getFormatRules("token", "1.1")

        // Then
        assert(rules.isNotEmpty())
        verify(rulesStateRepository).findByTypeAndOwnerIdIsNull(RulesType.FORMATTER)
    }

    @Test
    fun `getLintRules should return rules from global state when user state not found`() {
        // Given
        val globalState =
            FormatterRulesState(
                id = null,
                type = RulesType.LINTER,
                ownerId = null,
                enabledJson = listOf("rule1"),
                optionsJson = emptyMap(),
                configText = null,
                configFormat = null,
            )
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(globalState)
        whenever(linterRulesFactory.getAvailableRules(any())).thenReturn(testRules)
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")

        // When
        val rules = rulesService.getLintRules("token", "1.1")

        // Then
        assert(rules.isNotEmpty())
        verify(rulesStateRepository).findByTypeAndOwnerIdIsNull(RulesType.LINTER)
    }

    @Test
    fun `lintSnippetSync should return snippet detail with warnings`() {
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
        val warnings = listOf("warning1", "warning2")
        val warningsJson = """["warning1", "warning2"]"""

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(linterRulesFactory.getAvailableRules(any())).thenReturn(testRules)
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(ResponseEntity.ok(warnings))
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")
        val updatedSnippet = FullSnippet(snippetModel, "print('hello')", warnings)
        whenever(snippetService.updateStatus(any(), any())).thenReturn(updatedSnippet)
        whenever(snippetService.get(any())).thenReturn(snippet, updatedSnippet)

        // When
        val result = rulesService.lintSnippetSync("token", 1L)

        // Then
        assert(result.lintCount == 2)
        assert(result.warnings.size == 2)
        assert(result.isValid == false)
    }

    @Test
    fun `lintSnippetSync should update status to success when no warnings`() {
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
        val emptyWarnings = emptyList<String>()

        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)
        whenever(linterRulesFactory.getAvailableRules(any())).thenReturn(testRules)
        whenever(
            restTemplate.exchange(
                any<String>(),
                any<HttpMethod>(),
                any<HttpEntity<*>>(),
                any<ParameterizedTypeReference<*>>(),
            ),
        ).thenReturn(ResponseEntity.ok(emptyWarnings))
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")
        val updatedSnippet = FullSnippet(snippetModel, "print('hello')", emptyWarnings)
        whenever(snippetService.updateStatus(any(), any())).thenReturn(updatedSnippet)
        whenever(snippetService.get(any())).thenReturn(snippet, updatedSnippet)

        // When
        val result = rulesService.lintSnippetSync("token", 1L)

        // Then
        assert(result.lintCount == 0)
        assert(result.isValid == true)
        verify(snippetService).updateStatus(1L, Compliance.SUCCESS)
    }

    @Test
    fun `lintSnippetSync should throw when content is blank`() {
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
            rulesService.lintSnippetSync("token", 1L)
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("empty") == true)
        }
    }
}
