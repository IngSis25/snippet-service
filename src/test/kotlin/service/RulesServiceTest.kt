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
import snippets.dto.request.Rule
import snippets.dto.response.FullSnippet
import snippets.enums.RulesType
import snippets.factories.FormatterRulesFactory
import snippets.factories.LinterRulesFactory
import snippets.model.FormatterRulesState
import snippets.repositories.FormatterRulesStateRepository
import snippets.service.AssetService
import snippets.service.AuthorizationServiceClient
import snippets.service.RulesService
import snippets.service.RunnerServiceProducer
import snippets.service.SnippetService

@ExtendWith(MockitoExtension::class)
class RulesServiceTest {
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
    fun `getFormatRules should return default rules when user not authenticated`() {
        // Given
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok(null))
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(testRules)

        // When
        val rules = rulesService.getFormatRules("token", "1.1")

        // Then
        assert(rules.isNotEmpty())
        verify(formatterRulesFactory).getAvailableRules("1.1")
    }

    @Test
    fun `getFormatRules should return default rules when no rules state found`() {
        // Given
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(null)
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(testRules)

        // When
        val rules = rulesService.getFormatRules("token", "1.1")

        // Then
        assert(rules.isNotEmpty())
        verify(formatterRulesFactory).getAvailableRules("1.1")
    }

    @Test
    fun `getLintRules should return empty list when user not authenticated`() {
        // Given
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok(null))

        // When
        val rules = rulesService.getLintRules("token", "1.1")

        // Then
        assert(rules.isEmpty())
    }

    @Test
    fun `saveFormatResult should save formatted content`() {
        // When
        rulesService.saveFormatResult(1L, "formatted content")

        // Then
        verify(assetService).put("snippets", 1L, "formatted content")
    }

    @Test
    fun `saveLintResult should save lint warnings`() {
        // When
        rulesService.saveLintResult(1L, "warnings json")

        // Then
        verify(assetService).put("lint-warnings", 1L, "warnings json")
    }

    @Test
    fun `getFormatRules should return rules from state when found`() {
        // Given
        val rulesState =
            FormatterRulesState(
                id = null,
                type = RulesType.FORMATTER,
                ownerId = "user123",
                enabledJson = listOf("rule1"),
                optionsJson = emptyMap(),
                configText = null,
                configFormat = null,
            )
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(rulesState)
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(testRules)

        // When
        val rules = rulesService.getFormatRules("token", "1.1")

        // Then
        assert(rules.isNotEmpty())
        verify(rulesStateRepository).findByTypeAndOwnerId(RulesType.FORMATTER, "user123")
    }

    @Test
    fun `getLintRules should return rules from state when found`() {
        // Given
        val rulesState =
            FormatterRulesState(
                id = null,
                type = RulesType.LINTER,
                ownerId = "user123",
                enabledJson = listOf("rule1"),
                optionsJson = emptyMap(),
                configText = null,
                configFormat = null,
            )
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(rulesState)
        whenever(linterRulesFactory.getAvailableRules(any())).thenReturn(testRules)
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")

        // When
        val rules = rulesService.getLintRules("token", "1.1")

        // Then
        assert(rules.isNotEmpty())
        verify(rulesStateRepository).findByTypeAndOwnerId(RulesType.LINTER, "user123")
    }

    @Test
    fun `getLintRules should return default rules when no state found and persist to asset`() {
        // Given
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(null)
        whenever(linterRulesFactory.getAvailableRules(any())).thenReturn(testRules)
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")

        // When
        val rules = rulesService.getLintRules("token", "1.1")

        // Then
        assert(rules.isNotEmpty())
        verify(linterRulesFactory).getAvailableRules("1.1")
        verify(assetService).put(any(), any(), any())
    }

    @Test
    fun `lintSnippet should publish lint event`() {
        // Given
        val snippetModel =
            snippets.model.Snippet(
                id = 1L,
                name = "Test",
                owner = "user",
                status = snippets.model.Compliance.PENDING,
                language = snippets.model.Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps"),
            )
        val snippet = snippets.dto.response.FullSnippet(snippetModel, "print('hello')")
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(snippetService.get(any())).thenReturn(snippet)

        // When
        rulesService.lintSnippet("token", 1L)

        // Then
        verify(snippetService).get(1L)
        verify(runnerServiceProducer).publishLintEvent(any())
    }
}
