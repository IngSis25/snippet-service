package snippets.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class AssetService(
    private val restTemplate: RestTemplate,
    @Value("\${spring.asset.service.url}") private val assetServiceUrl: String,
) : AssetServiceRoutes {
    override fun get(
        directory: String,
        id: Long,
    ): String {
        val response =
            restTemplate.getForObject(
                "$assetServiceUrl/$directory/$id",
                String::class.java,
            )
        return response ?: throw Exception("Asset not found")
    }

    override fun put(
        directory: String,
        id: Long,
        content: String,
    ): String {
        restTemplate.put(
            "$assetServiceUrl/$directory/$id",
            content,
            String::class.java,
        )
        return "Asset updated"
    }

    override fun delete(
        directory: String,
        id: Long,
    ) {
        restTemplate.delete("$assetServiceUrl/$directory/$id")
    }

    override fun exists(
        directory: String,
        id: Long,
    ): Boolean {
        return try {
            restTemplate.getForObject(
                "$assetServiceUrl/$directory/$id",
                String::class.java,
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}
