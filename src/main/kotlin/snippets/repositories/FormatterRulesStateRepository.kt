package snippets.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import snippets.enums.RulesType
import snippets.model.FormatterRulesState
import java.util.UUID

@Repository
interface FormatterRulesStateRepository : JpaRepository<FormatterRulesState, UUID> {
    fun findByTypeAndOwnerId(
        type: RulesType,
        ownerId: String?,
    ): FormatterRulesState?

    fun findByTypeAndOwnerIdIsNull(type: RulesType): FormatterRulesState?
}
