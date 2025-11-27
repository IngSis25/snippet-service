package service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import snippets.service.AssetService

@ExtendWith(MockitoExtension::class)
class AssetServiceTest {
    @Mock
    private lateinit var restTemplate: RestTemplate

    private val assetServiceUrl = "http://asset-service:8080/v1/asset"
    private lateinit var assetService: AssetService

    @BeforeEach
    fun setUp() {
        assetService = AssetService(restTemplate, assetServiceUrl)
    }

    @Test
    fun `get should return content when asset exists`() {
        // Given
        val directory = "snippets"
        val id = 1L
        val content = "print('Hello')"

        whenever(restTemplate.getForObject(any<String>(), any<Class<*>>()))
            .thenReturn(content)

        // When
        val result = assetService.get(directory, id)

        // Then
        assert(result == content)
        verify(restTemplate).getForObject("$assetServiceUrl/$directory/$id", String::class.java)
    }

    @Test
    fun `get should throw exception when asset not found`() {
        // Given
        val directory = "snippets"
        val id = 1L

        whenever(restTemplate.getForObject(any<String>(), any<Class<*>>()))
            .thenReturn(null)

        // When/Then
        try {
            assetService.get(directory, id)
            org.junit.jupiter.api.Assertions.fail("Should have thrown Exception")
        } catch (e: Exception) {
            assert(e.message == "Asset not found")
        }
    }

    @Test
    fun `put should update asset`() {
        // Given
        val directory = "snippets"
        val id = 1L
        val content = "print('Updated')"

        // When
        val result = assetService.put(directory, id, content)

        // Then
        assert(result == "Asset updated")
        verify(restTemplate).put("$assetServiceUrl/$directory/$id", content, String::class.java)
    }

    @Test
    fun `delete should delete asset`() {
        // Given
        val directory = "snippets"
        val id = 1L

        // When
        assetService.delete(directory, id)

        // Then
        verify(restTemplate).delete("$assetServiceUrl/$directory/$id")
    }

    @Test
    fun `exists should return true when asset exists`() {
        // Given
        val directory = "snippets"
        val id = 1L
        val content = "print('Hello')"

        whenever(restTemplate.getForObject(any<String>(), any<Class<*>>()))
            .thenReturn(content)

        // When
        val result = assetService.exists(directory, id)

        // Then
        assert(result == true)
        verify(restTemplate).getForObject("$assetServiceUrl/$directory/$id", String::class.java)
    }

    @Test
    fun `exists should return false when asset does not exist`() {
        // Given
        val directory = "snippets"
        val id = 1L

        whenever(restTemplate.getForObject(any<String>(), any<Class<*>>()))
            .thenThrow(RestClientException("Not found"))

        // When
        val result = assetService.exists(directory, id)

        // Then
        assert(result == false)
        verify(restTemplate).getForObject("$assetServiceUrl/$directory/$id", String::class.java)
    }

    @Test
    fun `exists should return false on any exception`() {
        // Given
        val directory = "snippets"
        val id = 1L

        whenever(restTemplate.getForObject(any<String>(), any<Class<*>>()))
            .thenThrow(RuntimeException("Any error"))

        // When
        val result = assetService.exists(directory, id)

        // Then
        assert(result == false)
    }
}
