package snippets.dto.request

data class CreateTestDTO(
    val name: String,
    val input: List<String>,
    val output: List<String>,
)
