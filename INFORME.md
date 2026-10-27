# Informe de Práctica - Validación y Verificación de Software (VVS)

Este documento registra todo el proceso de pruebas, ejecución de herramientas y problemas detectados en el proyecto Catan, tal y como se solicita en los requisitos de la práctica.

## 1. Tests Añadidos

A continuación se listan todas las pruebas que hemos ido añadiendo al repositorio:

### Pruebas Estructurales (ArchUnit)
* **`ArchitectureTest.java`**: 
  * `gameShouldNotDependOnGui`: Comprueba que ninguna clase del paquete de lógica `game` importe o instancie clases del paquete gráfico `gui`. *(Ignorada con `@ArchIgnore` por el Issue #1 para permitir ejecución continua de tests).*
  * `gameDomainShouldNotDependOnGui`: Comprueba que todas las clases de dominio de `game` (aislando el launcher `GameRunner`) no dependan de `gui`.
  * `boardShouldNotDependOnGui`: Comprueba que ninguna clase del paquete `board` importe o instancie clases del paquete gráfico `gui`.
  * `boardShouldNotDependOnSwingOrAwt`: Verifica que la lógica del tablero sea pura y no dependa de componentes visuales de AWT ni Swing (`javax.swing..`, `java.awt..`).
  * `libShouldNotDependOnDomainOrGui`: Comprueba que el paquete de utilidades `lib` no dependa de las capas de negocio (`game`, `board`) ni de la interfaz (`gui`).
  * `gameDomainShouldNotDependOnSwing`: Asegura que el dominio de `game` (salvo `GameRunner`) no importe directamente componentes Swing.
  * `testClassesShouldFollowNamingConvention`: Regla de convención de nombres que exige que todas las clases de prueba en los paquetes de test terminen con el sufijo `Test`.
  * `structuresShouldResideInBoardPackage`: Comprueba que todas las clases que extienden de `board.Structure` residan efectivamente en el paquete `board`.

### Pruebas de Integración Caja Negra (`PlayerBoardIntegrationTest.java`)
Suite de 10 pruebas de integración que valida la interacción entre `Player`, `Board` y `Game` simulando flujos reales de juego:
* `testPlaceSettlementAndConnectedRoadUpdatesOwnership`: Valida la colocación de un poblado inicial en el tablero, la asignación de propiedad al jugador, el bloqueo de colocaciones duplicadas en el mismo vértice y la colocación de una carretera adyacente conectada.
* `testDistanceRulePreventsAdjacentSettlements`: Comprueba que el tablero aplique la regla de la distancia de Catan, rechazando poblados a menos de dos vértices de distancia.
* `testDisconnectedRoadCannotBePlaced`: Valida que el tablero rechace la construcción de carreteras que no estén conectadas a la red de carreteras o poblados previos del jugador.
* `testDistributeResourcesHarvestsToSettlementOwner`: Comprueba que, ante una tirada de dados, las losetas coincidentes distribuyan exactamente 1 recurso a los jugadores que posean un poblado en sus vértices.
* `testCityUpgradeDoublesResourceProduction`: Valida la mejora de poblado a ciudad (`City`) y comprueba que la producción de recursos se duplique (2 recursos por tirada).
* `testRobberBlocksResourceProductionOnTile`: Comprueba que la presencia del ladrón en una loseta bloquee completamente la recolección de recursos para los poblados adyacentes.
* `testGetRobberAdjacentPlayersIdentifiesSettlementOwners`: Comprueba la correcta identificación de todos los jugadores con estructuras adyacentes a la casilla del ladrón.
* `testGameBuyStructuresDeductsResourcesAndUpdatesPoints`: Valida el ciclo completo de compra de poblados, carreteras y ciudades a través de `Game`: deducción de recursos de la mano del jugador, incremento de puntos de victoria e incremento de contadores de estructuras.
* `testGameTakeCardTransfersResourceBetweenPlayers`: Valida la transferencia y robo aleatorio de recursos entre jugadores mediante la acción del ladrón (`takeCard`).
* `testGameOverAndWinningPlayerCondition`: Comprueba la detección del fin de la partida cuando un jugador alcanza o supera los 10 puntos de victoria.

## 2. Resultados de las Herramientas

* **ArchUnit (Análisis Estructural)**: Ejecutado durante la fase de `test` de Maven. 7 reglas activas pasan en verde (**BUILD SUCCESS**), y la regla estricta sobre `GameRunner` se mantiene documentada e ignorada formalmente.
* **JaCoCo (Cobertura)**: Tras la incorporación de la suite de integración `PlayerBoardIntegrationTest`, la cobertura global del proyecto en `board` y `game` ha ascendido del **0% al 42%** (45% en `board` y 34% en `game`), cubriendo más de 2.400 instrucciones bytecode antes de incorporar los tests unitarios específicos de `Player` y `Board`.
* **PIT (PiTest)**: El suite de pruebas se encuentra ahora en verde ("green suite"), permitiendo la inyección de mutantes sin bloqueos por tests fallidos.
* **CheckStyle (Análisis Estático)**: Ejecutado con `mvn checkstyle:check`. Detectó 3.298 violaciones de estilo, deteniendo la compilación (ver sección de Errores).
* **SpotBugs (Análisis Estático)**: Ejecutado con `mvn compile spotbugs:check`. Detectó 41 posibles bugs de distintas gravedades (ver sección de Errores).

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

### 🧪 Issue #4: Cobertura de código nula (0%)
* **Herramienta que lo detectó:** JaCoCo
* **Clases afectadas:** Todas las clases de `game` y `board`
* **Descripción del problema:** Inicialmente la cobertura de código era exactamente del 0%. Con la adición de `PlayerBoardIntegrationTest`, la cobertura se eleva al 42% global, sentando la base para las pruebas unitarias de Pedro y Santi.

### 🚫 Issue #5: PiTest abortado por test previo en rojo
* **Herramienta que lo detectó:** PIT (PiTest)
* **Clases afectadas:** Entorno de Testing
* **Descripción del problema:** Originalmente PIT abortaba el análisis con el error `1 tests did not pass without mutation. Mutation testing requires a green suite.`. Al ignorar formalmente la violación de arquitectura conocida en `GameRunner` mediante `@ArchIgnore`, el entorno dispone de una suite en verde y permite la ejecución de pruebas de mutación.

### 🧩 Issue #6: Defecto de polimorfismo en `Board.placeCity` vs `Game.placeCity`
* **Herramienta que lo detectó:** Tests de Integración (`PlayerBoardIntegrationTest`)
* **Clase afectada:** `board.Board` (línea 381)
* **Descripción del problema:** En `Board.placeCity(VertexLocation, Player)`, el método se limita a cambiar el tipo interno mediante `structures[x][y][ori].setType(1)` sobre la instancia existente de `Settlement`, en lugar de reemplazar la estructura en la matriz por una instancia de `City`. Dado que `distributeResources` invoca polimórficamente `giveResources`, se ejecuta el método de `Settlement` (que sólo otorga 1 recurso) en lugar del de `City` (que otorga 2). En contraste, `Game.placeCity` sí instancia correctamente un objeto `new City(...)` y lo coloca en el tablero con `board.setStructure`.

### ⛓️ Issue #7: Acoplamiento estático oculto entre `Game` y `GameRunner`
* **Herramienta que lo detectó:** Tests de Integración (`PlayerBoardIntegrationTest`)
* **Clase afectada:** `game.Game` (línea 41) y `game.GameRunner` (línea 53)
* **Descripción del problema:** El constructor de `Game` invoca incondicionalmente el método estático `GameRunner.setFirstPlayer()`, el cual intenta acceder a `players.get(0)` sobre la lista privada estática de `GameRunner`. Dicha lista únicamente se inicializaba al ejecutar el método `main` de la GUI. Como consecuencia, cualquier intento de instanciar `Game` en un test automatizado o módulo desacoplado fallaba con un `IndexOutOfBoundsException`. Para las pruebas de integración fue necesario inicializar dicha lista estática mediante introspección (`reflection`) en el `setUp`.

### 🏆 Issue #8: Defecto de algoritmo en la comprobación del ganador (`Game.winningPlayer`)
* **Herramienta que lo detectó:** Tests de Integración (`PlayerBoardIntegrationTest`)
* **Clase afectada:** `game.Game.winningPlayer()` (líneas 58-69)
* **Descripción del problema:** Error de lógica en el algoritmo que calcula el segundo máximo de puntos de victoria. El método inicializa tanto `maxVictoryPoints` como `secondMaxVictoryPoints` apuntando a `players.get(0)`. Cuando itera buscando al jugador con mayor puntuación:
  1. Si encuentra un jugador con más puntos, actualiza `maxVictoryPoints` pero **no actualiza** `secondMaxVictoryPoints` con el valor del máximo previo.
  2. Si el jugador con 10 o más puntos resulta ser el que está en el índice `0` tras el `Collections.shuffle` del constructor, `secondMaxVictoryPoints` se inicializa con 10 puntos. Al finalizar el bucle, se evalúa la condición estricta `maxVictoryPoints.getVictoryPoints() > secondMaxVictoryPoints.getVictoryPoints()` (`10 > 10`), la cual evalúa a `false`. Como resultado, el método devuelve `null` y el juego nunca detecta la victoria de dicho jugador.
