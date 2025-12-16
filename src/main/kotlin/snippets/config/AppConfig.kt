package snippets.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.web.client.RestTemplate

@Configuration
class AppConfig {
    @Bean
    fun restTemplate(): RestTemplate {
        val restTemplate = RestTemplate()
        // Agregar interceptor para propagar Request-ID en todas las llamadas HTTP salientes
        restTemplate.interceptors.add(RequestIdInterceptor())
        return restTemplate
    }

    @Bean
    fun stringRedisTemplate(connectionFactory: RedisConnectionFactory): StringRedisTemplate {
        return StringRedisTemplate(connectionFactory)
    }
}
