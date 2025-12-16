package snippets.dto.request

data class ShareRequest(
    val fromEmail: String,
    val toEmail: String,
    // Default: Editor (Full Access). Options: "Editor" (read+write), "Viewer" (read-only)
    val role: String = "Editor",
)
