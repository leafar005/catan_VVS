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
* *(Pendiente)* **CheckStyle / SpotBugs**: Análisis estático sin ejecutar todavía.
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
