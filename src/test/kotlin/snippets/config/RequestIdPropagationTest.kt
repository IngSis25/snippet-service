package snippets.config

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators
import org.springframework.web.client.RestTemplate

/**
 * Test para verificar que el RequestIdInterceptor propaga el Request-ID
 * en las llamadas HTTP salientes realizadas con RestTemplate.
 */
class RequestIdPropagationTest {
    private lateinit var restTemplate: RestTemplate
    private lateinit var mockServer: MockRestServiceServer
    private lateinit var interceptor: RequestIdInterceptor

    @BeforeEach
    fun setUp() {
        restTemplate = RestTemplate()
        interceptor = RequestIdInterceptor()
        restTemplate.interceptors.add(interceptor)
        mockServer = MockRestServiceServer.createServer(restTemplate)
        MDC.clear()
    }

    @Test
    fun `should propagate Request-ID from MDC to outgoing HTTP request`() {
        // Given
        val requestId = "propagation-test-request-id"
        MDC.put("requestId", requestId)

        mockServer.expect(MockRestRequestMatchers.requestTo("http://test-service/api/test"))
            .andExpect(MockRestRequestMatchers.header("X-Request-ID", requestId))
            .andRespond(
                MockRestResponseCreators.withSuccess(
                    "Success",
                    MediaType.APPLICATION_JSON,
                ),
            )

        // When
        restTemplate.getForEntity("http://test-service/api/test", String::class.java)

        // Then
        mockServer.verify()
    }

    @Test
    fun `should not add Request-ID header when not in MDC`() {
        // Given
        MDC.clear() // Ensure MDC is empty

        mockServer.expect(MockRestRequestMatchers.requestTo("http://test-service/api/test"))
            .andExpect(MockRestRequestMatchers.headerDoesNotExist("X-Request-ID"))
            .andRespond(
                MockRestResponseCreators.withSuccess(
                    "Success",
                    MediaType.APPLICATION_JSON,
                ),
            )

        // When
        restTemplate.getForEntity("http://test-service/api/test", String::class.java)

        // Then
        mockServer.verify()
    }

    @Test
    fun `should propagate Request-ID in POST requests`() {
        // Given
        val requestId = "post-request-id"
        MDC.put("requestId", requestId)

        mockServer.expect(MockRestRequestMatchers.requestTo("http://test-service/api/create"))
            .andExpect(MockRestRequestMatchers.header("X-Request-ID", requestId))
            .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
            .andRespond(
                MockRestResponseCreators.withSuccess(
                    "Created",
                    MediaType.APPLICATION_JSON,
                ),
            )

        // When
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val entity = HttpEntity(mapOf("key" to "value"), headers)
        restTemplate.exchange(
            "http://test-service/api/create",
            HttpMethod.POST,
            entity,
            String::class.java,
        )

        // Then
        mockServer.verify()
    }
}
