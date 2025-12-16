package snippets.dto.response

import snippets.model.Compliance

data class SnippetDetailDto(
    val id: Long,
    val name: String,
    val owner: String,
    val language: String,
    val extension: String,
    val version: String,
    val content: String,
    val compliance: Compliance,
    val lintCount: Int,
    val isValid: Boolean,
    val warnings: List<String> = emptyList(),
)
