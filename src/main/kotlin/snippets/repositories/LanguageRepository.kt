package snippets.repositories

import snippets.model.Language
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface LanguageRepository : JpaRepository<Language, Long> {
    fun findByNameIgnoreCase(name: String): Optional<Language>
    fun findByExtensionIgnoreCase(extension: String): Optional<Language>
}
