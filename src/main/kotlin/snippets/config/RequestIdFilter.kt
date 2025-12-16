package snippets.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Filter que captura o genera un Request-ID y lo propaga a través de:
 * 1. MDC (Mapped Diagnostic Context) para que aparezca en los logs
 * 2. Response header para que el cliente pueda verlo
 * 3. Está disponible para ser propagado en llamadas HTTP salientes
 */
@Component
@Order(1) // Ejecutar antes que otros filters
class RequestIdFilter : Filter {
    companion object {
        const val REQUEST_ID_HEADER = "X-Request-ID"
        const val REQUEST_ID_MDC_KEY = "requestId"
    }

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        try {
            // Obtener Request-ID del header o generar uno nuevo
            val requestId =
                httpRequest.getHeader(REQUEST_ID_HEADER)
                    ?: UUID.randomUUID().toString()

            // Agregar al MDC para que aparezca en los logs
            MDC.put(REQUEST_ID_MDC_KEY, requestId)

            // Agregar al response header para que el cliente pueda verlo
            httpResponse.setHeader(REQUEST_ID_HEADER, requestId)

            // Continuar con la cadena de filters
            chain.doFilter(request, response)
        } finally {
            // Limpiar MDC al finalizar la request
            MDC.clear()
        }
    }
}
