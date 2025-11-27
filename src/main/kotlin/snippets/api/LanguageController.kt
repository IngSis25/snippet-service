package snippets.api

import snippets.model.Language
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import snippets.service.LanguageService

@RestController
@RequestMapping("/api/languages")
class LanguageController(
    private val languageService: LanguageService,
) {
    @GetMapping("/all")
    fun getAll(): ResponseEntity<List<Language>> {
        val languages = languageService.getAll()
        return ResponseEntity.ok(languages)
    }

    @PostMapping("/")
    fun create(
        @RequestBody language: Language,
    ): ResponseEntity<Language> {
        val createdLanguage = languageService.create(language)
        return ResponseEntity.ok(createdLanguage)
    }
}
