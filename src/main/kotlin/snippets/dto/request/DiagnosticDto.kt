package snippets.dto.request

data class DiagnosticDto(
    val code: String,
    val message: String,
    val severity: String,
    val line: Int,
    val column: Int,
    val suggestions: List<String> = emptyList(),
)

