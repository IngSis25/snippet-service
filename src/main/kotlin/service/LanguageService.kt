package service

import errors.LanguageNotFound
import model.Language
import org.springframework.stereotype.Service
import repositories.LanguageRepository

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
}
