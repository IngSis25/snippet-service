package service

import config.SnippetMessage
import dto.response.FullSnippet
import dto.response.SnippetUserDto
import dto.response.SnippetWithRoleAndWarnings
import errors.SnippetNotFound
import kotlinx.coroutines.runBlocking
import model.Compliance
import model.Snippet
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import repositories.SnippetRepository

@Service
class SnippetService(
    private val snippetRepository: SnippetRepository,
    @Lazy private val authorizationServiceClient: AuthorizationServiceClient,
    private val languageService: LanguageService,
    private val runnerServiceProducer: RunnerServiceProducer
) : SnippetServiceRoutes {

    override fun create(name: String, content: String, languageId: String, owner: String, token: String): FullSnippet {
        val language = languageService.getLanguageById(languageId.toLongOrNull())
        val snippet = Snippet(name = name, language = language, owner = owner, content = content)
        snippetRepository.save(snippet)
        authorizationServiceClient.addSnippetToUser(token, owner, snippet.id, "Owner")
        
        // Publicar mensaje en Redis para que runner-service valide el snippet
        val userId = authorizationServiceClient.validate(token).body ?: 0L
        runBlocking {
            runnerServiceProducer.publishEvent(
                SnippetMessage(
                    snippetId = snippet.id,
                    userId = userId,
                    version = snippet.language.version,
                    jwtToken = token
                )
            )
        }
        
        return FullSnippet(snippet, content, emptyList())
    }

    override fun get(id: Long): FullSnippet {
        val snippet = snippetRepository.findById(id)
            .orElseThrow { SnippetNotFound("Snippet not found when trying to get it") }
        
        // Por ahora los warnings están vacíos, se pueden agregar como campo en Snippet si es necesario
        val warnings = emptyList<String>()

        return FullSnippet(snippet, snippet.content, warnings)
    }

    fun getFilteredSnippets(
        page: Int,
        pageSize: Int,
        snippetsIds: List<SnippetUserDto>,
        snippetName: String?,
        roles: List<String>?,
        languages: List<Long>?,
        compliance: List<Compliance>?
    ): Pair<List<SnippetWithRoleAndWarnings>, Long> {
        val snippetIdToRoleMap = snippetsIds.associateBy({ it.snippetId }, { it.role })
        if (snippetIdToRoleMap.isEmpty()) return Pair(emptyList(), 0)
        val filteredSnippetIdToRoleMap = if (!roles.isNullOrEmpty()) {
            snippetIdToRoleMap.filter { entry -> roles.contains(entry.value) }
        } else {
            snippetIdToRoleMap
        }

        val snippets = snippetRepository.findAllById(filteredSnippetIdToRoleMap.keys)

        val snippetsWithWarnings = snippets.map { snippet ->
            // Por ahora los warnings están vacíos, se pueden agregar como campo en Snippet si es necesario
            val warnings = emptyList<String>()

            SnippetWithRoleAndWarnings(
                snippet = snippet,
                role = snippetIdToRoleMap[snippet.id] ?: "Default",
                warnings = warnings,
            )
        }

        val snippetIdToWarnings = snippetsWithWarnings.associateBy({ it.id }, { it.lintWarnings })

        val filteredSnippets = snippets.filter { snippet ->
            (snippetName == null || snippet.name.contains(snippetName, ignoreCase = true)) &&
                (languages.isNullOrEmpty() || languages.contains(snippet.language.id)) &&
                (compliance.isNullOrEmpty() || compliance.contains(snippet.status))
        }

        val totalCount = filteredSnippets.size.toLong()

        val pagedSnippets = filteredSnippets
            .drop(page * pageSize)
            .take(pageSize)
            .map { snippet ->
                val role = filteredSnippetIdToRoleMap[snippet.id]!!
                val warnings = snippetIdToWarnings[snippet.id]!!
                SnippetWithRoleAndWarnings(snippet, role, warnings)
            }

        return Pair(pagedSnippets, totalCount)
    }

    override fun update(id: Long, content: String, token: String): FullSnippet {
        checkIfExists(id, "edit")
        val snippet = snippetRepository.findById(id).get()
        snippet.content = content
        snippetRepository.save(snippet)
        
        // Publicar mensaje en Redis para que runner-service valide el snippet actualizado
        val userId = authorizationServiceClient.validate(token).body ?: 0L
        runBlocking {
            runnerServiceProducer.publishEvent(
                SnippetMessage(
                    snippetId = snippet.id,
                    userId = userId,
                    version = snippet.language.version,
                    jwtToken = token
                )
            )
        }
        
        return FullSnippet(snippet, content)
    }

    override fun delete(directory: String, id: Long) {
        checkIfExists(id, "delete")
        snippetRepository.deleteById(id)
    }

    override fun checkIfExists(id: Long, operation: String) {
        if (!snippetRepository.existsById(id)) {
            throw SnippetNotFound("Snippet not found when trying to $operation it")
        }
    }

    fun countSnippets(snippetName: String?): Long {
        return if (!snippetName.isNullOrEmpty()) {
            snippetRepository.countByNameContainingIgnoreCase(snippetName)
        } else {
            snippetRepository.count()
        }
    }

    fun updateStatus(id: Long, status: Compliance): FullSnippet {
        val snippet = snippetRepository.findById(id).orElseThrow {
            RuntimeException("Snippet with ID $id not found")
        }
        snippet.status = status
        println("HIT snippet service: snippet $id status was updated to $status")

        val updatedSnippet = snippetRepository.save(snippet)
        return FullSnippet(updatedSnippet, updatedSnippet.content)
    }

    fun format(id: Long, content: String, token: String) {
        val userId = authorizationServiceClient.validate(token).body ?: return
        val snippet = get(id)
        
        // Publicar mensaje en Redis para que runner-service formatee el snippet
        runBlocking {
            runnerServiceProducer.publishEvent(
                SnippetMessage(
                    snippetId = snippet.id,
                    userId = userId,
                    version = snippet.version,
                    jwtToken = token
                )
            )
        }
    }
}

