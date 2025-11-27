package snippets.api

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import snippets.dto.response.TestDTO
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet
import snippets.service.AuthorizationServiceClient
import snippets.service.TestService
import snippets.model.Test as TestModel

@WebMvcTest(
    controllers = [TestController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.security.oauth2.resourceserver.jwt.issuer-uri=", "auth0.audience="])
class TestControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var testService: TestService

    @MockitoBean
    private lateinit var authorizationServiceClient: AuthorizationServiceClient

    private lateinit var language: Language
    private lateinit var snippet: Snippet
    private lateinit var test: TestModel

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
        test =
            TestModel(
                id = 1L,
                name = "Test 1",
                input = listOf("input1"),
                output = listOf("output1"),
                snippet = snippet,
            )
    }

    @Test
    fun `getTestsBySnippetId should return list of tests`() {
        // Given
        val snippetId = 1L
        val tests = listOf(test)
        whenever(testService.getTestsBySnippetId(snippetId)).thenReturn(tests)

        // When/Then
        mockMvc.perform(get("/api/tests/snippet/$snippetId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("Test 1"))
    }

    @Test
    fun `addTestToSnippet should create test`() {
        // Given
        val snippetId = 1L
        val testDTO = TestDTO(test)

        whenever(
            testService.addTestToSnippet(
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(testDTO)

        // When/Then
        mockMvc.perform(
            post("/api/tests/snippet/$snippetId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                        "name": "New Test",
                        "input": ["input1"],
                        "output": ["output1"]
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1L))
    }

    @Test
    fun `deleteTestById should delete test`() {
        // Given
        val testId = 1L

        // When/Then
        mockMvc.perform(delete("/api/tests/$testId"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `runTest should publish test execution request`() {
        // Given
        val testId = 1L
        val token = "test-token"
        val userId = 1L

        whenever(authorizationServiceClient.validate(token)).thenReturn(ResponseEntity.ok(userId))

        // When/Then
        mockMvc.perform(
            post("/api/tests/$testId/run")
                .header("Authorization", token),
        )
            .andExpect(status().isOk)
            .andExpect(content().string("Test execution request published to runner-service"))
    }

    @Test
    fun `runAllTests should publish all tests execution requests`() {
        // Given
        val snippetId = 1L
        val token = "test-token"
        val userId = 1L

        whenever(authorizationServiceClient.validate(token)).thenReturn(ResponseEntity.ok(userId))

        // When/Then
        mockMvc.perform(
            post("/api/tests/snippet/$snippetId/run-all")
                .header("Authorization", token),
        )
            .andExpect(status().isOk)
            .andExpect(content().string("All tests execution requests published to runner-service"))
    }
}
