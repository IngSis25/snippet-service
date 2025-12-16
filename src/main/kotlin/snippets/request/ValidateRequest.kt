package snippets.dto.request

// validar codigo
data class ValidateRequest(
    val version: String,
    val code: String,
)
