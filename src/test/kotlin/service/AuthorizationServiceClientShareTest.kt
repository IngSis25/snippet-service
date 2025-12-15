package service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import snippets.dto.response.FullSnippet
import snippets.service.AuthorizationServiceClient

@ExtendWith(MockitoExtension::class)
class AuthorizationServiceClientShareTest {
    @Mock
    private lateinit var restTemplate: RestTemplate

    private lateinit var authorizationServiceClient: AuthorizationServiceClient

    @BeforeEach
    fun setUp() {
        authorizationServiceClient = AuthorizationServiceClient(restTemplate, "http://auth-service")
    }

    @Test
    fun `shareSnippet should return ok when successful`() {
        // Given
        val snippet = FullSnippet()
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(ResponseEntity.ok("User is the owner of the snippet"))
            .thenReturn(ResponseEntity.ok("Snippet added"))

        // When
        val result =
            authorizationServiceClient.shareSnippet(
                "token",
                1L,
                "from@example.com",
                "to@example.com",
                snippet,
                "Guest",
            )

        // Then
        assert(result.statusCode == HttpStatus.OK)
    }

    @Test
    fun `shareSnippet should return bad request when sharing with self`() {
        // Given
        val snippet = FullSnippet()

        // When
        val result =
            authorizationServiceClient.shareSnippet(
                "token",
                1L,
                "user@example.com",
                "user@example.com",
                snippet,
                "Guest",
            )

        // Then
        assert(result.statusCode == HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `shareSnippet should return forbidden when user is not owner`() {
        // Given
        val snippet = FullSnippet()
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(ResponseEntity.ok("User is not the owner"))

        // When
        val result =
            authorizationServiceClient.shareSnippet(
                "token",
                1L,
                "from@example.com",
                "to@example.com",
                snippet,
                "Guest",
            )

        // Then
        assert(result.statusCode == HttpStatus.FORBIDDEN)
    }

    @Test
    fun `shareSnippet should handle errors when adding snippet to user fails`() {
        // Given
        val snippet = FullSnippet()
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(ResponseEntity.ok("User is the owner of the snippet"))
            .thenReturn(ResponseEntity.ok(null))

        // When
        val result =
            authorizationServiceClient.shareSnippet(
                "token",
                1L,
                "from@example.com",
                "to@example.com",
                snippet,
                "Guest",
            )

        // Then
        assert(result.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `shareSnippet should handle exceptions when adding snippet to user`() {
        // Given
        val snippet = FullSnippet()
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(ResponseEntity.ok("User is the owner of the snippet"))
            .thenThrow(RuntimeException("Error"))

        // When
        val result =
            authorizationServiceClient.shareSnippet(
                "token",
                1L,
                "from@example.com",
                "to@example.com",
                snippet,
                "Guest",
            )

        // Then
        assert(result.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
