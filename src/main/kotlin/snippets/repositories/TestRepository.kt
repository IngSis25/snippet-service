package snippets.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import snippets.model.Test

@Repository
interface TestRepository : JpaRepository<Test, Long> {
    fun findBySnippetId(snippetId: Long): List<Test>
}
