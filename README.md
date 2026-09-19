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
financiero. No hay API, autenticación real ni PDF todavía.

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
  core/util/           formato de moneda y porcentaje (S/, es-PE), idioma de la demo
  feature/simulation/  ui (Resultados), domain (modelos, casos de uso), data (FakeSimulationRepository)
  feature/auth/        ui (Registro, LoginRequiredSheet), domain (validación), data (pendiente)
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
