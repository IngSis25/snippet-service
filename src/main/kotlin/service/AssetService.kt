package com.tuempresa.snippetservice.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class AssetService(
    @Value("\${asset.service.url}") private val assetServiceUrl: String
) {
    private val restTemplate = RestTemplate()

    fun uploadAsset(content: String): String {
        val request = mapOf("content" to content)
        val response: ResponseEntity<Map<*, *>> =
            restTemplate.postForEntity("$assetServiceUrl/assets", request, Map::class.java)
        return response.body?.get("id") as String
    }

    fun getAsset(id: String): String {
        val response: ResponseEntity<Map<*, *>> =
            restTemplate.getForEntity("$assetServiceUrl/assets/$id", Map::class.java)
        return response.body?.get("content") as String
    }

    fun deleteAsset(id: String) {
        restTemplate.delete("$assetServiceUrl/assets/$id")
    }
}
