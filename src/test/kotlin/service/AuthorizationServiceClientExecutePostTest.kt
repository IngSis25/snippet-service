package service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import snippets.service.AuthorizationServiceClient

@ExtendWith(MockitoExtension::class)
class AuthorizationServiceClientExecutePostTest {
    @Mock
    private lateinit var restTemplate: RestTemplate

    private lateinit var authorizationServiceClient: AuthorizationServiceClient

    @BeforeEach
    fun setUp() {
        authorizationServiceClient = AuthorizationServiceClient(restTemplate, "http://auth-service")
    }

    @Test
    fun `addSnippetToUser should throw when result is null`() {
        // Given
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(ResponseEntity.ok(null))

        // When/Then
        try {
            authorizationServiceClient.addSnippetToUser("token", "user@example.com", 1L, "Owner")
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("Failed to add snippet") == true)
        }
    }

    @Test
    fun `addSnippetToUser should succeed when result is not null`() {
        // Given
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(ResponseEntity.ok("Success"))

        // When - should not throw
        authorizationServiceClient.addSnippetToUser("token", "user@example.com", 1L, "Owner")

        // Then
        verify(restTemplate).exchange(any<String>(), eq(HttpMethod.POST), any<HttpEntity<*>>(), eq(String::class.java))
    }

    @Test
    fun `addSnippetToUser should handle non-2xx responses`() {
        // Given
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error"))

        // When/Then
        try {
            authorizationServiceClient.addSnippetToUser("token", "user@example.com", 1L, "Owner")
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("Failed to add snippet") == true)
        }
    }

    @Test
    fun `addSnippetToUser should handle exceptions`() {
        // Given
        whenever(
            restTemplate.exchange(
                any<String>(),
                eq(HttpMethod.POST),
                any<HttpEntity<*>>(),
                eq(String::class.java),
            ),
        ).thenThrow(RuntimeException("Connection error"))

        // When/Then
        try {
            authorizationServiceClient.addSnippetToUser("token", "user@example.com", 1L, "Owner")
            assert(false) { "Should have thrown RuntimeException" }
        } catch (e: RuntimeException) {
            assert(e.message?.contains("Failed to add snippet") == true)
        }
    }
}
