package snippets.errors

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleRuntimeException should return 500 with error details`() {
        // Given
        val exception = RuntimeException("Test error message")

        // When
        val response: ResponseEntity<Map<String, String>> = handler.handleRuntimeException(exception)

        // Then
        assert(response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
        assert(response.body != null)
        assert(response.body!!["error"] == "Internal Server Error")
        assert(response.body!!["message"] == "Test error message")
        assert(response.body!!["type"] == "RuntimeException")
    }

    @Test
    fun `handleRuntimeException should handle null message`() {
        // Given
        val exception = RuntimeException()

        // When
        val response: ResponseEntity<Map<String, String>> = handler.handleRuntimeException(exception)

        // Then
        assert(response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
        assert(response.body != null)
        assert(response.body!!["error"] == "Internal Server Error")
        assert(response.body!!["message"] == "Unknown error")
        assert(response.body!!["type"] == "RuntimeException")
    }

    @Test
    fun `handleRuntimeException should handle custom exception types`() {
        // Given
        val exception = IllegalArgumentException("Custom error")

        // When
        val response: ResponseEntity<Map<String, String>> = handler.handleRuntimeException(exception)

        // Then
        assert(response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR)
        assert(response.body != null)
        assert(response.body!!["type"] == "IllegalArgumentException")
    }
}
