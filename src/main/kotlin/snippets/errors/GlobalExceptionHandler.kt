package snippets.errors

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class GlobalExceptionHandler {

    /**
     * Manejador global de excepciones para asegurar que todas las excepciones
     * se conviertan en respuestas HTTP apropiadas que New Relic pueda capturar.
     */
    @ExceptionHandler(RuntimeException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<Map<String, String>> {
        // Log del error para debugging
        println("Error capturado por GlobalExceptionHandler: ${ex.message}")
        
        // Devolver un 500 con el mensaje de error
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf(
                "error" to "Internal Server Error",
                "message" to (ex.message ?: "Unknown error"),
                "type" to ex.javaClass.simpleName
            ))
    }
}

