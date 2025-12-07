package snippets.config

data class SnippetMessage(
    val snippetId: Long,
    val userId: String,
    val version: String,
    val jwtToken: String,
)
