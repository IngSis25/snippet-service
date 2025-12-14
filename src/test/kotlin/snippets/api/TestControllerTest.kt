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
import snippets.service.TestRunResult
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

    @Autowired
    private lateinit var objectMapper: ObjectMapper

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
        whenever(testService.getTestsBySnippetId(any())).thenReturn(listOf(test))

        // When/Then
        mockMvc.perform(get("/api/tests/snippet/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].name").value("Test 1"))
    }

    @Test
    fun `addTestToSnippet should create and return test`() {
        // Given
        val testDTO = TestDTO(test)
        whenever(testService.addTestToSnippet(any(), any(), any(), any())).thenReturn(testDTO)
        val requestBody =
            mapOf(
                "name" to "Test 1",
                "input" to listOf("input1"),
                "output" to listOf("output1"),
            )

        // When/Then
        mockMvc.perform(
            post("/api/tests/snippet/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Test 1"))
    }

    @Test
    fun `deleteTestById should delete test`() {
        // Given
        // No exception means success

        // When/Then
        mockMvc.perform(delete("/api/tests/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `runTest should return success when test passes`() {
        // Given
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(testService.runTestSync(any(), any())).thenReturn(
            TestRunResult(status = "success", errors = emptyList()),
        )

        // When/Then
        mockMvc.perform(
            post("/api/tests/1/run")
                .header("Authorization", "Bearer token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("success"))
            .andExpect(jsonPath("$.errors").isArray)
    }

    @Test
    fun `runTest should return fail when test fails`() {
        // Given
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(testService.runTestSync(any(), any())).thenReturn(
            TestRunResult(
                status = "fail",
                errors = listOf("At index 0 expected '2' but got '1'"),
            ),
        )

        // When/Then
        mockMvc.perform(
            post("/api/tests/1/run")
                .header("Authorization", "Bearer token"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.errors").isArray)
            .andExpect(jsonPath("$.errors[0]").value("At index 0 expected '2' but got '1'"))
    }

    @Test
    fun `runTest should return unauthorized when token invalid`() {
        // Given
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok(null))

        // When/Then
        mockMvc.perform(
            post("/api/tests/1/run")
                .header("Authorization", "Bearer invalid"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.errors[0]").value("Unauthorized"))
    }

    @Test
    fun `runTest should handle exceptions`() {
        // Given
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))
        whenever(testService.runTestSync(any(), any())).thenThrow(RuntimeException("Error"))

        // When/Then
        mockMvc.perform(
            post("/api/tests/1/run")
                .header("Authorization", "Bearer token"),
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.status").value("fail"))
            .andExpect(jsonPath("$.errors[0]").value("Error"))
    }

    @Test
    fun `runAllTests should publish all tests`() {
        // Given
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok("user123"))

        // When/Then
        mockMvc.perform(
            post("/api/tests/snippet/1/run-all")
                .header("Authorization", "Bearer token"),
        )
            .andExpect(status().isOk)
            .andExpect(content().string("All tests execution requests published to runner-service"))
    }

    @Test
    fun `runAllTests should return unauthorized when token invalid`() {
        // Given
        whenever(authorizationServiceClient.validate(any()))
            .thenReturn(ResponseEntity.ok(null))

        // When/Then
        mockMvc.perform(
            post("/api/tests/snippet/1/run-all")
                .header("Authorization", "Bearer invalid"),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(content().string("Invalid token"))
    }
}
