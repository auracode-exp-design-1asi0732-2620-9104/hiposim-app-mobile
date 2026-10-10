# HipoSim Android

HipoSim es un simulador hipotecario independiente para compradores de primera vivienda en Perú (proyecto
universitario). Este repositorio es la aplicación móvil nativa para Android, hecha con Kotlin, Jetpack Compose y
Material 3. La elección de tecnología está documentada en [docs/adr/0001-mobile-technology.md](docs/adr/0001-mobile-technology.md).

## Estado

Primer avance ejecutable con dos pantallas basadas en los mockups de `docs/design/mockups/`:

1. **Resultados del Crédito**: subsidio (Bono del Buen Pagador), cuota, TCEA, VAN, TIR, plazo, seguro, cronograma
   inicial, enviar cotización, PDF (aviso "Disponible próximamente") y barra inferior con destinos bloqueados.
2. **Registro Contextual**: formulario con consentimiento obligatorio (Ley N° 29733).

Los datos salen de `FakeSimulationRepository`. El VAN y la TIR son valores de ejemplo hasta que exista el motor
financiero.

### Cuentas y sesión (provisional)

El registro crea una cuenta en memoria con `FakeAuthRepository` detrás de la interfaz `AuthRepository`. Al registrarte
quedas con la sesión iniciada: los candados de la barra inferior desaparecen y "Perfil" abre una hoja con tu cuenta y
"Cerrar sesión". Es solo para la demo:

- Las cuentas y la sesión no se guardan: se pierden al cerrar la app.
- No hay inicio de sesión todavía (no está en los mockups) ni se guarda la contraseña.
- "Enviar cotización" e "Inmobiliarias" muestran "Disponible próximamente" con sesión iniciada, hasta que exista `feature/leads`.

Cuando exista el backend, se reemplaza `FakeAuthRepository` por una implementación con la API en `di/RepositoryModule.kt`.
No hay API real ni PDF todavía.

## Requisitos

- Android Studio reciente y Android SDK con plataforma 36 y build-tools 36.
- JDK 17 o superior. Se recomienda el JBR de Android Studio (JDK 25). Gradle 9.7 ejecuta con JDK 17 a 26.
- `local.properties` con la ruta del SDK (no se versiona):

```properties
sdk.dir=C\:\\Users\\<usuario>\\AppData\\Local\\Android\\Sdk
```

## Compilar y ejecutar

```powershell
$env:JAVA_HOME = "C:\Users\<usuario>\AppData\Local\Programs\Android Studio\jbr"
.\gradlew assembleDebug
```

Emulador `Pixel_10_Pro`:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Pixel_10_Pro
.\gradlew installDebug
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" shell am start -n com.auracode.hiposim/.MainActivity
```

Capturas para el informe:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" exec-out screencap -p > captura.png
```

## Pruebas y calidad

```powershell
.\gradlew ktlintCheck lintDebug testDebugUnitTest   # estilo, lint y pruebas unitarias
.\gradlew ktlintFormat                              # corrige el formato
.\gradlew connectedDebugAndroidTest                 # pruebas de UI, requiere emulador o dispositivo
```

El workflow [.github/workflows/ci.yml](.github/workflows/ci.yml) corre ktlint, lint, pruebas unitarias y
`assembleDebug` en cada push a `main` y `develop` y en cada pull request.

## Conexión con la API

El contrato es `docs/api-contract/openapi.v0.yaml` del repo `hiposim-backEnd` (v0.1.0). La capa de red vive en
`core/network` y se arma en `di/NetworkModule.kt` (Retrofit, OkHttp y kotlinx.serialization).

| Ajuste | `BuildConfig` | Valor por defecto | Propiedad de Gradle |
|---|---|---|---|
| URL base en debug | `API_BASE_URL` | `http://10.0.2.2:5000/` | `hiposim.apiBaseUrl.debug` |
| URL base en release | `API_BASE_URL` | `https://api.hiposim.invalid/` (marcador) | `hiposim.apiBaseUrl.release` |
| Implementación falsa o real | `USE_FAKE_API` | `true` | `hiposim.useFakeApi` |

Las propiedades se definen en `~/.gradle/gradle.properties` o en la línea de comandos, sin tocar el código. La URL
debe terminar en `/`. Para probar contra la API real en el emulador:

```powershell
.\gradlew installDebug "-Phiposim.useFakeApi=false" "-Phiposim.apiBaseUrl.debug=http://10.0.2.2:5000/"
```

- **Falsa o real**: mientras no exista el host de la API, `USE_FAKE_API` vale `true` y cada repositorio responde con
  una implementación en memoria que respeta la forma del contrato. La elección se hace en `di/RepositoryModule.kt`;
  la UI y el dominio no cambian.
- **HTTP sin cifrar**: permitido solo en debug y solo hacia `10.0.2.2` y `localhost`
  (`app/src/debug/res/xml/network_security_config.xml`). Release exige HTTPS.
- **Token JWT**: `AuthInterceptor` agrega `Authorization: Bearer <accessToken>` cuando `TokenStore.token` tiene valor.
  El registro y el inicio de sesión deben guardar ahí el `accessToken` y limpiarlo al cerrar sesión. Sin sesión la
  petición sale sin cabecera (la simulación es pública).
- **Registro de peticiones**: solo en debug (Logcat, etiqueta `okhttp.OkHttpClient`), con el token oculto.

### Errores

