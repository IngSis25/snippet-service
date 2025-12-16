package service

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import snippets.errors.LanguageNotFound
import snippets.model.Language
import snippets.repositories.LanguageRepository
import snippets.service.LanguageService
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class LanguageServiceTest {
    @Mock
    private lateinit var languageRepository: LanguageRepository

    @InjectMocks
    private lateinit var languageService: LanguageService

    private val language: Language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")

    @Test
    fun `getAll should return all languages`() {
        // Given
        val languages = listOf(language)
        whenever(languageRepository.findAll()).thenReturn(languages)

        // When
        val result = languageService.getAll()

        // Then
        result shouldBeEqualTo languages
        verify(languageRepository).findAll()
    }

    @Test
    fun `getLanguageById should return language when found`() {
        // Given
        val languageId = 1L
        whenever(languageRepository.findById(languageId)).thenReturn(Optional.of(language))

        // When
        val result = languageService.getLanguageById(languageId)

        // Then
        result shouldBeEqualTo language
        verify(languageRepository).findById(languageId)
    }

    @Test
    fun `getLanguageById should throw LanguageNotFound when language not found`() {
        // Given
        val languageId = 999L
        whenever(languageRepository.findById(languageId)).thenReturn(Optional.empty())

        // When/Then
        try {
            languageService.getLanguageById(languageId)
            org.junit.jupiter.api.Assertions.fail("Should have thrown LanguageNotFound")
        } catch (e: LanguageNotFound) {
            // Expected
        }
        verify(languageRepository).findById(languageId)
    }

    @Test
    fun `getLanguageById should throw LanguageNotFound when id is null`() {
        // When/Then
        try {
            languageService.getLanguageById(null)
            org.junit.jupiter.api.Assertions.fail("Should have thrown LanguageNotFound")
        } catch (e: LanguageNotFound) {
            // Expected
        }
    }

    @Test
    fun `getLanguageByName should return language when found`() {
        // Given
        val languageName = "PrintScript"
        whenever(languageRepository.findByNameIgnoreCase(languageName)).thenReturn(Optional.of(language))

        // When
        val result = languageService.getLanguageByName(languageName)

        // Then
        result shouldBeEqualTo language
        verify(languageRepository).findByNameIgnoreCase(languageName)
    }

    @Test
    fun `getLanguageByName should return null when not found`() {
        // Given
        val languageName = "NonExistent"
        whenever(languageRepository.findByNameIgnoreCase(languageName)).thenReturn(Optional.empty())

        // When
        val result = languageService.getLanguageByName(languageName)

        // Then
        result shouldBeEqualTo null
        verify(languageRepository).findByNameIgnoreCase(languageName)
    }

    @Test
    fun `getLanguageByName should return null when name is null`() {
        // When
        val result = languageService.getLanguageByName(null)

        // Then
        result shouldBeEqualTo null
    }

    @Test
    fun `getLanguageByName should return null when name is blank`() {
        // When
        val result = languageService.getLanguageByName("")

        // Then
        result shouldBeEqualTo null
    }

    @Test
    fun `getLanguageByExtension should return language when found`() {
        // Given
        val extension = "ps"
        whenever(languageRepository.findByExtensionIgnoreCase(extension)).thenReturn(Optional.of(language))

        // When
        val result = languageService.getLanguageByExtension(extension)

        // Then
        result shouldBeEqualTo language
        verify(languageRepository).findByExtensionIgnoreCase(extension)
    }

    @Test
    fun `getLanguageByExtension should return null when not found`() {
        // Given
        val extension = "xyz"
        whenever(languageRepository.findByExtensionIgnoreCase(extension)).thenReturn(Optional.empty())

        // When
        val result = languageService.getLanguageByExtension(extension)

        // Then
        result shouldBeEqualTo null
        verify(languageRepository).findByExtensionIgnoreCase(extension)
    }

    @Test
    fun `getLanguageByExtension should return null when extension is null`() {
        // When
        val result = languageService.getLanguageByExtension(null)

        // Then
        result shouldBeEqualTo null
    }

    @Test
    fun `getLanguageByExtension should return null when extension is blank`() {
        // When
        val result = languageService.getLanguageByExtension("")

        // Then
        result shouldBeEqualTo null
    }

    @Test
    fun `create should save and return language`() {
        // Given
        val newLanguage = Language(id = 0L, name = "NewLanguage", version = "1.0", extension = "nl")
        val savedLanguage = newLanguage.copy(id = 2L)

        whenever(languageRepository.save(newLanguage)).thenReturn(savedLanguage)

        // When
        val result = languageService.create(newLanguage)

        // Then
        result shouldBeEqualTo savedLanguage
        result.id shouldBeEqualTo 2L
        verify(languageRepository).save(newLanguage)
    }

    @Test
    fun `getLanguageByNameAndVersion should return language when found`() {
        // Given
        whenever(languageRepository.findByNameIgnoreCaseAndVersion("PrintScript", "1.0"))
            .thenReturn(Optional.of(language))

        // When
        val result = languageService.getLanguageByNameAndVersion("PrintScript", "1.0")

        // Then
        result shouldBeEqualTo language
        verify(languageRepository).findByNameIgnoreCaseAndVersion("PrintScript", "1.0")
    }

    @Test
    fun `getLanguageByNameAndVersion should return null when not found`() {
        // Given
        whenever(languageRepository.findByNameIgnoreCaseAndVersion(any(), any()))
            .thenReturn(Optional.empty())

        // When
        val result = languageService.getLanguageByNameAndVersion("Unknown", "1.0")

        // Then
        result shouldBeEqualTo null
    }

    @Test
    fun `getLanguageByNameAndVersion should return null when name is null`() {
        // When
        val result = languageService.getLanguageByNameAndVersion(null, "1.0")

        // Then
        result shouldBeEqualTo null
    }

    @Test
    fun `getLanguageByNameAndVersion should return null when version is null`() {
        // When
        val result = languageService.getLanguageByNameAndVersion("PrintScript", null)

        // Then
        result shouldBeEqualTo null
    }

    @Test
    fun `getLanguageByNameAndVersion should return null when name is blank`() {
        // When
        val result = languageService.getLanguageByNameAndVersion("", "1.0")

        // Then
        result shouldBeEqualTo null
    }

    @Test
    fun `getLanguageByNameAndVersion should return null when version is blank`() {
        // When
        val result = languageService.getLanguageByNameAndVersion("PrintScript", "")

        // Then
        result shouldBeEqualTo null
    }
}
