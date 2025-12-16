package snippets.service

import org.springframework.stereotype.Service
import snippets.errors.LanguageNotFound
import snippets.model.Language
import snippets.repositories.LanguageRepository

@Service
class LanguageService(
    private val languageRepository: LanguageRepository,
) {
    fun getAll(): List<Language> {
        return languageRepository.findAll()
    }

    fun create(language: Language): Language {
        return languageRepository.save(language)
    }

    fun getLanguageById(id: Long?): Language {
        if (id == null) {
            throw LanguageNotFound("Language not found when trying to get it")
        }
        return languageRepository.findById(id)
            .orElseThrow { LanguageNotFound("Language not found when trying to get it") }
    }

    fun getLanguageByName(name: String?): Language? {
        if (name.isNullOrBlank()) {
            return null
        }
        return languageRepository.findByNameIgnoreCase(name)
            .orElse(null)
    }

    fun getLanguageByExtension(extension: String?): Language? {
        if (extension.isNullOrBlank()) {
            return null
        }
        return languageRepository.findByExtensionIgnoreCase(extension)
            .orElse(null)
    }

    fun getLanguageByNameAndVersion(
        name: String?,
        version: String?,
    ): Language? {
        if (name.isNullOrBlank() || version.isNullOrBlank()) {
            return null
        }
        return languageRepository.findByNameIgnoreCaseAndVersion(name, version)
            .orElse(null)
    }
}
