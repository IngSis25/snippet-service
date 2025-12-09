package snippets.config

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpResponse

class RequestIdInterceptorTest {
    private lateinit var interceptor: RequestIdInterceptor
    private lateinit var request: HttpRequest
    private lateinit var execution: ClientHttpRequestExecution
    private lateinit var response: ClientHttpResponse

    @BeforeEach
    fun setUp() {
        interceptor = RequestIdInterceptor()
        request = mock()
        execution = mock()
        response = mock()
        MDC.clear()
    }

    @Test
    fun `should add Request-ID header when present in MDC`() {
        // Given
        val requestId = "test-request-id-789"
        MDC.put("requestId", requestId)
        val headers = HttpHeaders()
        whenever(request.headers).thenReturn(headers)
        whenever(execution.execute(any(), any())).thenReturn(response)

        // When
        interceptor.intercept(request, ByteArray(0), execution)

        // Then
        verify(request).headers
        assert(headers["X-Request-ID"]?.contains(requestId) == true)
        verify(execution).execute(request, ByteArray(0))
    }

    @Test
    fun `should not add header when Request-ID is not in MDC`() {
        // Given
        MDC.clear() // Ensure MDC is empty
        val headers = HttpHeaders()
        whenever(request.headers).thenReturn(headers)
        whenever(execution.execute(any(), any())).thenReturn(response)

        // When
        interceptor.intercept(request, ByteArray(0), execution)

        // Then
        verify(execution).execute(request, ByteArray(0))
        // Header should not be added if MDC is empty
        assert(headers["X-Request-ID"] == null)
    }

    @Test
    fun `should propagate Request-ID to outgoing HTTP request`() {
        // Given
        val requestId = "propagated-request-id"
        MDC.put("requestId", requestId)
        val headers = HttpHeaders()
        whenever(request.headers).thenReturn(headers)
        whenever(execution.execute(any(), any())).thenReturn(response)

        // When
        interceptor.intercept(request, ByteArray(0), execution)

        // Then
        assert(headers["X-Request-ID"]?.first() == requestId)
        verify(execution).execute(request, ByteArray(0))
    }
}
