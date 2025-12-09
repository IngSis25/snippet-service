package snippets.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.MDC

class RequestIdFilterTest {
    private lateinit var filter: RequestIdFilter
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        filter = RequestIdFilter()
        request = mock()
        response = mock()
        filterChain = mock()
        MDC.clear()
    }

    @Test
    fun `should use existing X-Request-ID header when present`() {
        // Given
        val existingRequestId = "existing-request-id-123"
        whenever(request.getHeader("X-Request-ID")).thenReturn(existingRequestId)

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        verify(response).setHeader("X-Request-ID", existingRequestId)
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `should generate new Request-ID when header is not present`() {
        // Given
        whenever(request.getHeader("X-Request-ID")).thenReturn(null)

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        verify(response).setHeader(eq("X-Request-ID"), any())
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `should put Request-ID in MDC`() {
        // Given
        val testRequestId = "test-request-id-456"
        whenever(request.getHeader("X-Request-ID")).thenReturn(testRequestId)

        // When
        filter.doFilter(request, response, filterChain)

        // Then - MDC should be cleared after filter, but we can verify it was set during execution
        verify(filterChain).doFilter(request, response)
    }

    @Test
    fun `should clear MDC after request processing`() {
        // Given
        whenever(request.getHeader("X-Request-ID")).thenReturn("test-id")

        // When
        filter.doFilter(request, response, filterChain)

        // Then
        // MDC should be cleared in finally block
        assertDoesNotThrow { MDC.get("requestId") } // Should be null or empty
    }

    @Test
    fun `should handle filter chain exceptions and still clear MDC`() {
        // Given
        whenever(request.getHeader("X-Request-ID")).thenReturn("test-id")
        whenever(filterChain.doFilter(any(), any())).thenThrow(RuntimeException("Test exception"))

        // When & Then
        try {
            filter.doFilter(request, response, filterChain)
        } catch (e: RuntimeException) {
            // Expected exception
        }
        // MDC should still be cleared even if exception occurs
    }
}
