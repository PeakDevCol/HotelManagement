# Licencias de terceros

Archivos de terceros que van dentro de la app. No son dependencias: se copiaron al repositorio.

| Qué | Dónde está | Licencia | Origen |
|---|---|---|---|
| Fuente Manrope, pesos 500, 600, 700 y 800 | `shared/src/commonMain/composeResources/font/` | SIL Open Font License 1.1 (`Manrope-OFL.txt`) | `google/fonts`, `ofl/manrope`. Los pesos estáticos se generaron de la fuente variable con `fontTools`. |
| Íconos Material Symbols Rounded | `shared/src/commonMain/composeResources/drawable/ic_*.xml` | Apache 2.0 (`MaterialSymbols-Apache-2.0.txt`) | `google/material-design-icons`, `symbols/android`. Sin el atributo `android:tint`, que Compose no lee. |
