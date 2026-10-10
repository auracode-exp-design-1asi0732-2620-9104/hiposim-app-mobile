# Distribución con Firebase App Distribution

Guía para generar el APK de release firmado y subirlo a Firebase App Distribution con la CLI. La subida la hace una
persona con acceso al proyecto de Firebase: en el repositorio no hay llaves, contraseñas ni archivos de Firebase.

## Qué APK generar

El nombre del archivo indica de dónde salen los datos:

| APK | Cuándo usarlo | Propiedades de Gradle |
|---|---|---|
| `hiposim-<versión>-demo-release.apk` | Mientras no exista una URL del backend desplegado. Responde desde memoria, funciona sin servidor y sin red. | Ninguna (`hiposim.useFakeApi` es `true` por defecto) |
| `hiposim-<versión>-api-release.apk` | Cuando el backend esté desplegado con HTTPS. | `-Phiposim.useFakeApi=false` y `-Phiposim.apiBaseUrl.release=https://<host>/` |

Cada compilación deja su APK en `app/build/outputs/apk/release/` y borra el de la compilación anterior: si necesitas
los dos, copia el primero a otra carpeta antes de generar el segundo. Tienen el mismo `applicationId` y la misma
versión, así que en un teléfono se instala uno u otro, no los dos a la vez.

El despliegue de demostración del backend está en Render: `https://hiposim-api.onrender.com/` (documentación en
`/swagger`). Es el plan gratuito: tras unos 15 minutos sin uso el servicio se suspende y la primera petición puede
tardar cerca de un minuto, así que la primera simulación puede mostrar un error de conexión y funcionar al reintentar.

En el APK `demo` el registro, el inicio de sesión y el envío a la inmobiliaria se simulan: las cuentas viven en
memoria y ninguna inmobiliaria recibe nada. Conviene decirlo en las notas de la versión.

Una release con `hiposim.useFakeApi=false` no compila si la URL es el valor de relleno, no usa HTTPS o no termina en
`/`. El tráfico sin cifrar solo está permitido en debug, hacia el emulador.

## Firma

La firma se configura con un archivo de propiedades que vive fuera del repositorio. Gradle lo busca en la propiedad
`hiposim.signing.propertiesFile` o en la variable de entorno `HIPOSIM_SIGNING_PROPERTIES`. Si no lo encuentra, la
release se compila sin firmar (`...-release-unsigned.apk`), que es lo que ocurre en CI.

### 1. Crear el keystore (una sola vez)

En una carpeta fuera de cualquier repositorio, por ejemplo `C:\Users\<usuario>\HipoSim\hiposim-signing`:

```powershell
& "$env:LOCALAPPDATA\Programs\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore hiposim-release.jks -storetype PKCS12 -alias hiposim -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` pide la contraseña y los datos del certificado. Guarda una copia del keystore y de su contraseña en un
lugar seguro: sin ellos no se puede publicar una actualización que se instale sobre la versión anterior.

### 2. Crear `keystore.properties` junto al keystore

```properties
storeFile=hiposim-release.jks
storePassword=<contraseña del keystore>
keyAlias=hiposim
keyPassword=<contraseña de la llave>
```

`storeFile` puede ser una ruta relativa a la carpeta de este archivo o una ruta absoluta.

### 3. Indicar a Gradle dónde está

Para la sesión actual de PowerShell:

```powershell
$env:HIPOSIM_SIGNING_PROPERTIES = "C:\Users\<usuario>\HipoSim\hiposim-signing\keystore.properties"
```

O de forma permanente en `~/.gradle/gradle.properties` (fuera del repositorio):

```properties
hiposim.signing.propertiesFile=C:/Users/<usuario>/HipoSim/hiposim-signing/keystore.properties
```

El `.gitignore` excluye `*.jks`, `*.keystore`, `*.p12`, `keystore.properties`, `google-services.json`, las llaves de
cuentas de servicio y los archivos `.env`, por si alguno termina dentro del repositorio por error.

## Generar los APK

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA\Programs\Android Studio\jbr"
```

APK de demostración, sin servidor:

```powershell
.\gradlew assembleRelease
```

APK conectado a la API desplegada:

```powershell
.\gradlew assembleRelease "-Phiposim.useFakeApi=false" "-Phiposim.apiBaseUrl.release=https://<host>/"
```

Con el despliegue de demostración:

```powershell
.\gradlew assembleRelease "-Phiposim.useFakeApi=false" "-Phiposim.apiBaseUrl.release=https://hiposim-api.onrender.com/"
```

Comprobar la firma:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\<versión>\apksigner.bat" verify --print-certs app\build\outputs\apk\release\hiposim-0.1.0-demo-release.apk
```

