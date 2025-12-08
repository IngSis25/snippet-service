package snippets.config

data class TestMessage(
    val testId: Long? = null,
    val snippetId: Long,
    val userId: String,
    val version: String,
    val jwtToken: String,
    val inputs: List<String>? = null,
    val outputs: List<String>? = null,
)
