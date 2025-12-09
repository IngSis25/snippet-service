package snippets.factories

import org.springframework.stereotype.Component
import snippets.dto.request.Rule

/**
 * Factory para crear las reglas del formatter disponibles.
 * Basado en el RulesFactory del proyecto PrintScript.
 */
@Component
class FormatterRulesFactory {

    /**
     * Obtiene todas las reglas disponibles del formatter para una versión específica.
     * @param version Versión del lenguaje (1.0 o 1.1)
     * @return Lista de reglas disponibles con sus valores por defecto
     */
    fun getAvailableRules(version: String): List<Rule> {
        return when (version) {
            "1.0" -> createRulesForV10()
            "1.1" -> createRulesForV11()
            else -> error("Unsupported version: $version")
        }
    }

    /**
     * Reglas disponibles para la versión 1.0
     */
    private fun createRulesForV10(): List<Rule> {
        return listOf(
            Rule(
                id = "space_before_colon",
                name = "space_before_colon",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "space_after_colon",
                name = "space_after_colon",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "newline_after_println",
                name = "newline_after_println",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "newline_before_println",
                name = "newline_before_println",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "space_around_equals",
                name = "space_around_equals",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "no_space_around_equals",
                name = "no_space_around_equals",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "single_space_separation",
                name = "single_space_separation",
                isActive = false,
                value = null,
            ),
        )
    }

    /**
     * Reglas disponibles para la versión 1.1
     * Orden exacto según RulesFactory del PrintScript
     */
    private fun createRulesForV11(): List<Rule> {
        return listOf(
            Rule(
                id = "space_before_colon",
                name = "space_before_colon",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "space_after_colon",
                name = "space_after_colon",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "newline_after_println",
                name = "newline_after_println",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "newline_before_println",
                name = "newline_before_println",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "space_around_equals",
                name = "space_around_equals",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "no_space_around_equals",
                name = "no_space_around_equals",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "number_of_spaces_indentation",
                name = "number_of_spaces_indentation",
                isActive = false,
                value = 2, // valor por defecto: 2 espacios
            ),
            Rule(
                id = "same_line_for_if_brace",
                name = "same_line_for_if_brace",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "same_line_for_else_brace",
                name = "same_line_for_else_brace",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "new_line_for_if_brace",
                name = "new_line_for_if_brace",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "single_space_separation",
                name = "single_space_separation",
                isActive = false,
                value = null,
            ),
        )
    }

    /**
     * Obtiene los IDs de todas las reglas disponibles para una versión.
     * Útil para inicializar enabledJson en FormatterRulesState.
     */
    fun getAvailableRuleIds(version: String): List<String> {
        return getAvailableRules(version).map { it.id }
    }
}

