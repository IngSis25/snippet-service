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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import snippets.dto.request.SnippetRequest
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
class SnippetControllerCreateTest {
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
    private lateinit var fullSnippet: FullSnippet

    @BeforeEach
    fun setUp() {
        language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        val snippet =
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
    fun `create should create snippet with language name and version`() {
        // Given
        val snippetRequest =
            SnippetRequest(
                name = "New Snippet",
                content = "print('Hello')",
                languageId = null,
                language = "PrintScript",
                version = "1.0",
                extension = null,
                owner = "test@example.com",
            )
        whenever(languageService.getLanguageByNameAndVersion(any(), any())).thenReturn(language)
        whenever(snippetService.create(any(), any(), any(), any(), any())).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(
            post("/api/snippets")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(snippetRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Test Snippet"))
    }

    @Test
    fun `create should create snippet with language name only`() {
        // Given
        val snippetRequest =
            SnippetRequest(
                name = "New Snippet",
                content = "print('Hello')",
                languageId = null,
                language = "PrintScript",
                version = null,
                extension = null,
                owner = "test@example.com",
            )
        whenever(languageService.getLanguageByName(any())).thenReturn(language)
        whenever(snippetService.create(any(), any(), any(), any(), any())).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(
            post("/api/snippets")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(snippetRequest)),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `create should create snippet with extension`() {
        // Given
        val snippetRequest =
            SnippetRequest(
                name = "New Snippet",
                content = "print('Hello')",
                languageId = null,
                language = null,
                version = null,
                extension = "ps",
                owner = "test@example.com",
            )
        whenever(languageService.getLanguageByExtension(any())).thenReturn(language)
        whenever(snippetService.create(any(), any(), any(), any(), any())).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(
            post("/api/snippets")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(snippetRequest)),
        )
            .andExpect(status().isOk)
    }
}
