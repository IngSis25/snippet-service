package snippets.config

import org.slf4j.MDC
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse

/**
 * Interceptor para RestTemplate que propaga el Request-ID
 * en todas las llamadas HTTP salientes a otros servicios.
 */
class RequestIdInterceptor : ClientHttpRequestInterceptor {

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-ID"
        const val REQUEST_ID_MDC_KEY = "requestId"
    }

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        // Obtener el Request-ID del MDC (que fue establecido por el Filter)
        val requestId = MDC.get(REQUEST_ID_MDC_KEY)

        // Si existe, agregarlo al header de la request saliente
        if (requestId != null) {
            request.headers.set(REQUEST_ID_HEADER, requestId)
        }

        // Continuar con la ejecución
        return execution.execute(request, body)
    }
}

