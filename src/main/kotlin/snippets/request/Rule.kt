package snippets.dto.request

import com.fasterxml.jackson.annotation.JsonProperty

data class Rule(
    @JsonProperty("id") val id: String = "",
    @JsonProperty("name") val name: String = "",
    @JsonProperty("isActive") val isActive: Boolean = false,
    @JsonProperty("value") val value: Any? = null,
)
