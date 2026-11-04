# CLAUDE.md

Guía para trabajar en este repositorio. **Qué** construir está en `docs/PRD.md`; aquí está **cómo**.

## Proyecto
App interna de gestión hotelera del Hotel Juan Majestic: pedidos, balance, asistencia, reservas, limpieza y mantenimiento.

- Kotlin Multiplatform + Compose Multiplatform: Android, escritorio (JVM) e iOS.
- Backend: Firebase Authentication y Cloud Firestore, a través de GitLive `firebase-kotlin-sdk`.
- Inyección de dependencias: Koin.
- Plan de Firebase: **Spark (gratuito)**. No usar Cloud Functions, Cloud Storage ni nada que exija el plan Blaze (PRD, sección 7).

## El PRD es la fuente de verdad
- Cada requisito tiene un ID: `RF-ORD-04`, `RNF-10`, `S7`, `V2`, etc.
- Antes de un hito, lee en `docs/PRD.md` la fila del hito (sección 10), sus requisitos (sección 6), el modelo de datos (sección 8) y sus validaciones (sección 12). No hace falta leer el PRD completo.
- Si un requisito es ambiguo o falta una decisión de negocio, **no la tomes**: pregunta. La respuesta se anota en el PRD (secciones 12 y 13).

## Estructura
| Módulo | Contenido |
|---|---|
| `shared` | Todo el código común: dominio, datos y UI |
| `androidApp` | Punto de entrada Android |
| `desktopApp` | Punto de entrada de escritorio (JVM) |
| `iosApp` | Proyecto de Xcode |

## Arquitectura: Clean Architecture + MVI
Capas dentro de `shared`, con las dependencias apuntando hacia el dominio: `presentation` → `domain` ← `data`. El dominio no depende de nadie.

- **domain:** entidades, casos de uso e interfaces de repositorio. Las reglas de negocio (totales, transiciones de estado, día operativo) viven aquí. Kotlin puro: sin Firebase, sin Compose y sin APIs de plataforma.
- **Casos de uso:** uno por acción de negocio (`CloseOrderUseCase`, `RegisterPaymentUseCase`), con un solo método público (`operator fun invoke`). La presentación llama a casos de uso, nunca a repositorios.
- **data:** implementaciones de los repositorios con GitLive, DTOs de Firestore y mappers a entidades del dominio. Todo el acceso a Firebase vive aquí, para poder cambiar de librería sin tocar el resto (PRD, R7).
- **presentation (MVI), en el paquete `ui`:** cada pantalla tiene cuatro piezas, con el nombre de la pantalla como prefijo (`OrdersState`, `OrdersIntent`, `OrdersEffect`, `OrdersViewModel`):
    - `State`: data class inmutable con todo lo que la pantalla muestra.
    - `Intent`: sealed interface con todo lo que el usuario puede hacer.
    - `Effect`: sealed interface para eventos de una sola vez, como navegar o mostrar un mensaje.
    - `ViewModel`: recibe los intents por un único punto de entrada (`onIntent`), expone el estado como `StateFlow<State>` y los efectos como un `Flow<Effect>`. El estado solo cambia dentro del ViewModel.
- **Pantallas:** en Compose, solo dibujan el `State` y envían `Intent`. No tienen lógica ni llaman a casos de uso.
- **Koin:** las dependencias se reciben por constructor y se enlazan en módulos de Koin; no se instancian a mano dentro de las clases.
- **Plataforma:** lo que depende de cada plataforma (ubicación, archivos locales) va con `expect`/`actual`, detrás de una interfaz del dominio.
- Si una pantalla existente no sigue este patrón, se adapta cuando se modifique, no en un cambio aparte.

## Principios SOLID
- **Responsabilidad única:** una clase, una razón para cambiar. Un caso de uso por acción, un ViewModel por pantalla y un repositorio por agregado (pedidos, pagos, habitaciones, etc.).
- **Abierto/cerrado:** el comportamiento nuevo se agrega con implementaciones nuevas, no con más `if` por tipo. Por ejemplo, la marcación desde el celular y desde el computador autorizado son dos implementaciones de una misma interfaz.
- **Sustitución de Liskov:** cualquier implementación de una interfaz, la de Firebase o un fake de test, cumple el mismo contrato ante el dominio.
- **Segregación de interfaces:** interfaces pequeñas y específicas. Nada de un repositorio que lo hace todo.
- **Inversión de dependencias:** el dominio define las interfaces y las capas externas las implementan.

