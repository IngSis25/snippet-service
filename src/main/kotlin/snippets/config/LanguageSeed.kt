package snippets.config

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import snippets.model.Language
import snippets.repositories.LanguageRepository

@Configuration
class LanguageSeed {
    private val log = LoggerFactory.getLogger(LanguageSeed::class.java)

    @Bean
    fun seedLanguages(languageRepository: LanguageRepository): CommandLineRunner {
        return CommandLineRunner {
            if (languageRepository.count() > 0) {
                log.info("🌱 Languages already present, skipping seed")
                return@CommandLineRunner
            }

            log.info("🌱 Seeding languages...")

            val languages =
                listOf(
                    Language(name = "PrintScript", version = "1.0", extension = ".ps"),
                    Language(name = "PrintScript", version = "1.1", extension = ".ps"),
                    Language(name = "JavaScript", version = "ES6", extension = ".js"),
                    Language(name = "Python", version = "3.10", extension = ".py"),
                    Language(name = "Kotlin", version = "3.6", extension = ".kt"),
                )

            languageRepository.saveAll(languages)

            log.info("✅ Language seed completed: ${languages.size} entries")
        }
    }
}
