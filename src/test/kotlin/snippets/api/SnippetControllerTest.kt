package snippets.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import snippets.dto.request.ContentRequest
import snippets.dto.request.ShareRequest
import snippets.dto.request.SnippetRequest
import snippets.dto.response.FullSnippet
import snippets.dto.response.SnippetUserDto
import snippets.dto.response.SnippetWithRoleAndWarnings
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
class SnippetControllerTest {
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
    fun `get should return snippet by id`() {
        // Given
        val snippetId = 1L
        whenever(snippetService.get(snippetId)).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(get("/api/snippets/$snippetId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Test Snippet"))
            .andExpect(jsonPath("$.content").value("print('Hello')"))
    }

    @Test
    fun `create should create snippet with languageId`() {
        // Given
        val snippetRequest =
            SnippetRequest(
                name = "New Snippet",
                content = "print('Hello')",
                languageId = "1",
                owner = "test@example.com",
            )
        val token = "test-token"

        whenever(
            snippetService.create(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(
            post("/api/snippets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .content(objectMapper.writeValueAsString(snippetRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Test Snippet"))
    }

    @Test
    fun `create should create snippet with language name`() {
        // Given
        val snippetRequest =
            SnippetRequest(
                name = "New Snippet",
                content = "print('Hello')",
                language = "PrintScript",
                owner = "test@example.com",
            )
        val token = "test-token"

        whenever(languageService.getLanguageByName("PrintScript")).thenReturn(language)
        whenever(
            snippetService.create(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(
            post("/api/snippets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
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
                extension = "ps",
                owner = "test@example.com",
            )
        val token = "test-token"

        whenever(languageService.getLanguageByExtension("ps")).thenReturn(language)
        whenever(
            snippetService.create(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(
            post("/api/snippets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .content(objectMapper.writeValueAsString(snippetRequest)),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `update should update snippet`() {
        // Given
        val snippetId = 1L
        val contentRequest = ContentRequest(content = "print('Updated')")
        val token = "test-token"

        whenever(snippetService.update(any(), any(), any())).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(
            put("/api/snippets/$snippetId")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .content(objectMapper.writeValueAsString(contentRequest)),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `delete should delete snippet`() {
        // Given
        val snippetId = 1L

        // When/Then
        mockMvc.perform(post("/api/snippets/delete/$snippetId"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `share should share snippet`() {
        // Given
        val snippetId = 1L
        val token = "test-token"
        val shareRequest = ShareRequest(fromEmail = "from@example.com", toEmail = "to@example.com")

        whenever(snippetService.get(snippetId)).thenReturn(fullSnippet)
        whenever(
            authorizationServiceClient.shareSnippet(
                any(),
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(ResponseEntity.ok(fullSnippet))

        // When/Then
        mockMvc.perform(
            post("/api/snippets/share/$snippetId")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .content(objectMapper.writeValueAsString(shareRequest)),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `format should publish format request`() {
        // Given
        val snippetId = 1L
        val token = "test-token"
        val body = mapOf("content" to "print('Hello')")

        // When/Then
        mockMvc.perform(
            post("/api/snippets/format/$snippetId")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(content().string("Format request published to runner-service"))
    }

    @Test
    fun `format should return bad request when content is missing`() {
        // Given
        val snippetId = 1L
        val token = "test-token"
        val body = mapOf<String, String>()

        // When/Then
        mockMvc.perform(
            post("/api/snippets/format/$snippetId")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", token)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `updateStatus should update snippet status`() {
        // Given
        val snippetId = 1L
        val newStatus = Compliance.SUCCESS

        whenever(snippetService.updateStatus(any(), any())).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(
            put("/api/snippets/$snippetId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"$newStatus\""),
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `updateStatus should return internal server error on exception`() {
        // Given
        val snippetId = 1L
        val newStatus = Compliance.SUCCESS

        whenever(snippetService.updateStatus(any(), any())).thenThrow(RuntimeException("Error"))

        // When/Then
        mockMvc.perform(
            put("/api/snippets/$snippetId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("\"$newStatus\""),
        )
            .andExpect(status().isInternalServerError)
    }

    @Test
    fun `checkOwner should return ok when user is owner`() {
        // Given
        val snippetId = 1L
        val token = "test-token"
        whenever(authorizationServiceClient.checkIfOwner(any(), any(), any())).thenReturn(true)

        // When/Then
        mockMvc.perform(
            post("/api/snippets/$snippetId/check-owner")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isOk)
            .andExpect(content().string("User is the owner of the snippet"))
    }

    @Test
    fun `checkOwner should return bad request when user is not owner`() {
        // Given
        val snippetId = 1L
        val token = "test-token"
        whenever(authorizationServiceClient.checkIfOwner(any(), any(), any())).thenReturn(false)

        // When/Then
        mockMvc.perform(
            post("/api/snippets/$snippetId/check-owner")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("User is not the owner of the snippet"))
    }

    @Test
    fun `downloadSnippet should return snippet data`() {
        // Given
        val snippetId = 1L
        whenever(snippetService.get(snippetId)).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(get("/api/snippets/$snippetId/download"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("Test Snippet"))
            .andExpect(jsonPath("$.content").value("print('Hello')"))
    }

    @Test
    fun `downloadFormattedSnippet should return formatted snippet data`() {
        // Given
        val snippetId = 1L
        whenever(snippetService.get(snippetId)).thenReturn(fullSnippet)

        // When/Then
        mockMvc.perform(get("/api/snippets/$snippetId/download/formatted"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.name").value("Test Snippet"))
            .andExpect(jsonPath("$.content").value("print('Hello')"))
    }

    @Test
    fun `getSnippetsOfUser should return filtered snippets`() {
        // Given
        val token = "test-token"
        val userId = "auth0|123"
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        val snippets =
            listOf(
                SnippetWithRoleAndWarnings(
                    snippet,
                    "Owner",
                    emptyList(),
                ),
            )

        whenever(authorizationServiceClient.getSnippetsOfUser(token, userId)).thenReturn(snippetsIds)
        doReturn(Pair(snippets, 1L)).whenever(snippetService).getFilteredSnippets(
            any(),
            any(),
            any(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
        )

        // When/Then
        mockMvc.perform(
            get("/api/snippets/user")
                .param("page", "0")
                .param("pageSize", "10")
                .param("userId", userId)
                .header("Authorization", token),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.count").value(1L))
            .andExpect(jsonPath("$.snippets").isArray)
    }
}
