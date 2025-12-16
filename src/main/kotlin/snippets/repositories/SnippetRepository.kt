package snippets.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import snippets.model.Snippet

@Repository
interface SnippetRepository : JpaRepository<Snippet, Long> {
    fun findByNameContainingIgnoreCase(
        name: String,
        pageable: Pageable,
    ): Page<Snippet>

    fun countByNameContainingIgnoreCase(name: String): Long
}
