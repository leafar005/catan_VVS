# Informe de Práctica - Validación y Verificación de Software (VVS)

Este documento registra todo el proceso de pruebas, ejecución de herramientas y problemas detectados en el proyecto Catan, tal y como se solicita en los requisitos de la práctica.

## 1. Tests Añadidos

A continuación se listan todas las pruebas que hemos ido añadiendo al repositorio:

### Pruebas Estructurales (ArchUnit)
* **`ArchitectureTest.java`**: 
  * `gameShouldNotDependOnGui`: Comprueba que ninguna clase del paquete de lógica `game` importe o instancie clases del paquete gráfico `gui`.
  * `boardShouldNotDependOnGui`: Comprueba que ninguna clase del paquete `board` importe o instancie clases del paquete gráfico `gui`.

## 2. Resultados de las Herramientas

* **ArchUnit (Análisis Estructural)**: Ejecutado durante la fase de `test` de Maven. El test `gameShouldNotDependOnGui` **FALLA**, demostrando un acoplamiento incorrecto en la arquitectura (ver sección de Errores).
* **CheckStyle (Análisis Estático)**: Ejecutado con `mvn checkstyle:check`. Detectó 3.298 violaciones de estilo, deteniendo la compilación (ver sección de Errores).
* **SpotBugs (Análisis Estático)**: Ejecutado con `mvn compile spotbugs:check`. Detectó 41 posibles bugs de distintas gravedades (ver sección de Errores).
* *(Pendiente)* **JaCoCo**: Análisis de cobertura de código sin ejecutar todavía.
* *(Pendiente)* **PIT (PiTest)**: Análisis de mutaciones sin ejecutar todavía.

## 3. Errores y Problemas Detectados (Issues)

Aquí documentaremos cualquier *bug*, defecto o problema de diseño detectado mediante las herramientas, sin obligación de solucionarlo en el código fuente.

### 🐛 Issue #1: Violación de la arquitectura de capas en `GameRunner`
* **Herramienta que lo detectó:** ArchUnit
* **Clase afectada:** `game.GameRunner`
* **Descripción del problema:** La clase `GameRunner`, que teóricamente pertenece a la capa de lógica de negocio pura (`game`), está instanciando y manipulando directamente componentes visuales como `GameWindow` y `CatanBoard`. Esto rompe el principio de separación de responsabilidades y la arquitectura de capas, haciendo que la lógica del juego sea inseparable de la interfaz gráfica de Swing.
* **Traza del error:**
  ```text
  Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package '..game..' should depend on classes that reside in a package '..gui..'' was violated (3 times):
  Method <game.GameRunner$1.run()> calls constructor <gui.GameWindow.<init>(java.util.ArrayList)> in (GameRunner.java:32)
  Method <game.GameRunner$1.run()> calls method <gui.CatanBoard.getGame()> in (GameRunner.java:33)
  Method <game.GameRunner$1.run()> calls method <gui.GameWindow.getBoard()> in (GameRunner.java:33)
  ```

### 🧹 Issue #2: Violaciones masivas de estilo de código
* **Herramienta que lo detectó:** CheckStyle
* **Clases afectadas:** Todo el proyecto (especialmente la capa `gui` y `lib`)
* **Descripción del problema:** Se han detectado 3.298 violaciones de estilo (espacios incorrectos, falta de JavaDoc, números mágicos, omisión de `final` en parámetros, etc.). Debido al altísimo volumen de errores de formato heredados en el código, se documenta el hallazgo pero no se corregirán por exceder el ámbito de la práctica.

### 🐞 Issue #3: Errores graves de lógica y comparación
* **Herramienta que lo detectó:** SpotBugs
* **Clase afectada:** Principalmente `game.Player`
* **Descripción del problema:** SpotBugs ha encontrado 41 defectos de código. Entre ellos destacan errores **críticos** de programación en Java que provocan comportamientos indeseados: comparar `String` usando `==` en vez de `.equals()` (`ES_COMPARING_PARAMETER_STRING_WITH_EQ`), y comparar un `ArrayList` con un `String` (`EC_UNRELATED_TYPES`), lo cual siempre devolverá falso.
* **Ejemplos de la traza:**
  - `High: Call to java.util.ArrayList<java.lang.String>.equals(String) in game.Player.hasResources(ArrayList)`
  - `High: Comparison of String parameter using == or != in game.Player.giveResourceType(String)`
