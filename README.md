# Supermercado a domicilio: servidor MCP + ChatBot

Trabajo final del curso **Arquitectura de Software** (Universidad de Antioquia, 2026).
Docente: Diego José Luis Botia.

Un **servidor MCP** (Model Context Protocol) en Spring Boot expone herramientas para atender pedidos de supermercado a domicilio sobre **MySQL**. Un **ChatBot** construido con **Google ADK** y un modelo **Gemini** las usa para conversar con el cliente, consultar productos reales y armar su pedido.

```
Navegador ──► Agente ADK (8090) ──► Gemini API
                    │
                    └─ MCP (Streamable HTTP) ─► Servidor MCP (8089, /mcp) ──► MySQL
Claude Desktop ──STDIO──► mcp-remote ──HTTP──► Servidor MCP
```

## Estado del proyecto

| Componente | Estado |
|---|---|
| Servidor MCP con 8 herramientas | Completo |
| Base de datos MySQL (5 tablas, restricciones `CHECK`) | Completo |
| ChatBot ADK + Gemini (interfaz web) | Completo: arma el pedido hasta estado **BORRADOR** |
| Claude Desktop con `mcp-remote` | Validado con el servidor del laboratorio; para este servidor solo cambia el puerto (8089) |
| `confirmarPedido`, `consultarPedido`, `listarPedidosCliente`, `cancelarPedido` | Pendiente |

## Tecnologías

- Java 21 (Eclipse Temurin), Maven Wrapper
- Spring Boot 3.4.7, Spring AI 1.1.4 (`spring-ai-starter-mcp-server-webmvc`)
- JPA / Hibernate 6.6, Bean Validation
- MySQL 8.0
- Google ADK para Java (`google-adk` 0.6.0) y Gemini API (modelo `gemini-3.5-flash-lite`)
- Node.js (`npx mcp-remote`) para Claude Desktop

## Requisitos

