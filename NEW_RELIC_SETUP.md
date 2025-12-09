# Configuración de New Relic - Alertas y Dashboard

## Endpoint de Prueba para Errores HTTP 500

Se ha agregado un endpoint de prueba en el `SnippetController` que devuelve un error HTTP 500:

**Endpoint:** `GET /api/snippets/test-error`

Este endpoint está diseñado específicamente para testing y monitoreo. Lanza una excepción que resulta en un error HTTP 500.

### Cómo probar el endpoint:

```bash
# Desde tu terminal o Postman
curl http://localhost:8001/api/snippets/test-error
```

O desde el navegador:
```
http://localhost:8001/api/snippets/test-error
```

---

## Configurar Alerta para Errores HTTP 500 en New Relic

### Paso 1: Acceder a Alertas en New Relic

1. Inicia sesión en [New Relic](https://one.newrelic.com)
2. En el menú lateral, ve a **Alerts & AI** → **Alert conditions (NRQL)**

### Paso 2: Crear una Nueva Condición de Alerta

1. Haz clic en **New alert condition**
2. Selecciona **NRQL** como tipo de condición

### Paso 3: Configurar la Query NRQL

En el campo **NRQL query**, ingresa:

```sql
SELECT count(*) FROM TransactionError 
WHERE http.statusCode = 500 
FACET appName
```

O si quieres ser más específico y solo monitorear el snippet-service:

```sql
SELECT count(*) FROM TransactionError 
WHERE http.statusCode = 500 
AND appName = 'Snippet Searcher'
```

### Paso 4: Configurar el Threshold (Umbral)

- **Threshold type:** `Static`
- **Critical threshold:** `> 0` (se dispara si hay al menos 1 error 500)
- **Warning threshold:** (opcional) puedes dejarlo vacío

### Paso 5: Configurar la Ventana de Tiempo

- **Evaluation window:** `5 minutes` (recomendado)
- Esto significa que la alerta se evaluará cada 5 minutos

### Paso 6: Configurar Notificaciones

1. En **Notification channels**, selecciona o crea un canal de notificación
2. Puedes configurar:
   - Email
   - Slack
   - PagerDuty
   - Webhook
   - etc.

### Paso 7: Guardar la Alerta

1. Dale un nombre descriptivo: `HTTP 500 Errors - Snippet Service`
2. Haz clic en **Save**

---

## Disparar la Alerta

Una vez configurada la alerta, puedes dispararla haciendo una petición al endpoint de prueba:

```bash
curl http://localhost:8001/api/snippets/test-error
```

O desde el navegador:
```
http://localhost:8001/api/snippets/test-error
```

**Nota:** Puede tomar unos minutos (según la ventana de evaluación que configuraste) para que New Relic detecte el error y dispare la alerta.

---

## Crear Dashboard de Control

### Paso 1: Crear un Nuevo Dashboard

1. En New Relic, ve a **Dashboards** → **Dashboards**
2. Haz clic en **Create a dashboard**
3. Dale un nombre: `Snippet Service - Control Dashboard`

### Paso 2: Agregar Widgets con Métricas Importantes

Agrega los siguientes widgets (cada uno es una consulta NRQL):

#### 1. Tasa de Errores HTTP (Error Rate)
```sql
SELECT percentage(count(*), WHERE http.statusCode >= 400) 
FROM Transaction 
FACET appName 
TIMESERIES
```

#### 2. Errores HTTP 500
```sql
SELECT count(*) FROM TransactionError 
WHERE http.statusCode = 500 
FACET appName 
TIMESERIES
```

#### 3. Tiempo de Respuesta Promedio (Response Time)
```sql
SELECT average(duration) 
FROM Transaction 
FACET appName 
TIMESERIES
```

#### 4. Throughput (Requests por minuto)
```sql
SELECT rate(count(*), 1 minute) 
FROM Transaction 
FACET appName 
TIMESERIES
```

#### 5. Errores por Tipo
```sql
SELECT count(*) 
FROM TransactionError 
FACET error.class 
TIMESERIES
```

#### 6. Top 10 Endpoints más Lentos
```sql
SELECT average(duration) 
FROM Transaction 
FACET name 
ORDER BY average(duration) DESC 
LIMIT 10
```

#### 7. Errores por Endpoint
```sql
SELECT count(*) 
FROM TransactionError 
FACET transactionName 
ORDER BY count(*) DESC 
LIMIT 10
```

#### 8. Disponibilidad del Servicio (Uptime)
```sql
SELECT percentage(count(*), WHERE http.statusCode < 500) 
FROM Transaction 
FACET appName 
TIMESERIES
```

### Paso 3: Configurar cada Widget

Para cada query:
1. Haz clic en **Add chart**
2. Selecciona el tipo de visualización (Line chart, Bar chart, etc.)
3. Pega la query NRQL correspondiente
4. Configura el título del widget
5. Guarda el widget

### Paso 4: Organizar el Dashboard

- Arrastra y organiza los widgets de manera lógica
- Agrupa métricas relacionadas
- Ajusta el tamaño de los widgets según su importancia

---

## Métricas Recomendadas para Monitoreo

### Métricas Críticas (Deben estar siempre visibles):

1. **Error Rate** - Porcentaje de requests con errores
2. **HTTP 500 Errors** - Cantidad de errores del servidor
3. **Response Time** - Tiempo de respuesta promedio
4. **Throughput** - Cantidad de requests por minuto

### Métricas de Performance:

5. **Apdex Score** - Score de satisfacción del usuario
6. **Database Query Time** - Si usas base de datos
7. **External Service Calls** - Llamadas a servicios externos

### Métricas de Infraestructura:

8. **CPU Usage** - Uso de CPU
9. **Memory Usage** - Uso de memoria
10. **Thread Count** - Cantidad de threads activos

---

## Verificar que Todo Funciona

1. **Verifica que New Relic esté recibiendo datos:**
   - Ve a **APM & Services** → Selecciona tu aplicación
   - Deberías ver métricas en tiempo real

2. **Prueba el endpoint de error:**
   ```bash
   curl http://localhost:8001/api/snippets/test-error
   ```

3. **Verifica en New Relic:**
   - Ve a **APM & Services** → Tu aplicación → **Errors**
   - Deberías ver el error aparecer en la lista

4. **Espera a que se dispare la alerta:**
   - Puede tomar unos minutos según la configuración de la ventana de evaluación
   - Revisa **Alerts & AI** → **Alert history** para ver si se disparó

---

## Notas Importantes

- El endpoint `/api/snippets/test-error` está diseñado solo para testing. En producción, considera protegerlo o eliminarlo.
- Las alertas pueden tardar unos minutos en dispararse según la ventana de evaluación configurada.
- Asegúrate de que el agente de New Relic esté correctamente configurado y enviando datos.
- Revisa periódicamente el dashboard para identificar tendencias y problemas potenciales.

