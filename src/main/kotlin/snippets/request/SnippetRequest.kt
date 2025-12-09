package snippets.dto.request

// crear snippets
data class SnippetRequest(
    val name: String,
    val content: String,
    val languageId: String? = null,
    val language: String? = null,
    val extension: String? = null,
    val version: String? = null,
    val owner: String,
)