Los repositorios envuelven cada llamada con `safeApiCall { ... }` y devuelven `ApiResult<T>`, nunca lanzan. Un fallo
llega como `ApiResult.Failure(error)` con uno de estos `ApiError`:

| `ApiError` | Cuándo | Qué hace la UI |
|---|---|---|
| `NoConnection` | Sin red, tiempo agotado o servidor inaccesible | Mensaje de conexión y reintento |
| `Validation(fieldErrors, title)` | 400 con `application/problem+json` | Muestra cada mensaje junto a su campo |
| `Unauthorized(title)` | 401 | Credenciales inválidas o sesión vencida |
| `Conflict(title)` | 409 | Por ejemplo, correo ya registrado |
| `Client(code, title)` | Otro 4xx | Mensaje genérico |
| `Server(code)` | 5xx | Mensaje genérico y reintento |
| `Unexpected(cause)` | Respuesta que no coincide con el DTO u otro fallo | Mensaje genérico |

`fieldErrors` usa como clave la propiedad de la petición en camelCase (`downPayment`), igual que el DTO. Si el
servidor responde con PascalCase (`DownPayment`), se normaliza.

### Agregar un servicio nuevo

1. **DTO** en `feature/<nombre>/data`, con `@Serializable` y los mismos nombres del contrato. Las tasas son
   fracciones (`0.085` es 8.5%).
2. **Interfaz Retrofit** en la misma carpeta, con rutas relativas sin `/` inicial:

   ```kotlin
   interface AuthService {
       @POST("api/auth/login")
       suspend fun login(@Body request: LoginRequestDto): LoginResponseDto
   }
   ```

3. **Repositorio real** que implementa la interfaz del dominio, llama al servicio dentro de `safeApiCall` y convierte
   el DTO al modelo del dominio con `map`:

   ```kotlin
   override suspend fun login(email: String, password: String): ApiResult<AuthenticatedUser> =
       safeApiCall { service.login(LoginRequestDto(email, password)) }.map { it.toDomain() }
   ```

4. **Repositorio falso** con la misma interfaz y datos con la forma del contrato.
5. **Hilt**: provee el servicio con `retrofit.create(AuthService::class.java)` y elige la implementación en
   `di/RepositoryModule.kt` según `BuildConfig.USE_FAKE_API`.
6. **Pruebas**: el mapeo del DTO, y el repositorio real contra `MockWebServer` (ver `NetworkStackTest`).

## Idioma

`res/values` está en inglés (idioma por defecto del curso) y `res/values-b+es+419` en español. La demo arranca en
español porque `DEMO_LANGUAGE_TAG` en `core/util/DemoLocale.kt` vale `"es-419"`. Para volver al inglés por defecto
cambia esa constante a `"en"`, o a una cadena vacía para seguir el idioma del dispositivo.

## Arquitectura

Flujo de datos unidireccional: la UI (Compose) observa un `StateFlow<UiState>` del `ViewModel` y le envía eventos.
El `ViewModel` usa casos de uso del dominio, que dependen de interfaces de repositorio. Hilt inyecta las
implementaciones en `di/`, de modo que la UI nunca conoce la capa `data`.

```
app/src/main/java/com/auracode/hiposim/
  core/designsystem/   theme (Color, Type, Shape, Spacing) y componentes reutilizables
  core/navigation/     rutas tipadas y NavHost
  core/network/        ApiResult, ApiError, safeApiCall, AuthInterceptor, TokenStore
  core/util/           formato de moneda y porcentaje (S/, es-PE), idioma de la demo
  feature/simulation/  ui (Resultados), domain (modelos, casos de uso), data (FakeSimulationRepository)
  feature/auth/        ui (Registro, LoginRequiredSheet, AccountSheet), domain (validación, sesión), data (FakeAuthRepository)
  feature/leads/       reservado (US21)
  feature/history/     reservado
  feature/comparison/  reservado
  di/                  módulos de Hilt
  MainActivity.kt, HipoSimApp.kt
```

Pruebas en `app/src/test` (JVM) y `app/src/androidTest` (instrumentadas).

## Convenciones

- Identificadores de código en inglés. Textos visibles solo en recursos (`strings.xml`), nunca en el código.
- Espaciado en múltiplos de 8 dp, áreas táctiles de al menos 48 dp, `contentDescription` en íconos que informan.
- Colores y tipografía salen de `core/designsystem/theme`. Fuentes Outfit y Roboto Flex (licencia OFL, en
  `docs/third-party/`).
- Formato con ktlint (`.editorconfig`). Las versiones de dependencias viven en `gradle/libs.versions.toml`.
- Sin texto con guion largo en la documentación.

## Flujo de trabajo

GitFlow con ramas `main`, `develop` y `feature/<id-jira>-<descripcion>`, por ejemplo
`feature/us13-contextual-registration`. Commits con Conventional Commits e ID de Jira:

```
feat(auth): add contextual registration form (US13)
```

## Historias de Jira

| Historia | Alcance | Feature | Estado |
|---|---|---|---|
| SP03 | Spike de tecnología móvil | `docs/adr` | Hecho |
| US13 | Mockup de registro contextual | `feature/auth` | Mockup implementado |
| US21 | Mockup de resultados y conexión inmobiliaria | `feature/simulation`, `feature/leads` | Pantalla de resultados implementada, conexión pendiente |
| US01, US02, US04, US05, US06, US07, US08 | Por completar con el detalle de cada historia en Jira | Por asignar | Pendiente |
