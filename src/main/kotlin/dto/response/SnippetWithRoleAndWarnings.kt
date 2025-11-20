package dto.response

import model.Compliance
import model.Snippet

class SnippetWithRoleAndWarnings(
    val snippet: Snippet,
    val role: String,
    private val warnings: List<String>,
) {
    val id: Long = snippet.id
    val name: String = snippet.name
    val owner: String = snippet.owner
    val language: String = snippet.language.name
    val extension: String = snippet.language.extension
    val version: String = snippet.language.version
    val status: Compliance = snippet.status
    val lintWarnings: List<String> = warnings

    constructor() : this(Snippet(), "Default", emptyList())
}
