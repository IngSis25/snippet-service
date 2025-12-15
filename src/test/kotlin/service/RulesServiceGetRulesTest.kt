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
class RulesServiceGetRulesTest {
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
    fun `getFormatRules should return rules with options from state`() {
        // Given
        val rulesState =
            FormatterRulesState(
                id = null,
                type = RulesType.FORMATTER,
                ownerId = "user123",
                enabledJson = listOf("rule1"),
                optionsJson = mapOf("rule1" to "customValue"),
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
        val activeRule = rules.find { it.id == "rule1" }
        assert(activeRule != null)
        assert(activeRule!!.isActive == true)
    }

    @Test
    fun `getLintRules should return rules with options from state`() {
        // Given
        val rulesState =
            FormatterRulesState(
                id = null,
                type = RulesType.LINTER,
                ownerId = "user123",
                enabledJson = listOf("rule1"),
                optionsJson = mapOf("rule1" to "customValue"),
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
        val activeRule = rules.find { it.id == "rule1" }
        assert(activeRule != null)
        assert(activeRule!!.isActive == true)
    }

    @Test
    fun `getFormatRules should return default rules when state has no enabled rules`() {
        // Given
        val rulesState =
            FormatterRulesState(
                id = null,
                type = RulesType.FORMATTER,
                ownerId = "user123",
                enabledJson = emptyList(),
                optionsJson = null,
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
        assert(rules.all { !it.isActive })
    }

    @Test
    fun `getLintRules should persist rules to asset when state found`() {
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
        rulesService.getLintRules("token", "1.1")

        // Then
        verify(assetService).put(any(), any(), any())
    }

    @Test
    fun `persistRulesToAsset should handle errors gracefully`() {
        // Given
        val rules = listOf(Rule(id = "rule1", name = "rule1", isActive = true, value = null))
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.findByTypeAndOwnerIdIsNull(any())).thenReturn(null)
        whenever(linterRulesFactory.getAvailableRules(any())).thenReturn(rules)
        whenever(assetService.put(any(), any(), any())).thenThrow(RuntimeException("Error"))

        // When - should not throw
        val result = rulesService.getLintRules("token", "1.1")

        // Then
        assert(result.isNotEmpty())
    }
}
