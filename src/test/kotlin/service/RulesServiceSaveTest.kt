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
import snippets.dto.request.SaveRulesReq
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
class RulesServiceSaveTest {
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
            Rule(id = "rule2", name = "rule2", isActive = true, value = "test"),
            Rule(id = "rule3", name = "rule3", isActive = false, value = null),
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
    fun `saveFormatRules should create new state when not exists`() {
        // Given
        val request = SaveRulesReq(rules = testRules, configText = "config", configFormat = "json")
        val savedState =
            FormatterRulesState(
                id = null,
                type = RulesType.FORMATTER,
                ownerId = "user123",
                enabledJson = listOf("rule1", "rule2"),
                optionsJson = mapOf("rule2" to "test"),
                configText = "config",
                configFormat = "json",
            )
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.save(any<FormatterRulesState>())).thenReturn(savedState)
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(testRules)
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")

        // When
        val rules = rulesService.saveFormatRules("token", request)

        // Then
        assert(rules.isNotEmpty())
        verify(rulesStateRepository).save(any<FormatterRulesState>())
        verify(assetService).put(any(), any(), any())
    }

    @Test
    fun `saveFormatRules should update existing state`() {
        // Given
        val request = SaveRulesReq(rules = testRules, configText = "new config", configFormat = "yaml")
        val existingState =
            FormatterRulesState(
                id = null,
                type = RulesType.FORMATTER,
                ownerId = "user123",
                enabledJson = emptyList(),
                optionsJson = null,
                configText = null,
                configFormat = null,
            )
        existingState.enabledJson = listOf("rule1", "rule2")
        existingState.optionsJson = mapOf("rule2" to "test")
        existingState.configText = "new config"
        existingState.configFormat = "yaml"
        val savedState = existingState
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(existingState)
        whenever(rulesStateRepository.save(any<FormatterRulesState>())).thenReturn(savedState)
        whenever(formatterRulesFactory.getAvailableRules(any())).thenReturn(testRules)
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")

        // When
        val rules = rulesService.saveFormatRules("token", request)

        // Then
        assert(rules.isNotEmpty())
        verify(rulesStateRepository).save(any<FormatterRulesState>())
    }

    @Test
    fun `saveFormatRules should throw when user not authenticated`() {
        // Given
        val request = SaveRulesReq(rules = testRules, configText = null, configFormat = null)
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok(null))

        // When/Then
        try {
            rulesService.saveFormatRules("token", request)
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("autenticado") == true)
        }
    }

    @Test
    fun `saveLintRules should create new state when not exists`() {
        // Given
        val request = SaveRulesReq(rules = testRules, configText = null, configFormat = null)
        val savedState =
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
        whenever(rulesStateRepository.findByTypeAndOwnerId(any(), any())).thenReturn(null)
        whenever(rulesStateRepository.save(any<FormatterRulesState>())).thenReturn(savedState)
        whenever(linterRulesFactory.getAvailableRules(any())).thenReturn(testRules)
        whenever(assetService.put(any(), any(), any())).thenReturn("Asset updated")

        // When
        val rules = rulesService.saveLintRules("token", request)

        // Then
        assert(rules.isNotEmpty())
        verify(rulesStateRepository).save(any<FormatterRulesState>())
    }

    @Test
    fun `saveLintRules should throw when user not authenticated`() {
        // Given
        val request = SaveRulesReq(rules = testRules, configText = null, configFormat = null)
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok(null))

        // When/Then
        try {
            rulesService.saveLintRules("token", request)
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("autenticado") == true)
        }
    }
}
