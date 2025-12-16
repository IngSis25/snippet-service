package snippets.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import snippets.dto.request.Rule
import snippets.dto.request.SaveRulesReq
import snippets.service.RulesService
import snippets.service.SnippetService

@WebMvcTest(
    controllers = [RulesController::class, SnippetRulesController::class, InternalSnippetRulesController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.security.oauth2.resourceserver.jwt.issuer-uri=", "auth0.audience="])
class RulesControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var rulesService: RulesService

    @MockitoBean
    private lateinit var snippetService: SnippetService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val testRules =
        listOf(
            Rule(id = "rule1", name = "rule1", isActive = true, value = null),
            Rule(id = "rule2", name = "rule2", isActive = false, value = "test"),
        )

    @BeforeEach
    fun setUp() {
        whenever(rulesService.getFormatRules(any(), any())).thenReturn(testRules)
        whenever(rulesService.getLintRules(any(), any())).thenReturn(testRules)
        whenever(rulesService.saveFormatRules(any(), any())).thenReturn(testRules)
        whenever(rulesService.saveLintRules(any(), any())).thenReturn(testRules)
    }

    @Test
    fun `getFormatRules should return format rules`() {
        mockMvc.perform(
            get("/api/rules/format")
                .header("Authorization", "Bearer token")
                .param("version", "1.1"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value("rule1"))
            .andExpect(jsonPath("$[0].isActive").value(true))
    }

    @Test
    fun `getLintRules should return lint rules`() {
        mockMvc.perform(
            get("/api/rules/lint")
                .header("Authorization", "Bearer token")
                .param("version", "1.1"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value("rule1"))
    }

    @Test
    fun `saveFormatRules should save and return format rules`() {
        val request = SaveRulesReq(rules = testRules, configText = null, configFormat = null)

        mockMvc.perform(
            post("/api/rules/format")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value("rule1"))
    }

    @Test
    fun `saveLintRules should save and return lint rules`() {
        val request = SaveRulesReq(rules = testRules, configText = null, configFormat = null)

        mockMvc.perform(
            post("/api/rules/lint")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value("rule1"))
    }

    @Test
    fun `formatSnippet should format snippet and return content`() {
        whenever(rulesService.formatSnippet(any(), any())).thenReturn("formatted content")

        mockMvc.perform(
            post("/api/snippets/run/1/format")
                .header("Authorization", "Bearer token"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").value("formatted content"))
    }

    @Test
    fun `formatSnippet should handle errors`() {
        whenever(rulesService.formatSnippet(any(), any())).thenThrow(RuntimeException("Error formatting"))

        mockMvc.perform(
            post("/api/snippets/run/1/format")
                .header("Authorization", "Bearer token"),
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `saveFormatResult should save format result`() {
        val body = mapOf("content" to "formatted content")

        mockMvc.perform(
            post("/api/internal/snippets/1/format")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Format result saved"))
    }

    @Test
    fun `saveFormatResult should return error when content is missing`() {
        val body = mapOf<String, String>()

        mockMvc.perform(
            post("/api/internal/snippets/1/format")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Content field is required"))
    }

    @Test
    fun `saveLintResult should save lint result with list`() {
        val body = listOf(mapOf("message" to "warning1"))

        mockMvc.perform(
            post("/api/internal/snippets/1/lint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Lint result saved"))
    }

    @Test
    fun `saveLintResult should save lint result with map containing warnings`() {
        val body = mapOf("warnings" to "warning json")

        mockMvc.perform(
            post("/api/internal/snippets/1/lint")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Lint result saved"))
    }
}
