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
class RulesServicePersistTest {
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
    fun `saveFormatRules should handle empty options map`() {
        // Given
        val request =
            SaveRulesReq(
                rules = listOf(Rule(id = "rule1", name = "rule1", isActive = true, value = null)),
                configText = null,
                configFormat = null,
            )
        val savedState =
            FormatterRulesState(
                id = null,
                type = RulesType.FORMATTER,
                ownerId = "user123",
                enabledJson = listOf("rule1"),
                optionsJson = null,
                configText = null,
                configFormat = null,
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
    }

    @Test
    fun `saveLintRules should handle empty options map`() {
        // Given
        val request =
            SaveRulesReq(
                rules = listOf(Rule(id = "rule1", name = "rule1", isActive = true, value = null)),
                configText = null,
                configFormat = null,
            )
        val savedState =
            FormatterRulesState(
                id = null,
                type = RulesType.LINTER,
                ownerId = "user123",
                enabledJson = listOf("rule1"),
                optionsJson = null,
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
}
