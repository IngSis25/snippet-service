# snippet-service

## Configuración de Variables de Entorno

### 🔧 Desarrollo Local

Configura las variables de entorno en tu sistema antes de ejecutar la aplicación:

**Windows PowerShell:**
```powershell
$env:AUTH_SERVER_URI="https://tu-dominio.auth0.com/"
$env:AUTH0_AUDIENCE="tu-audience-aqui"
./gradlew bootRun
```

**Linux/Mac:**
```bash
export AUTH_SERVER_URI=https://tu-dominio.auth0.com/
export AUTH0_AUDIENCE=tu-audience-aqui
./gradlew bootRun
```

### 🚀 Producción

En producción (Docker, Kubernetes, servidores, etc.), configura las variables directamente en tu plataforma:

**Docker:**
```bash
docker run -e AUTH_SERVER_URI=https://... -e AUTH0_AUDIENCE=... snippet-service
```

**Docker Compose:**
```yaml
environment:
  - AUTH_SERVER_URI=https://...
  - AUTH0_AUDIENCE=...
```

**Kubernetes:**
```yaml
env:
  - name: AUTH_SERVER_URI
    value: "https://..."
  - name: AUTH0_AUDIENCE
    value: "..."
```

### 📋 Variables Requeridas

- `AUTH_SERVER_URI`: URI del issuer de Auth0 (ej: `https://tu-dominio.auth0.com/`)
- `AUTH0_AUDIENCE`: Audience configurado en Auth0 (API Identifier)

### 📋 Variables Opcionales

- `REDIS_HOST`: Host de Redis (default: `localhost`)
- `REDIS_PORT`: Puerto de Redis (default: `6379`)