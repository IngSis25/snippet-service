package service

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.ResponseEntity
import snippets.config.SnippetMessage
import snippets.dto.response.SnippetUserDto
import snippets.errors.SnippetNotFound
import snippets.model.Compliance
import snippets.model.Language
import snippets.model.Snippet
import snippets.repositories.SnippetRepository
import snippets.service.AssetService
import snippets.service.AuthorizationServiceClient
import snippets.service.LanguageService
import snippets.service.RunnerServiceProducer
import snippets.service.SnippetService
import snippets.service.TestService
import java.util.Optional
import snippets.model.Test as TestModel

@ExtendWith(MockitoExtension::class)
class SnippetServiceTest {
    @Mock
    private lateinit var snippetRepository: SnippetRepository

    @Mock
    private lateinit var authorizationServiceClient: AuthorizationServiceClient

    @Mock
    private lateinit var assetService: AssetService

    @Mock
    private lateinit var languageService: LanguageService

    @Mock
    private lateinit var runnerServiceProducer: RunnerServiceProducer

    @Mock
    private lateinit var testService: TestService

    @InjectMocks
    private lateinit var snippetService: SnippetService

    private lateinit var language: Language
    private lateinit var snippet: Snippet

    @BeforeEach
    fun setUp() {
        language = Language(id = 1L, name = "PrintScript", version = "1.0", extension = "ps")
        snippet =
            Snippet(
                id = 1L,
                name = "Test Snippet",
                owner = "test@example.com",
                status = Compliance.PENDING,
                language = language,
            )
    }

    @Test
    fun `get should return full snippet with content and warnings`() {
        // Given
        val snippetId = 1L
        val content = "print('Hello')"
        val warningsJson = """["warning1", "warning2"]"""

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(assetService.get("snippets", snippetId)).thenReturn(content)
        whenever(assetService.exists("lint-warnings", snippetId)).thenReturn(true)
        whenever(assetService.get("lint-warnings", snippetId)).thenReturn(warningsJson)

        // When
        val result = snippetService.get(snippetId)

        // Then
        result.id shouldBeEqualTo snippet.id
        result.content shouldBeEqualTo content
        result.errors.size shouldBeEqualTo 2
        verify(snippetRepository).findById(snippetId)
        verify(assetService).get("snippets", snippetId)
    }

    @Test
    fun `get should return full snippet without warnings when warnings do not exist`() {
        // Given
        val snippetId = 1L
        val content = "print('Hello')"

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(assetService.get("snippets", snippetId)).thenReturn(content)
        whenever(assetService.exists("lint-warnings", snippetId)).thenReturn(false)

        // When
        val result = snippetService.get(snippetId)

        // Then
        result.id shouldBeEqualTo snippet.id
        result.content shouldBeEqualTo content
        result.errors.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `get should throw SnippetNotFound when snippet not found`() {
        // Given
        val snippetId = 999L
        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.empty())

        // When/Then
        try {
            snippetService.get(snippetId)
            org.junit.jupiter.api.Assertions.fail("Should have thrown SnippetNotFound")
        } catch (e: SnippetNotFound) {
            // Expected
        }
        verify(snippetRepository).findById(snippetId)
    }

    @Test
    fun `checkIfExists should throw SnippetNotFound when snippet does not exist`() {
        // Given
        val snippetId = 999L
        whenever(snippetRepository.existsById(snippetId)).thenReturn(false)

        // When/Then
        try {
            snippetService.checkIfExists(snippetId, "test")
            org.junit.jupiter.api.Assertions.fail("Should have thrown SnippetNotFound")
        } catch (e: SnippetNotFound) {
            // Expected
        }
        verify(snippetRepository).existsById(snippetId)
    }

    @Test
    fun `checkIfExists should not throw when snippet exists`() {
        // Given
        val snippetId = 1L
        whenever(snippetRepository.existsById(snippetId)).thenReturn(true)

        // When/Then - should not throw
        snippetService.checkIfExists(snippetId, "test")
        verify(snippetRepository).existsById(snippetId)
    }

    @Test
    fun `delete should delete snippet and asset`() {
        // Given
        val snippetId = 1L
        val directory = "snippets"
        whenever(snippetRepository.existsById(snippetId)).thenReturn(true)

        // When
        snippetService.delete(directory, snippetId)

        // Then
        verify(snippetRepository).existsById(snippetId)
        verify(snippetRepository).deleteById(snippetId)
        verify(assetService).delete(directory, snippetId)
    }

    @Test
    fun `countSnippets should return count by name when name provided`() {
        // Given
        val snippetName = "Test"
        val expectedCount = 5L
        whenever(snippetRepository.countByNameContainingIgnoreCase(snippetName))
            .thenReturn(expectedCount)

        // When
        val result = snippetService.countSnippets(snippetName)

        // Then
        result shouldBeEqualTo expectedCount
        verify(snippetRepository).countByNameContainingIgnoreCase(snippetName)
    }