Cada subida nueva necesita un `versionCode` mayor: se pasa con `-Phiposim.versionCode=<n>` (por defecto 1). Cambia
`appVersionName` en `app/build.gradle.kts` si cambia la versión visible.

## Configurar Firebase (una sola vez)

1. En la [consola de Firebase](https://console.firebase.google.com/), crear el proyecto o abrir el existente.
2. Agregar una app Android con el nombre de paquete `com.auracode.hiposim`. No hace falta descargar
   `google-services.json` ni agregar el SDK de Firebase: la app no usa Firebase, solo se distribuye con él.
3. En "App Distribution", pulsar "Comenzar" y crear un grupo de verificadores, por ejemplo `equipo-hiposim`, con los
   correos de quienes van a probar.
4. Copiar el ID de la app (Configuración del proyecto, pestaña "General", "ID de la app"). Tiene la forma
   `1:1234567890:android:0a1b2c3d4e5f67890`.

Instalar la CLI e iniciar sesión:

```powershell
npm install -g firebase-tools
```

```powershell
firebase login
```

Comprobar que la cuenta ve el proyecto y la app:

```powershell
firebase projects:list
```

```powershell
firebase apps:list ANDROID --project <ID_DEL_PROYECTO>
```

## Subir el APK

APK de demostración:

```powershell
firebase appdistribution:distribute app\build\outputs\apk\release\hiposim-0.1.0-demo-release.apk --app <APP_ID> --groups "equipo-hiposim" --release-notes "HipoSim 0.1.0 (demo). Datos en memoria: no necesita servidor. Las cuentas y los envíos a inmobiliarias son simulados."
```

APK conectado a la API:

```powershell
firebase appdistribution:distribute app\build\outputs\apk\release\hiposim-0.1.0-api-release.apk --app <APP_ID> --groups "equipo-hiposim" --release-notes "HipoSim 0.1.0 conectado a la API desplegada."
```

Opciones útiles: `--testers "correo1@ejemplo.com,correo2@ejemplo.com"` en lugar de `--groups`, y
`--release-notes-file notas.txt` para notas largas. Cada verificador recibe un correo con el enlace de instalación.

## Distribución automática con GitHub Actions

El workflow [.github/workflows/distribute.yml](../.github/workflows/distribute.yml) compila el APK `api` firmado y
lo sube a Firebase App Distribution en cada push a `develop` (también se puede lanzar a mano desde la pestaña
Actions). Usa como `versionCode` el número de ejecución más 100, así cada subida es mayor que la anterior.

Necesita estos secretos del repositorio (Settings, Secrets and variables, Actions, pestaña Secrets). Mientras falte
alguno, el workflow termina con un aviso y no sube nada:

| Secreto | Valor |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | El keystore codificado en base64 (comando abajo). |
| `RELEASE_STORE_PASSWORD` | `storePassword` de `keystore.properties`. |
| `RELEASE_KEY_ALIAS` | `keyAlias` de `keystore.properties`. |
| `RELEASE_KEY_PASSWORD` | `keyPassword` de `keystore.properties`. |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | El contenido completo del archivo JSON de una cuenta de servicio con el rol "Administrador de Firebase App Distribution". |

Copiar el keystore en base64 al portapapeles (PowerShell):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\Users\<usuario>\HipoSim\hiposim-signing\hiposim-release.jks")) | Set-Clipboard
```

La cuenta de servicio se crea en la consola de Google Cloud del proyecto de Firebase (IAM y administración, Cuentas
de servicio): crear la cuenta, darle el rol "Administrador de Firebase App Distribution" y, en la pestaña Claves,
agregar una clave JSON. Ese archivo no se guarda en el repositorio.

Opcionalmente, estas variables (pestaña Variables) cambian los valores por defecto del workflow:
`HIPOSIM_API_BASE_URL` (URL de la API, HTTPS y terminada en `/`), `FIREBASE_APP_ID` y `FIREBASE_GROUPS`.

Para una subida manual, pasar un `versionCode` mayor que el último con `-Phiposim.versionCode=<n>`.
