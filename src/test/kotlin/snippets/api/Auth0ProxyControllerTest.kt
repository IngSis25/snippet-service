package snippets.api

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import snippets.service.AuthUserDTO
import snippets.service.AuthorizationServiceClient

@WebMvcTest(
    controllers = [Auth0ProxyController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.security.oauth2.resourceserver.jwt.issuer-uri=", "auth0.audience="])
class Auth0ProxyControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authorizationServiceClient: AuthorizationServiceClient

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `searchUsers should return users`() {
        // Given
        val users =
            listOf(
                AuthUserDTO(id = "auth0|1", email = "user1@example.com"),
                AuthUserDTO(id = "auth0|2", email = "user2@example.com"),
            )
        whenever(authorizationServiceClient.searchUsers(any(), any())).thenReturn(users)

        // When/Then
        mockMvc.perform(
            get("/api/auth0/users")
                .param("search", "user")
                .header("Authorization", "Bearer token"),
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `searchUsers should return empty list when search is blank`() {
        // Given
        whenever(authorizationServiceClient.searchUsers(any(), any())).thenReturn(emptyList())

        // When/Then
        mockMvc.perform(
            get("/api/auth0/users")
                .param("search", "")
                .header("Authorization", "Bearer token"),
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `searchUsers should return unauthorized when no auth header`() {
        // When/Then
        mockMvc.perform(
            get("/api/auth0/users")
                .param("search", "user"),
        )
            .andExpect(status().isUnauthorized)
    }
}
