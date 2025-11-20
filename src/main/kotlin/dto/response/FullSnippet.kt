package dto.response

import model.Compliance
import model.Snippet

class FullSnippet(snippet: Snippet, val content: String, val errors: List<String>) {
    val id: Long = snippet.id
    val name: String = snippet.name
    val owner: String = snippet.owner
    val language: String = snippet.language.name
    val extension: String = snippet.language.extension
    val status: Compliance = snippet.status
    val version: String = snippet.language.version

    constructor() : this(Snippet(), "", emptyList())
    constructor(snippet: Snippet, content: String) : this(snippet, content, emptyList())
}
