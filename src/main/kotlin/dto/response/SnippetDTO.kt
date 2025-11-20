package dto.response

import model.Compliance
import model.Snippet

open class SnippetDTO(snippet: Snippet) {
    val id: Long = snippet.id
    val name: String = snippet.name
    val owner: String = snippet.owner
    val language: String = snippet.language.name
    val extension: String = snippet.language.extension
    val compliance: Compliance = snippet.status
    val version: String = snippet.language.version
}
