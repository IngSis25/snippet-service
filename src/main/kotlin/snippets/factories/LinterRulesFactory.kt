package snippets.factories

import org.springframework.stereotype.Component
import snippets.dto.request.Rule

/**
 * Factory para crear las reglas del linter disponibles.
 * Basado en el AnalyzerVisitorsFactory del proyecto PrintScript.
 */
@Component
class LinterRulesFactory {
    /**
     * Obtiene todas las reglas disponibles del linter para una versión específica.
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
     * Según AnalyzerVisitorsFactory.createAnalyzerVisitorsV10FromJson:
     * - UnusedVariableCheck (siempre activo, no configurable)
     * - NamingFormatCheck (configurable)
     */
    private fun createRulesForV10(): List<Rule> {
        return listOf(
            Rule(
                id = "UnusedVariableCheck",
                name = "UnusedVariableCheck",
                isActive = true,
                value = null,
            ),
            Rule(
                id = "NamingFormatCheck",
                name = "NamingFormatCheck",
                isActive = false,
                value = "camelCase",
            ),
        )
    }

    /**
     * Reglas disponibles para la versión 1.1
     * Según AnalyzerVisitorsFactory.createAnalyzerVisitorsV11FromJson:
     * - UnusedVariableCheck (siempre activo, no configurable)
     * - NamingFormatCheck (configurable)
     * - PrintUseCheck (configurable)
     * - ReadInputCheck (configurable)
     */
    private fun createRulesForV11(): List<Rule> {
        return listOf(
            Rule(
                id = "UnusedVariableCheck",
                name = "UnusedVariableCheck",
                isActive = true,
                value = null,
            ),
            Rule(
                id = "NamingFormatCheck",
                name = "NamingFormatCheck",
                isActive = false,
                value = "camelCase",
            ),
            Rule(
                id = "PrintUseCheck",
                name = "PrintUseCheck",
                isActive = false,
                value = null,
            ),
            Rule(
                id = "ReadInputCheck",
                name = "ReadInputCheck",
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
