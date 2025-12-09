# Request-ID Tracing - Guía de Implementación

## ¿Qué es Request-ID Tracing?

Request-ID Tracing permite rastrear una petición de usuario a través de **todos los servicios** de la aplicación usando un ID único. Esto es esencial para:

- **Debugging**: Ver todos los logs relacionados con una misma petición
- **Monitoreo**: Rastrear cómo una acción impacta en toda la cadena de servicios
- **Análisis**: Entender el flujo completo de una transacción en New Relic

## Cómo Funciona

1. **Entrada de Request**: Cuando llega una petición HTTP, el `RequestIdFilter`:
   - Busca el header `X-Request-ID` en la request
   - Si no existe, genera un nuevo UUID
   - Lo guarda en el MDC (Mapped Diagnostic Context) para que aparezca en los logs
   - Lo agrega al response header para que el cliente pueda verlo

2. **Propagación entre Servicios**: El `RequestIdInterceptor`:
   - Intercepta todas las llamadas HTTP salientes (RestTemplate)
   - Obtiene el Request-ID del MDC
   - Lo agrega automáticamente como header `X-Request-ID` en la request saliente

3. **Logs**: El formato de logs incluye el Request-ID, permitiendo filtrar logs por request

## Implementación en snippet-service

Ya está implementado con:

- ✅ `RequestIdFilter`: Captura/genera el Request-ID
- ✅ `RequestIdInterceptor`: Propaga el Request-ID en llamadas salientes
- ✅ `AppConfig`: Configura RestTemplate con el interceptor
- ✅ `logback-spring.xml`: Formato de logs con Request-ID

## Implementación en Otros Servicios

Para implementar en **runner-service** y **authorization-service**, necesitas:

### 1. Crear RequestIdFilter (igual en todos los servicios)

```kotlin
@Component
@Order(1)
class RequestIdFilter : Filter {
    companion object {
        const val REQUEST_ID_HEADER = "X-Request-ID"
        const val REQUEST_ID_MDC_KEY = "requestId"
    }

    override fun doFilter(
        request: ServletRequest,
        response: ServletResponse,
        chain: FilterChain,
    ) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        try {
            val requestId = httpRequest.getHeader(REQUEST_ID_HEADER)
                ?: UUID.randomUUID().toString()
            
            MDC.put(REQUEST_ID_MDC_KEY, requestId)
            httpResponse.setHeader(REQUEST_ID_HEADER, requestId)
            chain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
}
```

### 2. Crear RequestIdInterceptor (si hacen llamadas HTTP salientes)

```kotlin
class RequestIdInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val requestId = MDC.get("requestId")
        if (requestId != null) {
            request.headers.set("X-Request-ID", requestId)
        }
        return execution.execute(request, body)
    }
}
```

### 3. Configurar RestTemplate con el Interceptor

```kotlin
@Bean
fun restTemplate(): RestTemplate {
    val restTemplate = RestTemplate()
    restTemplate.interceptors.add(RequestIdInterceptor())
    return restTemplate
}
```

### 4. Configurar Logs (logback-spring.xml)

```xml
<property name="CONSOLE_LOG_PATTERN" 
          value="%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(%5p) %clr([%X{requestId}]){cyan} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n%wEx"/>
```

## Cómo Ver Request-ID en New Relic

### En Logs

1. Ve a **Logs** en New Relic
2. Busca por el campo `requestId` o filtra por `requestId: "tu-uuid-aqui"`
3. Verás todos los logs relacionados con esa petición

### En Traces

1. Ve a **Traces** en New Relic
2. El Request-ID aparece como atributo en los traces
3. Puedes filtrar traces por Request-ID

### En Distributed Tracing

1. Ve a **APM & Services** → Tu servicio → **Distributed tracing**
2. Verás cómo el Request-ID se propaga entre servicios
3. Cada span muestra el mismo Request-ID

## Ejemplo de Uso

### Desde el Cliente (Frontend/Postman)

```bash
# Hacer una petición con Request-ID personalizado
curl -H "X-Request-ID: mi-request-123" \
     http://localhost:8001/api/snippets/1

# O dejar que el servicio genere uno automáticamente
curl http://localhost:8001/api/snippets/1
```

### Ver el Request-ID en la Response

El servicio automáticamente agrega el Request-ID al header de respuesta:

```
X-Request-ID: 550e8400-e29b-41d4-a716-446655440000
```

### Buscar en Logs

En los logs verás algo como:

```
2025-12-08 16:00:00.123 INFO  [550e8400-e29b-41d4-a716-446655440000] snippets.api.SnippetController : Getting snippet with id 1
2025-12-08 16:00:00.145 INFO  [550e8400-e29b-41d4-a716-446655440000] snippets.service.AuthorizationServiceClient : Validating token
2025-12-08 16:00:00.200 INFO  [550e8400-e29b-41d4-a716-446655440000] snippets.service.SnippetService : Snippet found
```

Todos estos logs tienen el mismo Request-ID, permitiendo rastrear toda la petición.

## Verificación

Para verificar que funciona:

1. **Haz una petición al servicio**
2. **Revisa el header de respuesta**: Debería tener `X-Request-ID`
3. **Revisa los logs**: Deberían incluir el Request-ID
4. **Haz una petición que llame a otro servicio**: El Request-ID debería propagarse
5. **Revisa New Relic Logs**: Busca por el Request-ID y verifica que aparezcan logs de múltiples servicios

## Notas Importantes

- El Request-ID se propaga automáticamente en todas las llamadas HTTP salientes
- Si un servicio no recibe el header, genera uno nuevo (rompe la cadena)
- Asegúrate de implementar el Filter en **todos los servicios** para mantener la trazabilidad
- New Relic captura automáticamente el Request-ID si está en los logs o en los headers

