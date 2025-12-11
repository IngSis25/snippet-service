package model

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import snippets.model.Language

class LanguageTest {
    @Test
    fun `Language should have all properties`() {
        // When
        val language =
            Language(
                id = 1L,
                name = "PrintScript",
                version = "1.0",
                extension = "ps",
            )

        // Then
        language.id shouldBeEqualTo 1L
        language.name shouldBeEqualTo "PrintScript"
        language.version shouldBeEqualTo "1.0"
        language.extension shouldBeEqualTo "ps"
    }

    @Test
    fun `Language should have default constructor`() {
        // When
        val language = Language()

        // Then
        language.id shouldBeEqualTo 0L
        language.name shouldBeEqualTo ""
        language.version shouldBeEqualTo ""
        language.extension shouldBeEqualTo ""
    }
}
