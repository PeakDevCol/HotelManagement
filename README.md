# Hotel Management

App interna del Hotel Juan Majestic: pedidos, balance, asistencia, reservas, limpieza y mantenimiento.
Kotlin Multiplatform + Compose Multiplatform para **Android, iOS y escritorio**, con Firebase como backend.

El alcance, los requisitos y la hoja de ruta están en el [PRD](docs/PRD.md).

## Módulos

| Módulo | Qué contiene |
|---|---|
| `shared` | Código común: dominio, datos (Firebase vía [GitLive](https://github.com/GitLiveApp/firebase-kotlin-sdk)), Koin y la UI en Compose. |
| `androidApp` | Punto de entrada Android (`MainActivity`). |
| `desktopApp` | Punto de entrada de escritorio (JVM): macOS, Windows y Linux. |
| `iosApp` | Proyecto de Xcode (no es un módulo de Gradle). Firebase iOS se añade con Swift Package Manager. |

Dentro de `shared/src/commonMain`:

```
domain/   modelos, interfaces de repositorio y casos de uso (sin dependencias de plataforma)
data/     implementaciones con Firebase y DTOs de Firestore
di/       módulos de Koin
ui/       tema, navegación y pantallas
```

## Requisitos

- Android Studio reciente (JDK 17 o superior). Gradle 9.6.1 y AGP 9.4.
- Xcode 26 o superior para iOS.

## Configuración de Firebase

Los archivos de configuración de Firebase **no se suben al repo** (están en `.gitignore`).
Descárgalos desde la consola de Firebase del proyecto `hotelmanagementpdc`:

| Plataforma | Archivo | Ubicación |
|---|---|---|
| Android | `google-services.json` | `androidApp/` |
| Escritorio | El mismo `google-services.json` | Se lee de `androidApp/` al compilar. Las opciones se generan en `desktopApp/build/`. |
| iOS | `GoogleService-Info.plist` (app iOS con bundle id `com.peakdevcol.project.hotelmanagement`) | `iosApp/iosApp/` |

Sin el archivo, las apps compilan y arrancan igual, pero con Firebase desactivado. Así compila el CI.

## Ejecutar

```bash
# Android
./gradlew :androidApp:installDebug

# Escritorio
./gradlew :desktopApp:run

# Tests del código compartido
./gradlew :shared:allTests :shared:testAndroidHostTest
```

iOS: abre `iosApp/iosApp.xcodeproj` en Xcode y ejecuta el esquema `iosApp`.
Para un dispositivo físico, pon tu `TEAM_ID` en `iosApp/Configuration/Config.xcconfig`.

## Flujo de trabajo (Gitflow)

- `main`: producción. Solo recibe merges de `release/*` y `hotfix/*`.
- `develop`: integración.
- `feature/<nombre>`: sale de `develop` y vuelve a `develop` mediante un Pull Request.
- `release/x.y.z`: sale de `develop` y se fusiona en `main` y `develop`, con el tag `vX.Y.Z`.
- `hotfix/<nombre>`: sale de `main` y se fusiona en `main` y `develop`.
- `task/<nombre>`: trabajo que no es código, como la documentación. Sale de `develop` y vuelve a `develop` mediante un Pull Request.
