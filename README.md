# HipoSim Android

HipoSim es un simulador hipotecario independiente para compradores de primera vivienda en Perú (proyecto
universitario). Este repositorio es la aplicación móvil nativa para Android, hecha con Kotlin, Jetpack Compose y
Material 3. La elección de tecnología está documentada en [docs/adr/0001-mobile-technology.md](docs/adr/0001-mobile-technology.md).

## Estado

Tres pantallas. Las dos últimas se basan en los mockups de `docs/design/mockups/`:

1. **Simulación (US01)**: pantalla de inicio. Precio de la vivienda, cuota inicial (mínimo 10%) y plazo de 5 a 30
   años. Funciona sin cuenta y calcula con `POST /api/simulations`.
2. **Resultados del Crédito**: subsidio (Bono del Buen Pagador, solo si se aplicó), cuota, TCEA, VAN, TIR, plazo,
   seguro, cronograma inicial (primer año), enviar cotización, PDF (aviso "Disponible próximamente") y barra
   inferior con destinos bloqueados. Muestra los valores que devuelve la API.
3. **Registro Contextual**: formulario con consentimiento obligatorio (Ley N° 29733).

Las simulaciones salen de la API de HipoSim (`RemoteSimulationRepository`). `FakeSimulationRepository` queda solo para
las pruebas. La tasa se envía fija: TEA de referencia de 8.85%, la misma del Landing Page
(`SimulationRequest.REFERENCE_ANNUAL_RATE`). La opción del bono (US05) llega después: la petición ya incluye
`applyGoodPayerBonus`, hoy siempre en `false`.

### Cuentas y sesión (provisional)

El registro crea una cuenta en memoria con `FakeAuthRepository` detrás de la interfaz `AuthRepository`. Al registrarte
quedas con la sesión iniciada: los candados de la barra inferior desaparecen y "Perfil" abre una hoja con tu cuenta y
"Cerrar sesión". Es solo para la demo:

- Las cuentas y la sesión no se guardan: se pierden al cerrar la app.
- No hay inicio de sesión todavía (no está en los mockups) ni se guarda la contraseña.
- "Enviar cotización" e "Inmobiliarias" muestran "Disponible próximamente" con sesión iniciada, hasta que exista `feature/leads`.

Cuando existan registro e inicio de sesión en la API, se reemplaza `FakeAuthRepository` por una implementación con la
API en `di/RepositoryModule.kt`. No hay PDF todavía.

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

### API

La app llama a la API de HipoSim (repositorio `hiposim-backEnd`). La dirección sale de `BuildConfig.API_BASE_URL`:

| Compilación | Dirección por defecto | Cambiarla con |
|---|---|---|
| debug | `http://10.0.2.2:5000/` (el equipo anfitrión visto desde el emulador) | `-Phiposim.apiBaseUrl.debug=http://<ip-lan>:5000/` |
| release | `https://hiposim-api.invalid/` (reservada, aún no hay API desplegada) | `-Phiposim.apiBaseUrl.release=https://<host>/` |

La dirección siempre termina en `/`. Las compilaciones debug permiten HTTP sin cifrar
(`app/src/debug/res/xml/network_security_config.xml`); release no. Para probar en un teléfono físico usa la IP de tu
red local y que la API escuche en ella. La API en desarrollo corre en `http://localhost:5000` y necesita PostgreSQL;
las instrucciones están en el repositorio del backend. Sin la API, el formulario muestra "No pudimos conectar con
HipoSim".

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
  core/network/        JSON compartido y manejo de respuestas de la API (errores de red, problem+json)
  core/util/           formato de moneda y porcentaje (S/, es-PE), idioma de la demo
  feature/simulation/  ui (Formulario, Resultados), domain (modelos, validación, casos de uso),
                       data (API con Retrofit, RemoteSimulationRepository, FakeSimulationRepository para pruebas)
  feature/auth/        ui (Registro, LoginRequiredSheet, AccountSheet), domain (validación, sesión), data (FakeAuthRepository)
  feature/leads/       reservado (US21)
  feature/history/     reservado
  feature/comparison/  reservado
  di/                  módulos de Hilt
  MainActivity.kt, HipoSimApp.kt
```

Pruebas en `app/src/test` (JVM) y `app/src/androidTest` (instrumentadas). Las pruebas JVM del cliente de red usan un
servidor local (MockWebServer) con los ejemplos del contrato copiados en `app/src/test/resources/contract/`. Las
instrumentadas reemplazan los repositorios por datos fijos (`TestRepositoryModule`), así que no necesitan la API.

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
| US01 | Simular crédito con la API, sin cuenta | `feature/simulation`, `core/network` | Implementado, pendiente de revisión |
| US02, US04, US05, US06, US07, US08 | Por completar con el detalle de cada historia en Jira | Por asignar | Pendiente |
