package snippets.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import snippets.enums.RulesType
import java.util.UUID

@Entity
@Table(
    name = "formatter_rules_state",
    uniqueConstraints = [
        // una fila por (tipo, owner) — ownerId null = GLOBAL
        UniqueConstraint(name = "uq_formatter_rules_scope", columnNames = ["type", "owner_id"]),
    ],
)
class FormatterRulesState(
    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(columnDefinition = "uuid")
    var id: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: RulesType,
    @Column(name = "owner_id", length = 64)
    var ownerId: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enabled_json", columnDefinition = "jsonb", nullable = false)
    var enabledJson: List<String> = emptyList(),
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", columnDefinition = "jsonb")
    var optionsJson: Map<String, Any?>? = null,
    @Column(name = "config_text", columnDefinition = "text")
    var configText: String? = null,
    @Column(name = "config_format", length = 32)
    var configFormat: String? = null,
)
