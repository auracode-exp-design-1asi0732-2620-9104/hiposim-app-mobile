# ADR 0001: Tecnología de la aplicación móvil (spike SP03)

- Estado: aceptada
- Historia Jira: SP03

## Contexto

HipoSim es un simulador hipotecario independiente para compradores de primera vivienda en Perú. La versión móvil
debe ser una aplicación nativa (lo exige el enunciado del curso), consumir la API REST del backend, seguir Material
Design y poder construirse en el tiempo disponible del ciclo por un equipo que aprende mientras desarrolla.

## Opciones evaluadas

| Criterio | Kotlin + Jetpack Compose | Flutter | React Native |
|---|---|---|---|
| Curva de aprendizaje | Media. Kotlin y Compose son declarativos y hay abundante material oficial. | Media. Exige aprender Dart y su ecosistema. | Baja si se conoce JavaScript o TypeScript, pero se suma la capa nativa de puentes y módulos. |
| Tiempo disponible | Suficiente: sin capa intermedia, el emulador y las herramientas de Android Studio bastan. | Suficiente, con un lenguaje y un motor de render adicionales. | Suficiente, pero los problemas de dependencias nativas consumen tiempo. |
| Ser nativo | Sí, es el stack nativo de Android. | No, dibuja con su propio motor. | No, usa un puente hacia vistas nativas. |
| Consumo de la API REST | Retrofit/OkHttp con Kotlin Serialization, patrón estándar. | Paquete `http` o `dio`. | `fetch` o `axios`. |
| Encaje con Material | Material 3 oficial (`androidx.compose.material3`). | Material 3 implementado por el framework. | Bibliotecas de terceros. |
| Pruebas | JUnit, Compose UI Test, Hilt. | `flutter_test`. | Jest, Detox. |
| Alcance de plataformas | Solo Android (iOS está fuera de alcance). | Android e iOS. | Android e iOS. |

## Decisión

Se usa **Kotlin + Jetpack Compose + Material 3**.

Motivos:

1. El enunciado exige una aplicación nativa. Flutter y React Native no lo cumplen aunque generen un binario Android.
2. Material 3 es un ciudadano de primera clase en Compose, y los mockups ya usan sus tokens.
3. Hilt, ViewModel, StateFlow y Navigation Compose dan una arquitectura de flujo de datos unidireccional sin
   depender de bibliotecas de terceros.
4. Kotlin Serialization y Retrofit/OkHttp cubren el consumo de la API REST cuando el backend exista.
5. Tener solo Android en alcance elimina la ventaja principal de las opciones multiplataforma.

## Consecuencias

- No hay código compartido con iOS. Si iOS entrara en alcance habría que reevaluar (por ejemplo Kotlin Multiplatform).
- El equipo debe aprender Compose y Kotlin. Se mitiga con una base pequeña y convenciones documentadas en el README.
- Las versiones de Gradle, AGP, Kotlin y Compose se fijan en `gradle/libs.versions.toml` y se actualizan de forma
  conjunta y comprobada.
