package snippets.service

import snippets.dto.response.FullSnippet

interface SnippetServiceRoutes {
    fun create(
        name: String,
        content: String,
        languageId: String,
        owner: String,
        token: String,
    ): FullSnippet

    fun get(id: Long): FullSnippet

    fun update(
        id: Long,
        content: String,
        token: String,
    ): FullSnippet

    fun delete(
        directory: String,
        id: Long,
    )

    fun checkIfExists(
        id: Long,
        operation: String,
    )
}