    @Test
    fun `countSnippets should return total count when name is null`() {
        // Given
        val expectedCount = 10L
        whenever(snippetRepository.count()).thenReturn(expectedCount)

        // When
        val result = snippetService.countSnippets(null)

        // Then
        result shouldBeEqualTo expectedCount
        verify(snippetRepository).count()
    }

    @Test
    fun `countSnippets should return total count when name is empty`() {
        // Given
        val expectedCount = 10L
        whenever(snippetRepository.count()).thenReturn(expectedCount)

        // When
        val result = snippetService.countSnippets("")

        // Then
        result shouldBeEqualTo expectedCount
        verify(snippetRepository).count()
    }

    @Test
    fun `getFilteredSnippets should return empty list when snippetsIds is empty`() {
        // Given
        val snippetsIds = emptyList<SnippetUserDto>()

        // When
        val result = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, null)

        // Then
        result.first.isEmpty() shouldBeEqualTo true
        result.second shouldBeEqualTo 0L
        verify(snippetRepository, never()).findAllById(any())
    }

    @Test
    fun `getFilteredSnippets should handle warnings with valid JSON`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        val warningsJson = """["warning1", "warning2"]"""

        whenever(snippetRepository.findAllById(any())).thenAnswer { invocation ->
            val ids = invocation.arguments[0] as Collection<Long>
            listOf(snippet).filter { it.id in ids }
        }
        whenever(assetService.exists("lint-warnings", snippet.id)).thenReturn(true)
        whenever(assetService.get("lint-warnings", snippet.id)).thenReturn(warningsJson)

        // When
        val result = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, null)

        // Then
        result.first.size shouldBeEqualTo 1
        result.first[0].lintWarnings.size shouldBeEqualTo 2
        result.first[0].lintWarnings[0] shouldBeEqualTo "warning1"
    }

    @Test
    fun `getFilteredSnippets should filter by roles`() {
        // Given
        val snippetsIds =
            listOf(
                SnippetUserDto(snippetId = 1L, role = "Owner"),
                SnippetUserDto(snippetId = 2L, role = "Guest"),
            )
        val roles = listOf("Owner")
        val snippet1 = snippet.copy(id = 1L)
        val snippet2 = snippet.copy(id = 2L)

        whenever(snippetRepository.findAllById(any())).thenAnswer { invocation ->
            val ids = invocation.arguments[0] as Collection<Long>
            listOf(snippet1, snippet2).filter { it.id in ids }
        }
        whenever(assetService.exists("lint-warnings", snippet1.id)).thenReturn(false)

        // When
        val result = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, roles, null, null)

        // Then
        result.first.size shouldBeEqualTo 1
        result.first[0].role shouldBeEqualTo "Owner"
        result.second shouldBeEqualTo 1L
    }

    @Test
    fun `update should update snippet and publish events`() {
        // Given
        val snippetId = 1L
        val content = "print('Updated')"
        val token = "test-token"
        val userId = 1L

        whenever(snippetRepository.existsById(snippetId)).thenReturn(true)
        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(authorizationServiceClient.validate(token))
            .thenReturn(ResponseEntity.ok(userId))
        whenever(testService.getTestsBySnippetId(snippet.id)).thenReturn(emptyList())

        // When
        val result = snippetService.update(snippetId, content, token)

        // Then
        result.content shouldBeEqualTo content
        verify(snippetRepository).existsById(snippetId)
        verify(assetService).put("snippets", snippetId, content)
        verify(runnerServiceProducer).publishSnippetEvent(any<SnippetMessage>())
    }

    @Test
    fun `format should publish format event`() {
        // Given
        val snippetId = 1L
        val content = "print('Hello')"
        val token = "test-token"
        val userId = 1L

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(assetService.get("snippets", snippetId)).thenReturn(content)
        whenever(assetService.exists("lint-warnings", snippetId)).thenReturn(false)
        whenever(authorizationServiceClient.validate(token))
            .thenReturn(ResponseEntity.ok(userId))

        // When
        snippetService.format(snippetId, content, token)

        // Then
        verify(authorizationServiceClient).validate(token)
        verify(runnerServiceProducer).publishSnippetEvent(any<SnippetMessage>())
    }

    @Test
    fun `update should publish test events when tests exist`() {
        // Given
        val snippetId = 1L
        val content = "print('Updated')"
        val token = "test-token"
        val userId = 1L
        val test1 =
            TestModel(id = 1L, name = "Test 1", input = listOf("input1"), output = listOf("output1"), snippet = snippet)
        val test2 =
            TestModel(id = 2L, name = "Test 2", input = listOf("input2"), output = listOf("output2"), snippet = snippet)

        whenever(snippetRepository.existsById(snippetId)).thenReturn(true)
        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(authorizationServiceClient.validate(token))
            .thenReturn(ResponseEntity.ok(userId))
        whenever(testService.getTestsBySnippetId(snippet.id)).thenReturn(listOf(test1, test2))

        // When
        val result = snippetService.update(snippetId, content, token)

        // Then
        result.content shouldBeEqualTo content
        verify(runnerServiceProducer).publishSnippetEvent(any<SnippetMessage>())
        verify(runnerServiceProducer, times(2)).publishTestEvent(any<snippets.config.TestMessage>())
    }

    @Test
    fun `getFilteredSnippets should filter by name`() {
        // Given
        val snippetsIds =
            listOf(
                SnippetUserDto(snippetId = 1L, role = "Owner"),
                SnippetUserDto(snippetId = 2L, role = "Owner"),
            )
        val snippetName = "Test"
        val snippet1 = snippet.copy(id = 1L, name = "Test Snippet")
        val snippet2 = snippet.copy(id = 2L, name = "Other Snippet")

        whenever(snippetRepository.findAllById(any())).thenAnswer { invocation ->
            val ids = invocation.arguments[0] as Collection<Long>
            listOf(snippet1, snippet2).filter { it.id in ids }
        }
        whenever(assetService.exists("lint-warnings", snippet1.id)).thenReturn(false)
        whenever(assetService.exists("lint-warnings", snippet2.id)).thenReturn(false)

        // When
        val result = snippetService.getFilteredSnippets(0, 10, snippetsIds, snippetName, null, null, null)

        // Then
        result.first.size shouldBeEqualTo 1
        result.first[0].snippet.name shouldBeEqualTo "Test Snippet"
        result.second shouldBeEqualTo 1L
    }

    @Test
    fun `getFilteredSnippets should filter by languages`() {
        // Given
        val snippetsIds =
            listOf(
                SnippetUserDto(snippetId = 1L, role = "Owner"),
                SnippetUserDto(snippetId = 2L, role = "Owner"),
            )
        val languages = listOf(1L)
        val language2 = Language(id = 2L, name = "Other", version = "1.0", extension = "other")
        val snippet1 = snippet.copy(id = 1L)
        val snippet2 = snippet.copy(id = 2L, language = language2)

        whenever(snippetRepository.findAllById(any())).thenAnswer { invocation ->
            val ids = invocation.arguments[0] as Collection<Long>
            listOf(snippet1, snippet2).filter { it.id in ids }
        }
        whenever(assetService.exists("lint-warnings", snippet1.id)).thenReturn(false)
        whenever(assetService.exists("lint-warnings", snippet2.id)).thenReturn(false)

        // When
        val result = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, languages, null)

        // Then
        result.first.size shouldBeEqualTo 1
        result.first[0].snippet.language.id shouldBeEqualTo snippet1.language.id
        result.second shouldBeEqualTo 1L
    }

    @Test
    fun `getFilteredSnippets should filter by compliance`() {
        // Given
        val snippetsIds =
            listOf(
                SnippetUserDto(snippetId = 1L, role = "Owner"),
                SnippetUserDto(snippetId = 2L, role = "Owner"),
            )
        val compliance = listOf(Compliance.SUCCESS)
        val snippet1 = snippet.copy(id = 1L, status = Compliance.SUCCESS)
        val snippet2 = snippet.copy(id = 2L, status = Compliance.PENDING)

        whenever(snippetRepository.findAllById(any())).thenAnswer { invocation ->
            val ids = invocation.arguments[0] as Collection<Long>
            listOf(snippet1, snippet2).filter { it.id in ids }
        }
        whenever(assetService.exists("lint-warnings", snippet1.id)).thenReturn(false)
        whenever(assetService.exists("lint-warnings", snippet2.id)).thenReturn(false)

        // When
        val result = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, compliance)

        // Then
        result.first.size shouldBeEqualTo 1
        result.first[0].snippet.status shouldBeEqualTo Compliance.SUCCESS
        result.second shouldBeEqualTo 1L
    }

    @Test
    fun `getFilteredSnippets should handle pagination`() {
        // Given
        val snippetsIds =
            listOf(
                SnippetUserDto(snippetId = 1L, role = "Owner"),
                SnippetUserDto(snippetId = 2L, role = "Owner"),
                SnippetUserDto(snippetId = 3L, role = "Owner"),
            )
        val snippets =
            listOf(
                snippet.copy(id = 1L),
                snippet.copy(id = 2L),
                snippet.copy(id = 3L),
            )

        whenever(snippetRepository.findAllById(any())).thenAnswer { invocation ->
            val ids = invocation.arguments[0] as Collection<Long>
            snippets.filter { it.id in ids }
        }
        whenever(assetService.exists("lint-warnings", snippet.id)).thenReturn(false)

        // When - page 0, size 2
        val result = snippetService.getFilteredSnippets(0, 2, snippetsIds, null, null, null, null)

        // Then
        result.first.size shouldBeEqualTo 2
        result.second shouldBeEqualTo 3L
        verify(snippetRepository).findAllById(any())
    }

    @Test
    fun `getFilteredSnippets should handle warnings errors gracefully`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))

        whenever(snippetRepository.findAllById(any())).thenReturn(listOf(snippet))
        whenever(assetService.exists("lint-warnings", snippet.id)).thenReturn(true)
        whenever(assetService.get("lint-warnings", snippet.id)).thenThrow(RuntimeException("Error"))

        // When
        val result = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, null)

        // Then
        result.first.size shouldBeEqualTo 1
        result.first[0].lintWarnings.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `getFilteredSnippets should handle invalid warnings JSON gracefully`() {
        // Given
        val snippetsIds = listOf(SnippetUserDto(snippetId = 1L, role = "Owner"))
        val invalidJson = "invalid json"

        whenever(snippetRepository.findAllById(any())).thenReturn(listOf(snippet))
        whenever(assetService.exists("lint-warnings", snippet.id)).thenReturn(true)
        whenever(assetService.get("lint-warnings", snippet.id)).thenReturn(invalidJson)

        // When
        val result = snippetService.getFilteredSnippets(0, 10, snippetsIds, null, null, null, null)

        // Then
        result.first.size shouldBeEqualTo 1
        result.first[0].lintWarnings.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `get should handle warnings errors gracefully`() {
        // Given
        val snippetId = 1L
        val content = "print('Hello')"

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(assetService.get("snippets", snippetId)).thenReturn(content)
        whenever(assetService.exists("lint-warnings", snippetId)).thenReturn(true)
        whenever(assetService.get("lint-warnings", snippetId)).thenThrow(RuntimeException("Error"))

        // When
        val result = snippetService.get(snippetId)

        // Then
        result.id shouldBeEqualTo snippetId
        result.content shouldBeEqualTo content
        result.errors.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `get should handle invalid warnings JSON gracefully`() {
        // Given
        val snippetId = 1L
        val content = "print('Hello')"
        val invalidJson = "invalid json"

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(assetService.get("snippets", snippetId)).thenReturn(content)
        whenever(assetService.exists("lint-warnings", snippetId)).thenReturn(true)
        whenever(assetService.get("lint-warnings", snippetId)).thenReturn(invalidJson)

        // When
        val result = snippetService.get(snippetId)

        // Then
        result.id shouldBeEqualTo snippetId
        result.content shouldBeEqualTo content
        result.errors.isEmpty() shouldBeEqualTo true
    }

    @Test
    fun `update should handle test service errors gracefully`() {
        // Given
        val snippetId = 1L
        val content = "print('Updated')"
        val token = "test-token"
        val userId = 1L

        whenever(snippetRepository.existsById(snippetId)).thenReturn(true)
        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.of(snippet))
        whenever(authorizationServiceClient.validate(token))
            .thenReturn(ResponseEntity.ok(userId))
        whenever(testService.getTestsBySnippetId(snippet.id)).thenThrow(RuntimeException("Error"))

        // When
        val result = snippetService.update(snippetId, content, token)

        // Then
        result.content shouldBeEqualTo content
        verify(runnerServiceProducer).publishSnippetEvent(any<SnippetMessage>())
    }

    @Test
    fun `updateStatus should throw exception when snippet not found`() {
        // Given
        val snippetId = 999L
        val newStatus = Compliance.SUCCESS

        whenever(snippetRepository.findById(snippetId)).thenReturn(Optional.empty())

        // When/Then
        try {
            snippetService.updateStatus(snippetId, newStatus)
            org.junit.jupiter.api.Assertions.fail("Should have thrown RuntimeException")
        } catch (e: RuntimeException) {
            // Expected
        }
        verify(snippetRepository).findById(snippetId)
    }

    @Test
    fun `format should return early when token validation fails`() {
        // Given
        val snippetId = 1L
        val content = "print('Hello')"
        val token = "invalid-token"

        whenever(authorizationServiceClient.validate(token))
            .thenReturn(ResponseEntity.ok(null))

        // When
        snippetService.format(snippetId, content, token)

        // Then
        verify(authorizationServiceClient).validate(token)
        verify(snippetRepository, never()).findById(any())
    }
}
