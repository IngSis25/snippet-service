package snippets.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import snippets.model.Language
import snippets.service.LanguageService

@WebMvcTest(
    controllers = [LanguageController::class],
    excludeAutoConfiguration = [SecurityAutoConfiguration::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.security.oauth2.resourceserver.jwt.issuer-uri=", "auth0.audience="])
class LanguageControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var languageService: LanguageService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var language: Language

    @BeforeEach
    fun setUp() {
        language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
    }

    @Test
    fun `getAll should return list of languages`() {
        // Given
        val languages = listOf(language)
        whenever(languageService.getAll()).thenReturn(languages)

        // When/Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/languages/all"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(1L))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("PrintScript"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].version").value("1.0"))
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].extension").value("ps"))
    }

    @Test
    fun `create should create and return language`() {
        // Given
        whenever(languageService.create(any())).thenReturn(language)

        // When/Then
        mockMvc.perform(
            MockMvcRequestBuilders.post("/api/languages/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(language)),
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(1L))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("PrintScript"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.version").value("1.0"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.extension").value("ps"))
    }
}
