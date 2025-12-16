package snippets.dto.request

data class SaveRulesReq(
    val rules: List<Rule>,
    val configText: String? = null,
    val configFormat: String? = null,
)