- JDK 21 (con otra versión por defecto, actívalo por ventana; ver abajo)
- MySQL 8.0 en el puerto 3306
- Una clave de la Gemini API creada en [Google AI Studio](https://aistudio.google.com)
- (Opcional) Node.js y Claude Desktop

## Estructura

```
.
├── supermercado-mcp/        Servidor MCP (Spring Boot)
│   ├── sql/                 01_schema.sql y 02_datos.sql
│   ├── scripts/             mcp-cliente.ps1 (cliente de pruebas en PowerShell)
│   └── src/main/java/com/udea/supermercado/
│       ├── config/          Publica las herramientas (ToolCallbackProvider)
│       ├── model/           Entidades JPA y enums
│       ├── repository/      Repositorios Spring Data
│       ├── dto/             Objetos que devuelven las herramientas
│       ├── exception/       ReglaNegocioException
│       ├── util/            Normalización de teléfonos
│       └── service/         Herramientas @Tool (catálogo, cliente, pedido)
├── agente-supermercado/     Agente Google ADK (SupermercadoAgent.java)
├── informe/                 Informe del trabajo final (.docx)
└── evidencias/              Capturas y salidas de las pruebas
```

## Puesta en marcha

### 1. Activar Java 21 (PowerShell)

Las variables solo viven en esa ventana; repite esto en cada ventana nueva.

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
java -version
```

### 2. Crear la base de datos

Ejecuta los dos scripts en MySQL, en este orden:

```powershell
mysql -u root -p < supermercado-mcp\sql\01_schema.sql
mysql -u root -p < supermercado-mcp\sql\02_datos.sql
```

Se crean la base `supermercado` (utf8mb4), 5 tablas, 5 categorías, 22 productos y 2 clientes de ejemplo.

### 3. Arrancar el servidor MCP (puerto 8089)

La contraseña de MySQL se pasa por la variable de entorno `PWD_UDEA`, sin escribirla en archivos:

```powershell
cd supermercado-mcp
$sec = Read-Host "Clave MySQL" -AsSecureString
$env:PWD_UDEA = [System.Net.NetworkCredential]::new("", $sec).Password
.\mvnw.cmd spring-boot:run
```

Señales de que arrancó bien en el log:

```
Found 4 JPA repository interfaces
HikariPool-1 - Start completed
Registered tools: 8
Started SupermercadoApplication
```

La advertencia `No tool methods found in the provided tool objects: []` es de otro proveedor interno de Spring AI y no afecta; el indicador fiable es `Registered tools: 8`.

### 4. Arrancar el ChatBot (puerto 8090)

En **otra** ventana de PowerShell (con Java 21 activo):

```powershell
cd agente-supermercado
$sec = Read-Host "GOOGLE_API_KEY" -AsSecureString
$env:GOOGLE_API_KEY = [System.Net.NetworkCredential]::new("", $sec).Password
.\mvnw.cmd compile exec:java@runAdkWebServer
```

La consola debe mostrar `Tools MCP: 8`. Abre <http://localhost:8090>, elige `SupermercadoAgent` y conversa. Ejemplo:

> quiero hacer un pedido. Mi celular es 3001112233
> quiero leche y un café molido, para mañana en la tarde

La pestaña **Trace** muestra cada herramienta MCP que invoca el modelo.

### 5. Probar el servidor sin modelo (opcional)

```powershell
cd supermercado-mcp
. .\scripts\mcp-cliente.ps1
Start-Mcp
Get-Tools
Invoke-Tool "buscarProductos" @{ texto = "cafe" }
```

Probar el servidor de forma independiente del modelo permite separar errores del servidor de errores del chat.

### 6. Conectar Claude Desktop (opcional)

Claude Desktop solo lanza servidores locales por STDIO; `mcp-remote` hace de puente hacia HTTP. Agrega esta entrada dentro de `mcpServers` en `claude_desktop_config.json` (con Claude Desktop cerrado por completo) y valida que el JSON siga siendo correcto:

```json
"supermercado": {
  "command": "npx",
  "args": ["-y", "mcp-remote", "http://localhost:8089/mcp"]
}
```

## Herramientas MCP

| Herramienta | Parámetros | Tipo | Qué hace |
|---|---|---|---|
| `buscarProductos` | `texto` | Lectura | Busca productos activos por nombre o categoría, sin importar tildes ni mayúsculas. Sin resultados devuelve lista vacía |
| `consultarProducto` | `productoId` | Lectura | Detalle de un producto. Falla si no existe o ya no se vende |
| `buscarClientePorTelefono` | `telefono` | Lectura | Busca un cliente por celular (acepta espacios, guiones y +57) |
| `registrarCliente` | `nombre`, `telefono`, `direccion` | Escritura | Crea un cliente. Falla si el teléfono ya existe |
| `obtenerFechaActual` | — | Lectura | Fecha actual en `America/Bogota`, para resolver "mañana" o "el viernes" |
| `crearPedidoBorrador` | `clienteId`, `fechaEntrega`, `franja`, `direccionEntrega` (opcional) | Escritura | Crea el pedido en estado BORRADOR |
| `agregarProducto` | `pedidoId`, `productoId`, `cantidad` | Escritura | Agrega o suma un producto verificando que esté activo y haya stock |
| `quitarProducto` | `pedidoId`, `productoId`, `cantidad` (opcional) | Escritura | Quita un producto o reduce su cantidad |

## Reglas de negocio

Las valida el **servidor**, no el prompt del agente:

- Fecha de entrega desde mañana hasta 14 días (zona `America/Bogota`).
- Franja: `MANANA`, `TARDE` o `NOCHE`.
- Cantidad entera de 1 a 50 por producto.
- Solo productos activos y con stock suficiente.
- Un único borrador por cliente.
- Solo se modifican pedidos en estado BORRADOR.
- El total lo calcula el servidor; el agente solo lo muestra.
- El borrador **no descuenta stock**; el descuento real ocurriría al confirmar (pendiente).

La base de datos añade una segunda defensa con restricciones `CHECK` (precio > 0, stock >= 0, estados y franjas válidos, cantidad > 0), `UNIQUE` y llaves foráneas. Las modificaciones de un pedido usan bloqueo `PESSIMISTIC_WRITE` y `@Version` para evitar cambios perdidos en concurrencia.

## Seguridad

- **No se guardan secretos en el repositorio.** La contraseña de MySQL entra por `PWD_UDEA` y la clave de Gemini por `GOOGLE_API_KEY`; `application.properties` solo contiene `${PWD_UDEA}`.
- Nunca pegues claves en el código, en comandos que vayas a copiar ni en capturas. Si una clave queda expuesta, revócala y crea otra.
- El proyecto no incluye autenticación: quien conoce un teléfono puede armar pedidos de ese cliente (limitación conocida).

## Problemas frecuentes

| Problema | Solución |
|---|---|
| El proyecto exige Java 21 y hay otra versión por defecto | Activar `JAVA_HOME` y `Path` en la ventana, como en el paso 1 |
| `BUILD FAILURE` con otro proyecto | Ejecutar `mvnw` siempre desde la carpeta del proyecto; comprobar con `Get-Location` |
| Gemini responde 404 con `gemini-2.5-flash` | Usar `gemini-3.5-flash-lite` (ya configurado) |
| El agente carga pero dice que el servidor no está disponible | Arrancar primero el servidor MCP (puerto 8089) y reiniciar el agente |
| Puerto 8080 ocupado | El agente ya usa el 8090; cambiarlo en el `pom.xml` si también está ocupado |
| Tildes rotas en la consola | PowerShell 5.1 decodifica UTF-8 como Latin-1; no afecta la base de datos |

## Limitaciones y trabajo futuro

- Implementar `confirmarPedido` (descuenta stock con bloqueo), `consultarPedido`, `listarPedidosCliente` y `cancelarPedido`.
- Probar Claude Desktop contra este servidor (puerto 8089).
- Autenticación de clientes y del cliente MCP.
- Parametrizar el límite de 50 unidades y la ventana de 14 días.
- Refactorizar la normalización de teléfonos, hoy duplicada en dos clases.

## Equipo

- Pablo Luis Rodríguez Burgos
- Isaac Cano Orozco

## Referencias

- [Especificación MCP 2025-06-18](https://modelcontextprotocol.io/specification/2025-06-18)
- [Spring AI: MCP Streamable-HTTP Server](https://docs.spring.io/spring-ai/reference/1.1/api/mcp/mcp-streamable-http-server-boot-starter-docs.html)
- [Google ADK para Java](https://github.com/google/adk-java)
- [Gemini API: claves de API](https://ai.google.dev/gemini-api/docs/api-key)
- [mcp-remote](https://github.com/geelen/mcp-remote)
- [MySQL 8.0: restricciones CHECK](https://dev.mysql.com/doc/refman/8.0/en/create-table-check-constraints.html)
- Repositorios base del laboratorio: [labmcp](https://github.com/diegobotia/labmcp) y [agentemcp](https://github.com/diegobotia/agentemcp)
