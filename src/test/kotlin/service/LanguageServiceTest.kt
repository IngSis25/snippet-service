package service

import errors.LanguageNotFound
import model.Language
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import repositories.LanguageRepository
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
}