## Código legible
- El código se explica solo: nombres claros y completos, sin abreviaturas, y funciones cortas con una sola tarea.
- Los comentarios solo explican un porqué que no se ve en el código, como una regla de negocio poco obvia o una limitación de Firebase, citando el ID del PRD si aplica. Nunca repiten lo que el código ya dice.
- No dejes código comentado ni `TODO` sin un ID del PRD.
- Prefiere la inmutabilidad (`val`, data classes, colecciones de solo lectura) y las funciones puras en el dominio.
- Si una clase o un archivo crece mucho, probablemente tiene más de una responsabilidad: divídelo.

## Comandos
```bash
# Tests: obligatorio antes de dar por terminada cualquier tarea
./gradlew :shared:allTests :shared:testAndroidHostTest

# Compilar
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:assemble
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64   # código común para iOS, solo en macOS

# Ejecutar la app de escritorio
./gradlew :desktopApp:run
```
La app iOS completa se compila desde Xcode (`iosApp/`) en macOS. Si no estás en macOS, dilo en vez de asumir que compila.
El CI (`.github/workflows/ci.yml`) corre los tests y compila Android y escritorio en cada PR hacia `develop`, sin `google-services.json`.

## Convenciones
- Código e identificadores en inglés. Textos que ve el usuario en español de Colombia.
- Dinero en pesos como `Long`, nunca `Double`. Se muestra con `formatPesos` (`$24.500`).
- Fechas: se guardan con la hora del servidor, y se muestran y agrupan en `America/Bogota` por día operativo (configurable, 6:00 a. m. por defecto).
- Colecciones, campos y valores de estado de Firestore: exactamente los de la sección 8 del PRD (`in_progress`, `closedBy`, `RoomNights`, etc.).
- Pedidos, pagos, reservas, marcaciones y tareas no se borran: se cancelan con un motivo y quedan en el historial.
- Los cambios de estado, y todo lo que toca dinero o disponibilidad de habitaciones, van en transacciones de Firestore.
- Consultas acotadas: listeners solo sobre lo abierto y reportes solo del rango elegido. La cuota gratuita es de 50.000 lecturas al día.
- Sin conexión: respeta qué se permite y qué no según RNF-04 del PRD.

## Firestore
- Las reglas van en `firestore.rules` y los índices en `firestore.indexes.json`, en la raíz del repo. Se crean en el hito M2 (S3). Si un cambio necesita un permiso o un índice nuevo, actualízalos en el mismo PR.
- Prueba las reglas con el emulador de Firebase, por rol y por cada cambio de estado (S7).
- Los permisos se aplican en las reglas, no solo en la UI.

## Tests
- Todo caso de uso y todo ViewModel nuevo lleva tests en `shared/src/commonTest`.
- En un ViewModel se prueba que, dado un estado y un intent, salen el estado y los efectos esperados.
- En los tests, los repositorios se reemplazan por fakes. No uses Firebase real.
- Un bug corregido lleva un test que lo reproduce.

## Flujo de trabajo (Gitflow)
- Ramas: `main`, `develop`, `feature/*`, `release/*`, `hotfix/*` y `task/*` para trabajo que no es código.
- Un hito, una rama desde `develop`, con el nombre que da la sección 10 del PRD (por ejemplo, `feature/current-orders`).
- Antes de escribir código, propón un plan y espera la aprobación.
- Al empezar un hito, resuelve primero sus validaciones (V1 a V8) y reporta el resultado.
- Commits pequeños, en inglés, que citen el ID: `RF-CUR-04: close the bill of an order`.
- El PR va hacia `develop` y lista los IDs que cubre.
- Al terminar, cumple la definición de terminado (PRD, sección 11) y actualiza en el PRD el estado de los requisitos.

## Nunca
- Subir secretos: `google-services.json`, `GoogleService-Info.plist`, keystores, tokens o claves.
- Desplegar reglas o índices, borrar datos o cambiar la configuración de Firebase. Eso lo hace una persona.
- Hacer push directo a `main` o `develop`, forzar un push o reescribir el historial.
- Agregar una dependencia sin preguntar. Si se aprueba, tiene que soportar Android, iOS y JVM.
- Usar Cloud Functions, Cloud Storage u otro servicio que exija el plan Blaze.
- Guardar contraseñas en Firestore, o coordenadas de la ubicación de los empleados.
