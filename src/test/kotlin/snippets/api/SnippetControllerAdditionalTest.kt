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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import snippets.dto.request.ContentRequest
import snippets.dto.response.FullSnippet
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet
import snippets.service.AuthorizationServiceClient
import snippets.service.LanguageService
import snippets.service.SnippetService

@WebMvcTest(
    controllers = [SnippetController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.security.oauth2.resourceserver.jwt.issuer-uri=", "auth0.audience="])
class SnippetControllerAdditionalTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var snippetService: SnippetService

    @MockitoBean
    private lateinit var authorizationServiceClient: AuthorizationServiceClient

    @MockitoBean
    private lateinit var languageService: LanguageService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var language: Language
    private lateinit var snippet: Snippet
    private lateinit var fullSnippet: FullSnippet

    @BeforeEach
    fun setUp() {
        language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        snippet =
            Snippet(
                id = 1L,
                name = "Test Snippet",
                owner = "test@example.com",
                status = Compliance.PENDING,
                language = language,
            )
        fullSnippet = FullSnippet(snippet, "print('Hello')", emptyList())
    }

    @Test
    fun `format should publish format request`() {
        // Given
        val body = mapOf("content" to "print('Hello')")

        // When/Then
        mockMvc.perform(
            post("/api/snippets/format/1")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(content().string("Format request published to runner-service"))
    }

    @Test
    fun `format should return bad request when content missing`() {
        // Given
        val body = mapOf<String, String>()

        // When/Then
        mockMvc.perform(
            post("/api/snippets/format/1")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Content field is required."))
    }

    @Test
    fun `runSnippet should return outputs`() {
        // Given
        val outputs = listOf("output1", "output2")
        whenever(snippetService.runSnippet(any(), any(), any())).thenReturn(outputs)
        val body = mapOf("inputs" to listOf("input1"))

        // When/Then
        mockMvc.perform(
            post("/api/snippets/1/run")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0]").value("output1"))
            .andExpect(jsonPath("$[1]").value("output2"))
    }

    @Test
    fun `runSnippet should work without body`() {
        // Given
        val outputs = listOf("output1")
        whenever(snippetService.runSnippet(any(), any(), any())).thenReturn(outputs)

        // When/Then
        mockMvc.perform(
            post("/api/snippets/1/run")
                .header("Authorization", "Bearer token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0]").value("output1"))
    }

    @Test
    fun `updateStatus should update snippet status`() {
        // Given
        whenever(snippetService.updateStatus(any(), any())).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(
            put("/api/snippets/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"SUCCESS\""),
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `updateStatus should handle errors`() {
        // Given
        whenever(snippetService.updateStatus(any(), any())).thenThrow(RuntimeException("Error"))

        // When/Then
        mockMvc.perform(
            put("/api/snippets/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"SUCCESS\""),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `checkOwner should return ok when user is owner`() {
        // Given
        whenever(authorizationServiceClient.checkIfOwner(any(), any(), any())).thenReturn(true)

        // When/Then
        mockMvc.perform(
            post("/api/snippets/1/check-owner")
                .header("Authorization", "Bearer token"),
        )
            .andExpect(status().isOk)
            .andExpect(content().string("User is the owner of the snippet"))
    }

    @Test
    fun `checkOwner should return bad request when user is not owner`() {
        // Given
        whenever(authorizationServiceClient.checkIfOwner(any(), any(), any())).thenReturn(false)

        // When/Then
        mockMvc.perform(
            post("/api/snippets/1/check-owner")
                .header("Authorization", "Bearer token"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("User is not the owner of the snippet"))
    }

    @Test
    fun `downloadSnippet should return snippet data`() {
        // Given
        whenever(snippetService.get(any())).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(get("/api/snippets/1/download"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Test Snippet"))
            .andExpect(jsonPath("$.content").value("print('Hello')"))
            .andExpect(jsonPath("$.language").value("PrintScript"))
            .andExpect(jsonPath("$.version").value("1.0"))
    }

    @Test
    fun `downloadFormattedSnippet should return snippet data`() {
        // Given
        whenever(snippetService.get(any())).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(get("/api/snippets/1/download/formatted"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Test Snippet"))
            .andExpect(jsonPath("$.content").value("print('Hello')"))
    }

    @Test
    fun `testError should throw runtime exception`() {
        // When/Then
        mockMvc.perform(get("/api/snippets/test-error"))
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `update should return forbidden when user is Viewer`() {
        // Given
        whenever(snippetService.update(any(), any(), any()))
            .thenThrow(RuntimeException("No tenés permisos para editar este snippet. Solo tenés permisos de lectura (Viewer)."))
        val request = ContentRequest(content = "updated content")

        // When/Then
        mockMvc.perform(
            put("/api/snippets/1")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `share should return not found when snippet not found`() {
        // Given
        whenever(snippetService.get(any())).thenThrow(RuntimeException("Snippet not found"))
        val request =
            mapOf(
                "fromEmail" to "from@example.com",
                "toEmail" to "to@example.com",
                "role" to "Guest",
            )

        // When/Then
        mockMvc.perform(
            post("/api/snippets/share/1")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isNotFound)
    }
}
