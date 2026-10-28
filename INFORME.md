# Informe de Práctica - Validación y Verificación de Software (VVS)

Este documento registra todo el proceso de pruebas, ejecución de herramientas y problemas detectados en el proyecto Catan, tal y como se solicita en los requisitos de la práctica.

## 1. Tests Añadidos

A continuación se listan todas las pruebas que hemos ido añadiendo al repositorio:

### Pruebas Estructurales (ArchUnit)

- **`ArchitectureTest.java`**:
  - `gameShouldNotDependOnGui`: Comprueba que ninguna clase del paquete de lógica `game` importe o instancie clases del paquete gráfico `gui`. _(Ignorada con `@ArchIgnore` por el Issue #1 para permitir ejecución continua de tests)._
  - `gameDomainShouldNotDependOnGui`: Comprueba que todas las clases de dominio de `game` (aislando el launcher `GameRunner`) no dependan de `gui`.
  - `boardShouldNotDependOnGui`: Comprueba que ninguna clase del paquete `board` importe o instancie clases del paquete gráfico `gui`.
  - `boardShouldNotDependOnSwingOrAwt`: Verifica que la lógica del tablero sea pura y no dependa de componentes visuales de AWT ni Swing (`javax.swing..`, `java.awt..`).
  - `libShouldNotDependOnDomainOrGui`: Comprueba que el paquete de utilidades `lib` no dependa de las capas de negocio (`game`, `board`) ni de la interfaz (`gui`).
  - `gameDomainShouldNotDependOnSwing`: Asegura que el dominio de `game` (salvo `GameRunner`) no importe directamente componentes Swing.
  - `testClassesShouldFollowNamingConvention`: Regla de convención de nombres que exige que todas las clases de prueba en los paquetes de test terminen con el sufijo `Test`.
  - `structuresShouldResideInBoardPackage`: Comprueba que todas las clases que extienden de `board.Structure` residan efectivamente en el paquete `board`.

### Pruebas de Integración Caja Negra (`PlayerBoardIntegrationTest.java`)

Suite de 10 pruebas de integración que valida la interacción entre `Player`, `Board` y `Game` simulando flujos reales de juego:

- `testPlaceSettlementAndConnectedRoadUpdatesOwnership`: Valida la colocación de un poblado inicial en el tablero, la asignación de propiedad al jugador, el bloqueo de colocaciones duplicadas en el mismo vértice y la colocación de una carretera adyacente conectada.
- `testDistanceRulePreventsAdjacentSettlements`: Comprueba que el tablero aplique la regla de la distancia de Catan, rechazando poblados a menos de dos vértices de distancia.
- `testDisconnectedRoadCannotBePlaced`: Valida que el tablero rechace la construcción de carreteras que no estén conectadas a la red de carreteras o poblados previos del jugador.
- `testDistributeResourcesHarvestsToSettlementOwner`: Comprueba que, ante una tirada de dados, las losetas coincidentes distribuyan exactamente 1 recurso a los jugadores que posean un poblado en sus vértices.
- `testCityUpgradeDoublesResourceProduction`: Valida la mejora de poblado a ciudad (`City`) y comprueba que la producción de recursos se duplique (2 recursos por tirada).
- `testRobberBlocksResourceProductionOnTile`: Comprueba que la presencia del ladrón en una loseta bloquee completamente la recolección de recursos para los poblados adyacentes.
- `testGetRobberAdjacentPlayersIdentifiesSettlementOwners`: Comprueba la correcta identificación de todos los jugadores con estructuras adyacentes a la casilla del ladrón.
- `testGameBuyStructuresDeductsResourcesAndUpdatesPoints`: Valida el ciclo completo de compra de poblados, carreteras y ciudades a través de `Game`: deducción de recursos de la mano del jugador, incremento de puntos de victoria e incremento de contadores de estructuras.
- `testGameTakeCardTransfersResourceBetweenPlayers`: Valida la transferencia y robo aleatorio de recursos entre jugadores mediante la acción del ladrón (`takeCard`).
- `testGameOverAndWinningPlayerCondition`: Comprueba la detección del fin de la partida cuando un jugador alcanza o supera los 10 puntos de victoria.

### Pruebas Unitarias Caja Negra de `Board` (`test/board/BoardTest.java`)

Suite de 148 métodos de test activos (más 9 desactivados que documentan defectos), organizada en grupos `@Nested` y con un único aspecto comprobado por test. Como el tablero se genera aleatoriamente (`Collections.shuffle`), los tests nunca suponen qué recurso o número hay en cada casilla: recorren `getTiles()` o fijan el estado con los setters (`setNumber`, `setOwner`, `setStructure`, `setRobberLocation`).

- **Constructor** (`@RepeatedTest(10)` sobre tableros aleatorios): 19 casillas de tierra en sus coordenadas, reparto de recursos del reglamento (4/3/4/4/3 + desierto), los 18 números del reglamento, ningún 7, desierto con número 0 y ladrón, ladrón en una única casilla, dimensiones de las matrices, estructuras y carreteras sin dueño.
- **Getters/setters**: `getStructure`, `setStructure`, `getRoad`, `getTile`, `get/setRobberLocation` y coordenadas fuera de la matriz.
- **`placeStructureNoRoad` y `placeStructure`**: vértice libre/ocupado, orientaciones 0 y 1, cada uno de los 3 vecinos de la regla de distancia por separado, cada una de las 3 carreteras que dan acceso, carretera rival, y valores frontera del tablero (`y = 1, 2, 4, 5`).
- **Puertos** (`checkPort`, probado a través de la colocación): los 6 tipos de puerto, los 18 vértices de puerto, vértices con las mismas coordenadas y la otra orientación, vértice interior, y que no se otorgue el puerto si la colocación falla.
- **`placeRoad`**: las 3 orientaciones, cada una de sus 6 conexiones posibles (2 poblados + 4 carreteras) por separado, arista ocupada, conexión rival, asignación de dueño y lista de carreteras del jugador.
- **`placeCity`**: poblado propio, ciudad ya existente, poblado rival y vértice vacío.
- **`distributeResources`**: poblado (+1), ciudad (+2), número que no coincide, ladrón, desierto, dos jugadores, jugador lejano, dos casillas con el mismo número y tiradas frontera 1, 2, 7, 12 y 13.
- **Ladrón**: `moveRobber` (misma casilla, misma X / misma Y, casilla distinta, origen y destino) y `getRobberAdjacentPlayers` (vacío, sin duplicados, dos jugadores, jugador fuera de la casilla).
- **`getAdjacentTilesStructure`**: vértices interiores, de costa y de mar en ambas orientaciones.
- **Propiedades jqwik**: cualquier vértice de la casilla tirada recibe 1 recurso; para cualquier tirada 2–12 el vértice central recibe tantos recursos como casillas productoras toca; tiradas > 12 o negativas nunca producen; el ladrón siempre está en una sola casilla; el dueño de cualquier vértice de la casilla del ladrón es adyacente; cualquier vértice de tierra acepta el primer poblado.
- **Tests desactivados (`@Disabled`)**: describen el comportamiento correcto en los Issues #6, #9, #10, #11, #12, #13, #14 y #15. Se ha comprobado que fallan al activarlos; se mantienen desactivados para que PITest disponga de una suite en verde.

### Pruebas Unitarias de `Deck` (`test/board/DeckTest.java`)

Suite de 3 pruebas unitarias para aislar y validar el comportamiento de la baraja de desarrollo (`Deck`), resolviendo la ausencia de pruebas en PITest:
- `deckInitialization_containsExactly25CardsWithCorrectDistribution`: Comprueba la extracción secuencial de las 25 cartas de desarrollo y verifica la distribución exacta de tipos y subtipos del reglamento (14 Caballeros, 5 Puntos de Victoria, 2 Construcción de carreteras, 2 Año de la abundancia y 2 Monopolio).
- `draw_returnsNullWhenDeckIsEmpty`: Verifica que al extraer las 25 cartas la baraja quede vacía y las extracciones subsiguientes devuelvan `null`.
- `isEmpty_reflectsDeckStateAccurately`: Valida que `isEmpty()` devuelva `false` mientras queden cartas y pase a `true` tras retirar la última carta.

### Pruebas Unitarias de Estructuras (`test/board/StructureUnitTest.java`)

Suite de 7 pruebas unitarias destinadas a aniquilar mutantes supervivientes en `City`, `Settlement` y `Road`:
- `cityConstructor_setsLocationAndType`: Valida la asignación de `VertexLocation` y `type = 1` en `City` (aniquila mutantes de `setLocation` y `setType`).
- `cityGiveResources_incrementsOwnerResourcesByTwo`: Comprueba que `City.giveResources` otorgue exactamente 2 unidades de recurso al dueño.
- `settlementConstructor_setsLocationAndType`: Valida que `Settlement` fije su `VertexLocation` y `type = 0` (aniquila mutante de `setType(0)`).
- `settlementGiveResources_incrementsOwnerResourcesByOne`: Comprueba que `Settlement.giveResources` incremente en 1 unidad el recurso del dueño.
- `roadConstructor_setsLocationAndNullOwner`: Valida `EdgeLocation`, dueño nulo inicial y bandera de visita en `Road`.
- `roadVisit_togglesVisitedState`: Valida el ciclo completo de marcado (`isVisited()`, `visit()` y `resetVisited()`).
- `boardPlaceRoad_setsOwnerForOrientationsOneAndTwo`: Valida que `Board.placeRoad` asigne correctamente el dueño en aristas de orientación 1 y 2 (aniquila mutantes en líneas 346 y 362 de `Board`).

### Pruebas Unitarias de Compras en `Game` (`test/game/GamePurchaseTest.java`)

Suite de 6 pruebas unitarias para eliminar mutantes supervivientes en las transacciones de compra de `Game`:
- `buyRoad_deductsExactResources`: Comprueba la deducción exacta de 1 BRICK y 1 LUMBER de la mano del jugador.
- `buySettlement_deductsExactResources`: Valida el descuento exacto de 1 BRICK, 1 LUMBER, 1 GRAIN y 1 WOOL.
- `buyCity_deductsExactResources`: Valida el descuento exacto de 2 GRAIN y 3 ORE al comprar una ciudad.
- `buyRoad_capacityLimitReached`, `buySettlement_capacityLimitReached`, `buyCity_capacityLimitReached`: Comprueban el rechazo con código 2 y la preservación intacta de recursos al alcanzar los límites máximos (15 carreteras, 5 poblados, 4 ciudades).

### Pruebas de Rendimiento No Funcional con JETM (`test/performance/GamePerformanceTest.java`)

Suite de 2 pruebas de rendimiento y profiling continuo mediante JETM (`fm.void.jetm`):
- `profileBoardCreationPerformance`: Monitoriza 100 iteraciones de instanciación completa del tablero (`Board::Instantiation`), verificando que el tiempo medio sea muy inferior a 50 ms (obtenido: ~0.100 ms).
- `profileResourceDistributionPerformance`: Monitoriza 500 iteraciones del reparto de recursos con tiradas 2-12 (`Board::distributeResources`), verificando tiempos medios inferiores a 5 ms (obtenido: ~0.004 ms). Renderiza informe estadístico en consola.

## 2. Resultados de las Herramientas

- **ArchUnit (Análisis Estructural)**: Ejecutado durante la fase de `test` de Maven. 7 reglas activas pasan en verde (**BUILD SUCCESS**), y la regla estricta sobre `GameRunner` se mantiene documentada e ignorada formalmente.
- **JaCoCo (Cobertura)**: Ejecutado automáticamente en la fase `test` de Maven (`mvn clean test`), informe en `target/site/jacoco/index.html`. Evolución:
  - Sin tests: **0 %**.
  - Con `PlayerBoardIntegrationTest`: **42 %** global (45 % en `board`, 34 % en `game`).
  - Con los tests unitarios de `Player` y `Board`: **62 % de instrucciones y 60 % de ramas** global.
  - Con la suite completa de métricas y mutación (361 tests): **64 % de instrucciones, 61 % de ramas y 65 % de líneas** global.

  | Paquete   | Instrucciones | Ramas    | Líneas               | Métodos       |
  | --------- | ------------- | -------- | -------------------- | ------------- |
  | `board`   | 65 %          | 61 %     | 346 / 508 (68 %)     | 62 / 66       |
  | `game`    | 64 %          | 59 %     | 206 / 323 (64 %)     | 42 / 52       |
  | `runner`  | 10 %          | n/a      | 4 / 27               | 2 / 13        |
  | **Total** | **64 %**      | **61 %** | **556 / 858 (65 %)** | **106 / 131** |

  En la clase `Board`, todo el código está cubierto al 100% salvo el cálculo del camino más largo (`findLongestRoad` y sus tres métodos auxiliares), que no se puede alcanzar por los Issues #9 y #10. Las clases auxiliares `City`, `Settlement`, `Road`, `Deck`, `DevCard`, `Tile`, `Location`, `VertexLocation` y `EdgeLocation` están cubiertas al **100 %**. En `game`, `Player` está cubierto al **100 %**. `runner.GameRunner` es el lanzador de la interfaz gráfica y no es objeto de pruebas unitarias.

- **PIT (PiTest - Pruebas de Mutación)**:
  - Suite de 361 tests en verde ejecutada con `mvn test-compile pitest:mutationCoverage`.
  - Mutaciones totales generadas: **796**.
  - Mutantes aniquilados (**Killed**): **492** (62 % de cobertura de mutación global).
  - Fuerza de las pruebas (**Test Strength**): **96 %** (492 mutantes matados de 515 cubiertos).
  - Cobertura de mutación por clases clave:
    - `Player.java`: **100 %** (89 / 89 mutantes muertos).
    - `City.java`: **100 %** (4 / 4 mutantes muertos).
    - `Road.java`: **100 %** (6 / 6 mutantes muertos).
    - `Structure.java`: **100 %** (4 / 4 mutantes muertos).
    - `DevCard.java`, `Tile.java`, `Location.java`, `EdgeLocation.java`, `VertexLocation.java`: **100 %**.
    - `Deck.java`: **91 %** (10 / 11 mutantes muertos; el único mutante superviviente corresponde a la llamada estocástica `Collections.shuffle`).
    - `Board.java`: **98 % de Test Strength** (289 mutantes muertos de 295 evaluados bajo cobertura).
    - `Game.java`: **92 % de Test Strength** (165 mutantes muertos).
- **JETM (Rendimiento y Profiling)**:
  - Integrado y ejecutado en `GamePerformanceTest.java`.
  - Instanciación de `Board`: 0.100 ms de media (100 ejecuciones).
  - Distribución de recursos: 0.004 ms de media (500 ejecuciones).
- **CheckStyle (Análisis Estático)**: Ejecutado con `mvn checkstyle:check`. Detectó 3.298 violaciones de estilo, deteniendo la compilación (ver sección de Errores).
- **SpotBugs (Análisis Estático)**: Ejecutado con `mvn compile spotbugs:check`. 0 bugs de prioridad alta (High) tras las correcciones de `Player.java`. Se detectan 31 advertencias de nivel medio (Medium) mayoritariamente en GUI y getters/setters internos (ver sección de Errores).

## 3. Errores y Problemas Detectados (Issues)

Aquí documentaremos cualquier _bug_, defecto o problema de diseño detectado mediante las herramientas, sin obligación de solucionarlo en el código fuente.

### 🐛 Issue #1: Violación de la arquitectura de capas en `GameRunner`

- **Herramienta que lo detectó:** ArchUnit
- **Clase afectada:** `game.GameRunner`
- **Descripción del problema:** La clase `GameRunner`, que teóricamente pertenece a la capa de lógica de negocio pura (`game`), está instanciando y manipulando directamente componentes visuales como `GameWindow` y `CatanBoard`. Esto rompe el principio de separación de responsabilidades y la arquitectura de capas, haciendo que la lógica del juego sea inseparable de la interfaz gráfica de Swing.
- **Traza del error:**
  ```text
  Architecture Violation [Priority: MEDIUM] - Rule 'no classes that reside in a package '..game..' should depend on classes that reside in a package '..gui..'' was violated (3 times):
  Method <game.GameRunner$1.run()> calls constructor <gui.GameWindow.<init>(java.util.ArrayList)> in (GameRunner.java:32)
  Method <game.GameRunner$1.run()> calls method <gui.CatanBoard.getGame()> in (GameRunner.java:33)
  Method <game.GameRunner$1.run()> calls method <gui.GameWindow.getBoard()> in (GameRunner.java:33)
  ```

### 🧹 Issue #2: Violaciones masivas de estilo de código

- **Herramienta que lo detectó:** CheckStyle
- **Clases afectadas:** Todo el proyecto (especialmente la capa `gui` y `lib`)
- **Descripción del problema:** Se han detectado 3.298 violaciones de estilo (espacios incorrectos, falta de JavaDoc, números mágicos, omisión de `final` en parámetros, etc.). Debido al altísimo volumen de errores de formato heredados en el código, se documenta el hallazgo pero no se corregirán por exceder el ámbito de la práctica.

### 🐞 Issue #3: Errores graves de lógica y comparación

- **Herramienta que lo detectó:** SpotBugs
- **Clase afectada:** Principalmente `game.Player`
- **Descripción del problema:** SpotBugs ha encontrado 41 defectos de código. Entre ellos destacan errores **críticos** de programación en Java que provocan comportamientos indeseados: comparar `String` usando `==` en vez de `.equals()` (`ES_COMPARING_PARAMETER_STRING_WITH_EQ`), y comparar un `ArrayList` con un `String` (`EC_UNRELATED_TYPES`), lo cual siempre devolverá falso.
- **Ejemplos de la traza:**
  - `High: Call to java.util.ArrayList<java.lang.String>.equals(String) in game.Player.hasResources(ArrayList)`
  - `High: Comparison of String parameter using == or != in game.Player.giveResourceType(String)`

### 🧪 Issue #4: Cobertura de código nula (0%)

- **Herramienta que lo detectó:** JaCoCo
- **Clases afectadas:** Todas las clases de `game` y `board`
- **Descripción del problema:** Inicialmente la cobertura de código era exactamente del 0%. Con la adición de `PlayerBoardIntegrationTest`, la cobertura se eleva al 42% global, sentando la base para las pruebas unitarias de Pedro y Santi.

### 🚫 Issue #5: PiTest abortado por test previo en rojo

- **Herramienta que lo detectó:** PIT (PiTest)
- **Clases afectadas:** Entorno de Testing
- **Descripción del problema:** Originalmente PIT abortaba el análisis con el error `1 tests did not pass without mutation. Mutation testing requires a green suite.`. Al ignorar formalmente la violación de arquitectura conocida en `GameRunner` mediante `@ArchIgnore`, el entorno dispone de una suite en verde y permite la ejecución de pruebas de mutación.

### 🧩 Issue #6: Defecto de polimorfismo en `Board.placeCity` vs `Game.placeCity`

- **Herramienta que lo detectó:** Tests de Integración (`PlayerBoardIntegrationTest`)
- **Clase afectada:** `board.Board` (línea 381)
- **Descripción del problema:** En `Board.placeCity(VertexLocation, Player)`, el método se limita a cambiar el tipo interno mediante `structures[x][y][ori].setType(1)` sobre la instancia existente de `Settlement`, en lugar de reemplazar la estructura en la matriz por una instancia de `City`. Dado que `distributeResources` invoca polimórficamente `giveResources`, se ejecuta el método de `Settlement` (que sólo otorga 1 recurso) en lugar del de `City` (que otorga 2). En contraste, `Game.placeCity` sí instancia correctamente un objeto `new City(...)` y lo coloca en el tablero con `board.setStructure`.

### ⛓️ Issue #7: Acoplamiento estático oculto entre `Game` y `GameRunner`

- **Herramienta que lo detectó:** Tests de Integración (`PlayerBoardIntegrationTest`)
- **Clase afectada:** `game.Game` (línea 41) y `game.GameRunner` (línea 53)
- **Descripción del problema:** El constructor de `Game` invoca incondicionalmente el método estático `GameRunner.setFirstPlayer()`, el cual intenta acceder a `players.get(0)` sobre la lista privada estática de `GameRunner`. Dicha lista únicamente se inicializaba al ejecutar el método `main` de la GUI. Como consecuencia, cualquier intento de instanciar `Game` en un test automatizado o módulo desacoplado fallaba con un `IndexOutOfBoundsException`. Para las pruebas de integración fue necesario inicializar dicha lista estática mediante introspección (`reflection`) en el `setUp`.

### 🏆 Issue #8: Defecto de algoritmo en la comprobación del ganador (`Game.winningPlayer`)

- **Herramienta que lo detectó:** Tests de Integración (`PlayerBoardIntegrationTest`)
- **Clase afectada:** `game.Game.winningPlayer()` (líneas 58-69)
- **Descripción del problema:** Error de lógica en el algoritmo que calcula el segundo máximo de puntos de victoria. El método inicializa tanto `maxVictoryPoints` como `secondMaxVictoryPoints` apuntando a `players.get(0)`. Cuando itera buscando al jugador con mayor puntuación:
  1. Si encuentra un jugador con más puntos, actualiza `maxVictoryPoints` pero **no actualiza** `secondMaxVictoryPoints` con el valor del máximo previo.
  2. Si el jugador con 10 o más puntos resulta ser el que está en el índice `0` tras el `Collections.shuffle` del constructor, `secondMaxVictoryPoints` se inicializa con 10 puntos. Al finalizar el bucle, se evalúa la condición estricta `maxVictoryPoints.getVictoryPoints() > secondMaxVictoryPoints.getVictoryPoints()` (`10 > 10`), la cual evalúa a `false`. Como resultado, el método devuelve `null` y el juego nunca detecta la victoria de dicho jugador.

### 🛣️ Issue #9: `Board.findLongestRoad` lanza `IndexOutOfBoundsException` con cualquier carretera

- **Herramienta que lo detectó:** Tests unitarios (`BoardTest.LongestRoadTests`)
- **Clase afectada:** `board.Board.findLongestRoad` (líneas 503 y 506)
- **Descripción del problema:** Los dos bucles internos usan `i <= connectedRoads.size()` y `k <= adjacentRoads.size()` en lugar de `<`. En la última iteración se llama a `get(size())`, que siempre está fuera de rango. Con una sola carretera de orientación 0 el método falla con `IndexOutOfBoundsException: Index 0 out of bounds for length 0`. El cálculo del camino más largo no puede funcionar nunca.

### 🧭 Issue #10: `Board.findAdjacentRoads` ignora la orientación de la carretera

- **Herramienta que lo detectó:** Tests unitarios (`BoardTest.LongestRoadTests`)
- **Clase afectada:** `board.Board.findAdjacentRoads` (línea 570)
- **Descripción del problema:** El método obtiene la carretera con `roads[x][y][0]`, con la orientación fija a 0, en lugar de usar `loc.getOrientation()`. Si el jugador solo tiene carreteras de orientación 1 o 2, la carretera obtenida no tiene dueño y `p.equals(...)` lanza `NullPointerException`.

### 0️⃣ Issue #11: `Board.findLongestRoad` devuelve 1 para un jugador sin carreteras

- **Herramienta que lo detectó:** Tests unitarios (`BoardTest.longest_noRoads_isZero`, valor frontera)
- **Clase afectada:** `board.Board.findLongestRoad` (línea 497)
- **Descripción del problema:** `maxCount` se inicializa a 1 y, si la lista de carreteras está vacía, no se modifica. Un jugador sin carreteras tiene un camino más largo de longitud 1 en lugar de 0.

### 🌊 Issue #12: `Board.distributeResources(0)` lanza `NullPointerException`

- **Herramienta que lo detectó:** Tests unitarios (`BoardTest.distribute_roll0_doesNotThrow`, valor frontera)
- **Clase afectada:** `board.Board.distributeResources` (línea 150)
- **Descripción del problema:** Las casillas de mar se crean con número 0 y tipo `null`. Con una tirada de 0, `getTilesWithNumber` las devuelve y `t.getType().equals("DESERT")` lanza `NullPointerException`. El método no valida que la tirada esté en el rango 2–12. Además, la comprobación del desierto solo se alcanza con tiradas no válidas, porque el desierto siempre tiene número 0.

### 🏝️ Issue #13: `Board.placeStructureNoRoad` permite construir en el mar

- **Herramienta que lo detectó:** Tests unitarios (`BoardTest.noRoad_seaVertex_returnsFalse`, partición inválida)
- **Clase afectada:** `board.Board.placeStructureNoRoad` (línea 224)
- **Descripción del problema:** El método solo comprueba la ocupación y la regla de distancia, no que el vértice toque alguna casilla de tierra. `placeStructureNoRoad(new VertexLocation(0, 4, 0), p)` devuelve `true` aunque `getAdjacentTilesStructure` de ese vértice esté vacío.

### 🦹 Issue #14: `Board.moveRobber` permite mover el ladrón al mar

- **Herramienta que lo detectó:** Tests unitarios (`BoardTest.moveRobber_seaTile_returnsFalse`, partición inválida)
- **Clase afectada:** `board.Board.moveRobber` (línea 409)
- **Descripción del problema:** Solo se comprueba que la casilla destino sea distinta de la actual. `moveRobber(new Location(0, 0))` devuelve `true` y coloca el ladrón en una casilla de mar, donde no bloquea ninguna producción.

### 🚧 Issue #15: `Board.placeRoad` permite continuar una carretera a través de un poblado rival

- **Herramienta que lo detectó:** Tests unitarios (`BoardTest.road_cannotPassThroughRivalSettlement`)
- **Clase afectada:** `board.Board.placeRoad` (línea 317)
- **Descripción del problema:** Según el reglamento de Catan, una carretera no puede prolongarse a través de un vértice ocupado por un poblado o ciudad de otro jugador. `placeRoad` acepta la conexión con cualquier carretera propia adyacente sin comprobar quién ocupa el vértice compartido. Por ejemplo, con una carretera de Alice en `(2,3,1)` y un poblado de Bob en `(3,4,1)`, Alice puede construir en `(3,3,0)`.

## 4. Justificación de la cobertura de `Board`

Con `BoardTest`, `board.Board` alcanza el **100 % de líneas y el 96 % de ramas en todos sus métodos salvo el cálculo del camino más largo**. Lo que queda sin cubrir es:

- **`findLongestRoad`, `findAdjacentRoads`, `findAdjacentRoadsDFS` y `roadConnectsToOther` (unas 160 líneas):** no se pueden cubrir porque `findLongestRoad` lanza una excepción en la primera iteración con cualquier carretera (Issues #9 y #10). El algoritmo de búsqueda en profundidad es inalcanzable mientras no se corrijan esos defectos. Los tests que describen el comportamiento esperado ya están escritos y desactivados.
- **Constructor, `if (numberTile == 18) break;`:** la rama solo se ejecuta si el desierto cae en la última posición de la espiral (la casilla central), lo que ocurre de forma aleatoria con probabilidad 1/19. El constructor no permite inyectar el orden de las casillas, así que no se puede forzar de forma determinista.
- **`checkPort` (10 ramas de condiciones compuestas `&&`):** corresponden en su mayoría a combinaciones con las coordenadas de un puerto y la otra orientación cuyo vértice queda fuera de la matriz, por ejemplo `(5,6,0)`. `placeStructure*` lanza `ArrayIndexOutOfBoundsException` antes de llegar a `checkPort`, y `checkPort` es privado.
