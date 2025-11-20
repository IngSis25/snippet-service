package dto.request

// crear snippets
data class SnippetRequest(
    val name: String,
    val content: String,
    val languageId: String,
    val owner: String
)

