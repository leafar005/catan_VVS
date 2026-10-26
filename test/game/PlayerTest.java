package game;

import board.DevCard;
import board.Road;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

class PlayerTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("TestPlayer", Color.BLUE);
    }

    // =========================================================================
    // 1. CONSTRUCTORES, IDENTIDAD Y ESTADO BÁSICO
    // =========================================================================
    @Nested
    @DisplayName("Tests de Constructores e Identidad")
    class ConstructorsAndIdentityTests {

        @Test
        @DisplayName("Constructor estándar inicializa el estado por defecto correctamente")
        void constructor_standard_shouldInitializeDefaultState() {
            Player p = new Player("Alice", Color.RED);

            assertAll(
                () -> assertEquals("Alice", p.getName()),
                () -> assertEquals(Color.RED, p.getColor()),
                () -> assertEquals(2, p.getVictoryPoints()),
                () -> assertEquals(2, p.getNumbSettlements()),
                () -> assertEquals(2, p.getNumbRoads()),
                () -> assertEquals(0, p.getNumbCities()),
                () -> assertEquals(0, p.getNumbKnights()),
                () -> assertFalse(p.hasLargestArmy()),
                () -> assertEquals(0, p.getTotalResources()),
                () -> assertTrue(p.getRoads().isEmpty()),
                () -> assertEquals(6, p.getPorts().length)
            );
        }

        @Test
        @DisplayName("Constructor estándar acepta parámetros nulos sin lanzar excepción")
        void constructor_nullParameters_shouldAcceptNullValues() {
            Player p = new Player(null, null);

            assertNull(p.getName());
            assertNull(p.getColor());
        }

        @Test
        @DisplayName("Constructor con recursos personalizados asigna cantidades y puntos de victoria")
        void customConstructor_validValues_shouldSetCustomResourcesAndVP() {
            Player p = new Player("Bob", Color.GREEN, 3, 2, 1, 4, 5, 10);

            assertAll(
                () -> assertEquals(3, p.getNumberResourcesType("BRICK")),
                () -> assertEquals(2, p.getNumberResourcesType("WOOL")),
                () -> assertEquals(1, p.getNumberResourcesType("ORE")),
                () -> assertEquals(4, p.getNumberResourcesType("GRAIN")),
                () -> assertEquals(5, p.getNumberResourcesType("LUMBER")),
                () -> assertEquals(10, p.getVictoryPoints())
            );
        }

        @Test
        @DisplayName("Constructor con recursos en valores frontera (0) asigna ceros correctamente")
        void customConstructor_boundaryZeroValues_shouldSetZeroResourcesAndVP() {
            Player p = new Player("Bob", Color.GREEN, 0, 0, 0, 0, 0, 0);

            assertAll(
                () -> assertEquals(0, p.getTotalResources()),
                () -> assertEquals(0, p.getVictoryPoints())
            );
        }

        @Test
        @DisplayName("getName devuelve el nombre asignado al jugador")
        void getName_shouldReturnPlayerName() {
            assertEquals("TestPlayer", player.getName());
        }

        @Test
        @DisplayName("getName devuelve null si el jugador se inicializó con nombre null")
        void getName_nullName_shouldReturnNull() {
            Player p = new Player(null, Color.BLACK);
            assertNull(p.getName());
        }

        @Test
        @DisplayName("getColor devuelve el color asignado al jugador")
        void getColor_shouldReturnPlayerColor() {
            assertEquals(Color.BLUE, player.getColor());
        }

        @Test
        @DisplayName("getColor devuelve null si el jugador se inicializó con color null")
        void getColor_nullColor_shouldReturnNull() {
            Player p = new Player("Player", null);
            assertNull(p.getColor());
        }

        @Test
        @DisplayName("getVictoryPoints devuelve los puntos de victoria iniciales")
        void getVictoryPoints_shouldReturnCurrentVictoryPoints() {
            assertEquals(2, player.getVictoryPoints());
        }

        @Test
        @DisplayName("setVictoryPoints actualiza correctamente a un valor positivo")
        void setVictoryPoints_positiveValue_shouldUpdateVictoryPoints() {
            player.setVictoryPoints(8);
            assertEquals(8, player.getVictoryPoints());
        }

        @Test
        @DisplayName("setVictoryPoints actualiza a valor frontera cero o negativo")
        void setVictoryPoints_zeroOrNegative_shouldUpdateVictoryPoints() {
            player.setVictoryPoints(0);
            assertEquals(0, player.getVictoryPoints());

            player.setVictoryPoints(-1);
            assertEquals(-1, player.getVictoryPoints());
        }

        @Test
        @DisplayName("toString devuelve el nombre del jugador")
        void toString_shouldReturnPlayerName() {
            assertEquals("TestPlayer", player.toString());
        }

        @Test
        @DisplayName("toString devuelve null si el nombre es null")
        void toString_nullName_shouldReturnNull() {
            Player p = new Player(null, Color.YELLOW);
            assertNull(p.toString());
        }
    }

    // =========================================================================
    // 2. CARTAS DE DESARROLLO (DevCards)
    // =========================================================================
    @Nested
    @DisplayName("Tests de Cartas de Desarrollo")
    class DevelopmentCardsTests {

        @Test
        @DisplayName("addDevCard añade una carta normal a la mano sin alterar puntos de victoria")
        void addDevCard_normalCard_shouldAddToHandWithoutIncreasingVP() {
            int initialVP = player.getVictoryPoints();
            DevCard knight = new DevCard("Knight");

            player.addDevCard(knight);

            assertEquals(1, player.getDevCardsType("Knight"));
            assertEquals(initialVP, player.getVictoryPoints());
        }

        @Test
        @DisplayName("addDevCard con carta 'Victory Point' incrementa los puntos de victoria")
        void addDevCard_victoryPointCard_shouldAddToHandAndIncreaseVP() {
            int initialVP = player.getVictoryPoints();
            DevCard vpCard = new DevCard("Victory Point");

            player.addDevCard(vpCard);

            assertEquals(1, player.getDevCardsType("Victory Point"));
            assertEquals(initialVP + 1, player.getVictoryPoints());
        }

        @Test
        @DisplayName("addDevCard añade carta de tipo Progress con subtipo")
        void addDevCard_progressCardWithSubType_shouldAddToHand() {
            DevCard progress = new DevCard("Progress", "Road Building");

            player.addDevCard(progress);

            assertTrue(player.hasCard("Road Building"));
            assertTrue(player.hasCard("Progress"));
        }

        @Test
        @DisplayName("hasCard devuelve false si la mano del jugador está vacía")
        void hasCard_emptyHand_shouldReturnFalse() {
            assertFalse(player.hasCard("Knight"));
        }

        @Test
        @DisplayName("hasCard devuelve true cuando coincide con el tipo principal de la carta")
        void hasCard_cardMatchesByType_shouldReturnTrue() {
            player.addDevCard(new DevCard("Knight"));
            assertTrue(player.hasCard("Knight"));
        }

        @Test
        @DisplayName("hasCard devuelve true cuando coincide con el subtipo de la carta")
        void hasCard_cardMatchesBySubType_shouldReturnTrue() {
            player.addDevCard(new DevCard("Progress", "Monopoly"));
            assertTrue(player.hasCard("Monopoly"));
        }

        @Test
        @DisplayName("hasCard devuelve false cuando la carta solicitada no está en la mano")
        void hasCard_cardNotPresentInHand_shouldReturnFalse() {
            player.addDevCard(new DevCard("Knight"));
            assertFalse(player.hasCard("Monopoly"));
        }

        @Test
        @DisplayName("removeCard elimina una carta existente por su tipo principal")
        void removeCard_cardExistsByType_shouldRemoveCard() {
            player.addDevCard(new DevCard("Knight"));
            player.removeCard("Knight");

            assertFalse(player.hasCard("Knight"));
            assertEquals(0, player.getDevCardsType("Knight"));
        }

        @Test
        @DisplayName("removeCard elimina una carta existente por su subtipo")
        void removeCard_cardExistsBySubType_shouldRemoveCard() {
            player.addDevCard(new DevCard("Progress", "Year of Plenty"));
            player.removeCard("Year of Plenty");

            assertFalse(player.hasCard("Year of Plenty"));
            assertEquals(0, player.getDevCardsType("Year of Plenty"));
        }

        @Test
        @DisplayName("removeCard no modifica la mano ni lanza excepción si la carta no existe")
        void removeCard_cardDoesNotExist_shouldNotModifyHandOrThrowException() {
            player.addDevCard(new DevCard("Knight"));

            assertDoesNotThrow(() -> player.removeCard("Monopoly"));
            assertEquals(1, player.getDevCardsType("Knight"));
        }

        @Test
        @DisplayName("removeCard elimina solo la primera instancia cuando hay duplicados")
        void removeCard_multipleCardsSameType_shouldRemoveOnlyFirstCard() {
            player.addDevCard(new DevCard("Knight"));
            player.addDevCard(new DevCard("Knight"));

            player.removeCard("Knight");

            assertEquals(1, player.getDevCardsType("Knight"));
        }

        @Test
        @DisplayName("getDevCardsType devuelve 0 si la mano está vacía")
        void getDevCardsType_emptyHand_shouldReturnZero() {
            assertEquals(0, player.getDevCardsType("Knight"));
        }

        @Test
        @DisplayName("getDevCardsType devuelve la cantidad correcta al coincidir por tipo")
        void getDevCardsType_matchingType_shouldReturnCorrectCount() {
            player.addDevCard(new DevCard("Knight"));
            player.addDevCard(new DevCard("Knight"));
            player.addDevCard(new DevCard("Victory Point"));

            assertEquals(2, player.getDevCardsType("Knight"));
        }

        @Test
        @DisplayName("getDevCardsType devuelve la cantidad correcta al coincidir por subtipo")
        void getDevCardsType_matchingSubType_shouldReturnCorrectCount() {
            player.addDevCard(new DevCard("Progress", "Road Building"));
            player.addDevCard(new DevCard("Progress", "Road Building"));
            player.addDevCard(new DevCard("Progress", "Monopoly"));

            assertEquals(2, player.getDevCardsType("Road Building"));
        }

        @Test
        @DisplayName("getDevCardsType con subtipo nulo y tipo no coincidente no incrementa el contador")
        void getDevCardsType_subTypeNullAndNoTypeMatch_shouldNotCount() {
            player.addDevCard(new DevCard("Knight")); // subType is null

            assertEquals(0, player.getDevCardsType("Progress"));
        }

        @Test
        @DisplayName("getDevCardsType con subtipo no nulo pero diferente no incrementa el contador")
        void getDevCardsType_subTypeNonNullAndNoMatch_shouldNotCount() {
            player.addDevCard(new DevCard("Progress", "Monopoly"));

            assertEquals(0, player.getDevCardsType("Road Building"));
        }
    }

    // =========================================================================
    // 3. GESTIÓN DE RECURSOS
    // =========================================================================
    @Nested
    @DisplayName("Tests de Gestión de Recursos")
    class ResourceManagementTests {

        @Test
        @DisplayName("getNumberResourcesType devuelve la cantidad de un recurso válido")
        void getNumberResourcesType_validResource_shouldReturnCurrentAmount() {
            player.setNumberResourcesType("BRICK", 4);
            assertEquals(4, player.getNumberResourcesType("BRICK"));
        }

        @Test
        @DisplayName("getNumberResourcesType devuelve 0 para 'DESERT'")
        void getNumberResourcesType_desert_shouldReturnZero() {
            assertEquals(0, player.getNumberResourcesType("DESERT"));
        }

        @Test
        @DisplayName("getNumberResourcesType devuelve 0 para recurso null")
        void getNumberResourcesType_null_shouldReturnZero() {
            assertEquals(0, player.getNumberResourcesType(null));
        }

        @Test
        @DisplayName("getNumberResourcesType lanza NullPointerException para un recurso desconocido no mapeado")
        void getNumberResourcesType_unknownResourceNotInMap_shouldThrowNullPointerException() {
            assertThrows(NullPointerException.class, () -> player.getNumberResourcesType("GOLD"));
        }

        @Test
        @DisplayName("setNumberResourcesType actualiza la cantidad de un recurso válido a un valor positivo")
        void setNumberResourcesType_positiveValue_shouldUpdateResource() {
            player.setNumberResourcesType("ORE", 5);
            assertEquals(5, player.getNumberResourcesType("ORE"));
        }

        @Test
        @DisplayName("setNumberResourcesType permite valores frontera cero y negativos")
        void setNumberResourcesType_zeroAndNegativeValue_shouldUpdateResource() {
            player.setNumberResourcesType("LUMBER", 0);
            assertEquals(0, player.getNumberResourcesType("LUMBER"));

            player.setNumberResourcesType("LUMBER", -2);
            assertEquals(-2, player.getNumberResourcesType("LUMBER"));
        }

        @Test
        @DisplayName("giveResourceType incrementa en 1 la cantidad de un recurso válido")
        void giveResourceType_validResource_shouldIncrementCount() {
            player.giveResourceType("BRICK");
            assertEquals(1, player.getNumberResourcesType("BRICK"));
        }

        @Test
        @DisplayName("giveResourceType incrementa adecuadamente cada uno de los 5 recursos estándar")
        void giveResourceType_allStandardResources_shouldIncrementRespectiveCounts() {
            player.giveResourceType("BRICK");
            player.giveResourceType("WOOL");
            player.giveResourceType("ORE");
            player.giveResourceType("GRAIN");
            player.giveResourceType("LUMBER");

            assertAll(
                () -> assertEquals(1, player.getNumberResourcesType("BRICK")),
                () -> assertEquals(1, player.getNumberResourcesType("WOOL")),
                () -> assertEquals(1, player.getNumberResourcesType("ORE")),
                () -> assertEquals(1, player.getNumberResourcesType("GRAIN")),
                () -> assertEquals(1, player.getNumberResourcesType("LUMBER"))
            );
        }

        @Test
        @DisplayName("giveResourceType con 'DESERT' no altera ningún recurso ni lanza excepción")
        void giveResourceType_desert_shouldIgnoreAndNotChangeResources() {
            player.giveResourceType("DESERT");
            assertEquals(0, player.getTotalResources());
        }

        @Test
        @DisplayName("giveResourceType con null no altera ningún recurso ni lanza excepción")
        void giveResourceType_null_shouldIgnoreAndNotChangeResources() {
            player.giveResourceType(null);
            assertEquals(0, player.getTotalResources());
        }

        @Test
        @DisplayName("giveResourceType con recurso desconocido no mapeado lanza NullPointerException")
        void giveResourceType_unknownResourceNotInMap_shouldThrowNullPointerException() {
            assertThrows(NullPointerException.class, () -> player.giveResourceType("UNKNOWN_RESOURCE"));
        }

        @Test
        @DisplayName("removeResources decrementa los recursos disponibles del jugador")
        void removeResources_availableResources_shouldDecreaseQuantities() {
            player.setNumberResourcesType("BRICK", 2);
            player.setNumberResourcesType("WOOL", 1);

            ArrayList<String> toRemove = new ArrayList<>(Arrays.asList("BRICK", "WOOL"));
            player.removeResources(toRemove);

            assertEquals(1, player.getNumberResourcesType("BRICK"));
            assertEquals(0, player.getNumberResourcesType("WOOL"));
        }

        @Test
        @DisplayName("removeResources sobre recurso con cantidad 0 decrementa a valor negativo según implementación real")
        void removeResources_zeroQuantityResource_shouldAllowNegativeValue() {
            player.setNumberResourcesType("ORE", 0);

            ArrayList<String> toRemove = new ArrayList<>(Arrays.asList("ORE"));
            player.removeResources(toRemove);

            assertEquals(-1, player.getNumberResourcesType("ORE"));
        }

        @Test
        @DisplayName("removeResources con lista vacía no modifica ningún recurso")
        void removeResources_emptyList_shouldNotModifyAnyResource() {
            player.setNumberResourcesType("GRAIN", 3);

            player.removeResources(new ArrayList<>());

            assertEquals(3, player.getNumberResourcesType("GRAIN"));
        }

        @Test
        @DisplayName("addResources añade múltiples recursos incrementando cada tipo")
        void addResources_multipleResources_shouldIncrementEachResource() {
            ArrayList<String> toAdd = new ArrayList<>(Arrays.asList("BRICK", "BRICK", "LUMBER"));
            player.addResources(toAdd);

            assertEquals(2, player.getNumberResourcesType("BRICK"));
            assertEquals(1, player.getNumberResourcesType("LUMBER"));
        }

        @Test
        @DisplayName("addResources con lista vacía no altera ningún recurso")
        void addResources_emptyList_shouldNotChangeResources() {
            player.addResources(new ArrayList<>());
            assertEquals(0, player.getTotalResources());
        }

        @Test
        @DisplayName("getTotalResources devuelve 0 para un jugador recién creado")
        void getTotalResources_initialPlayer_shouldReturnZero() {
            assertEquals(0, player.getTotalResources());
        }

        @Test
        @DisplayName("getTotalResources devuelve la suma exacta de todos los tipos de recursos")
        void getTotalResources_variousResources_shouldReturnSum() {
            player.setNumberResourcesType("BRICK", 2);
            player.setNumberResourcesType("WOOL", 1);
            player.setNumberResourcesType("ORE", 3);
            player.setNumberResourcesType("GRAIN", 4);
            player.setNumberResourcesType("LUMBER", 5);

            assertEquals(15, player.getTotalResources());
        }

        @Test
        @DisplayName("getOwnedResources devuelve lista vacía si todos los recursos tienen cantidad 0")
        void getOwnedResources_allZero_shouldReturnEmptyList() {
            assertTrue(player.getOwnedResources().isEmpty());
        }

        @Test
        @DisplayName("getOwnedResources devuelve los 5 recursos en orden cuando todos tienen cantidad mayor a 0")
        void getOwnedResources_allPositive_shouldReturnAllFiveResourcesInOrder() {
            player.setNumberResourcesType("BRICK", 1);
            player.setNumberResourcesType("GRAIN", 1);
            player.setNumberResourcesType("WOOL", 1);
            player.setNumberResourcesType("LUMBER", 1);
            player.setNumberResourcesType("ORE", 1);

            ArrayList<String> expected = new ArrayList<>(Arrays.asList("BRICK", "GRAIN", "WOOL", "LUMBER", "ORE"));
            assertEquals(expected, player.getOwnedResources());
        }

        @Test
        @DisplayName("getOwnedResources omite recursos con cantidad cero o negativa")
        void getOwnedResources_someZeroAndNegative_shouldOnlyIncludeStrictlyPositiveResources() {
            player.setNumberResourcesType("BRICK", 2);
            player.setNumberResourcesType("GRAIN", 0);
            player.setNumberResourcesType("WOOL", -1);
            player.setNumberResourcesType("LUMBER", 4);
            player.setNumberResourcesType("ORE", 0);

            ArrayList<String> owned = player.getOwnedResources();
            assertAll(
                () -> assertEquals(2, owned.size()),
                () -> assertTrue(owned.contains("BRICK")),
                () -> assertTrue(owned.contains("LUMBER")),
                () -> assertFalse(owned.contains("GRAIN")),
                () -> assertFalse(owned.contains("WOOL")),
                () -> assertFalse(owned.contains("ORE"))
            );
        }

        // Tests conservados de la implementación inicial
        @Test
        @DisplayName("hasResources devuelve true si el jugador tiene los recursos EXACTOS solicitados (Frontera)")
        void hasResources_ExactResources_ReturnsTrue() {
            player.setNumberResourcesType("ORE", 1);
            player.setNumberResourcesType("WOOL", 2);

            ArrayList<String> cost = new ArrayList<>(Arrays.asList("ORE", "WOOL", "WOOL"));

            assertTrue(player.hasResources(cost));
        }

        @Test
        @DisplayName("hasResources devuelve false si al jugador le falta un recurso de la lista")
        void hasResources_MissingOneResource_ReturnsFalse() {
            player.setNumberResourcesType("ORE", 1);
            player.setNumberResourcesType("WOOL", 1);

            ArrayList<String> cost = new ArrayList<>(Arrays.asList("ORE", "WOOL", "WOOL"));

            assertFalse(player.hasResources(cost));
        }

        // Cobertura específica de ramas de corto-circuito en hasResources
        @Test
        @DisplayName("hasResources devuelve false cuando falta WOOL")
        void hasResources_missingWool_shouldReturnFalse() {
            ArrayList<String> cost = new ArrayList<>(Arrays.asList("WOOL"));
            assertFalse(player.hasResources(cost));
        }

        @Test
        @DisplayName("hasResources devuelve false cuando falta ORE")
        void hasResources_missingOre_shouldReturnFalse() {
            ArrayList<String> cost = new ArrayList<>(Arrays.asList("ORE"));
            assertFalse(player.hasResources(cost));
        }

        @Test
        @DisplayName("hasResources devuelve false cuando falta LUMBER")
        void hasResources_missingLumber_shouldReturnFalse() {
            ArrayList<String> cost = new ArrayList<>(Arrays.asList("LUMBER"));
            assertFalse(player.hasResources(cost));
        }

        @Test
        @DisplayName("hasResources devuelve false cuando falta BRICK")
        void hasResources_missingBrick_shouldReturnFalse() {
            ArrayList<String> cost = new ArrayList<>(Arrays.asList("BRICK"));
            assertFalse(player.hasResources(cost));
        }

        @Test
        @DisplayName("hasResources devuelve false cuando falta GRAIN")
        void hasResources_missingGrain_shouldReturnFalse() {
            ArrayList<String> cost = new ArrayList<>(Arrays.asList("GRAIN"));
            assertFalse(player.hasResources(cost));
        }

        @Test
        @DisplayName("hasResources devuelve true para una lista de coste vacía")
        void hasResources_emptyList_shouldReturnTrue() {
            assertTrue(player.hasResources(new ArrayList<>()));
        }

        @Test
        @DisplayName("hasResources devuelve true cuando el jugador tiene más recursos que los solicitados")
        void hasResources_playerHasMoreThanRequested_shouldReturnTrue() {
            player.setNumberResourcesType("BRICK", 5);
            player.setNumberResourcesType("GRAIN", 5);

            ArrayList<String> cost = new ArrayList<>(Arrays.asList("BRICK", "GRAIN"));
            assertTrue(player.hasResources(cost));
        }

        @Test
        @DisplayName("hasResources ignora cadenas que no corresponden a recursos estándar en la lista")
        void hasResources_listContainsNonStandardResource_shouldIgnoreUnknownString() {
            ArrayList<String> cost = new ArrayList<>(Arrays.asList("DESERT", "GOLD"));
            assertTrue(player.hasResources(cost));
        }
    }

    // =========================================================================
    // 4. CONSTRUCCIONES Y CAMINOS
    // =========================================================================
    @Nested
    @DisplayName("Tests de Construcciones y Caminos")
    class BuildingsAndRoadsTests {

        @Test
        @DisplayName("getNumbSettlements devuelve el valor inicial de 2")
        void getNumbSettlements_initialValue_shouldBeTwo() {
            assertEquals(2, player.getNumbSettlements());
        }

        @Test
        @DisplayName("addSettlement incrementa el número de asentamientos en 1")
        void addSettlement_shouldIncreaseSettlementCount() {
            player.addSettlement();
            assertEquals(3, player.getNumbSettlements());
        }

        @Test
        @DisplayName("addSettlement llamadas múltiples incrementan secuencialmente")
        void addSettlement_multipleCalls_shouldIncrementSequentially() {
            player.addSettlement();
            player.addSettlement();
            assertEquals(4, player.getNumbSettlements());
        }

        @Test
        @DisplayName("getNumbCities devuelve el valor inicial de 0")
        void getNumbCities_initialValue_shouldBeZero() {
            assertEquals(0, player.getNumbCities());
        }

        @Test
        @DisplayName("upCity reduce un asentamiento e incrementa una ciudad")
        void upCity_shouldDecreaseSettlementsAndIncreaseCities() {
            player.upCity();

            assertAll(
                () -> assertEquals(1, player.getNumbSettlements()),
                () -> assertEquals(1, player.getNumbCities())
            );
        }

        @Test
        @DisplayName("upCity múltiples veces reduce asentamientos a 0 y aumenta ciudades a 2")
        void upCity_multipleUpgrades_shouldDecreaseSettlementsToZeroAndIncreaseCities() {
            player.upCity();
            player.upCity();

            assertAll(
                () -> assertEquals(0, player.getNumbSettlements()),
                () -> assertEquals(2, player.getNumbCities())
            );
        }

        @Test
        @DisplayName("getNumbRoads devuelve el valor inicial de 2")
        void getNumbRoads_initialValue_shouldBeTwo() {
            assertEquals(2, player.getNumbRoads());
        }

        @Test
        @DisplayName("addRoadCount incrementa el número de caminos en 1")
        void addRoadCount_shouldIncreaseRoadCount() {
            player.addRoadCount();
            assertEquals(3, player.getNumbRoads());
        }

        @Test
        @DisplayName("addRoadCount llamadas consecutivas incrementan secuencialmente")
        void addRoadCount_multipleCalls_shouldIncrementRoadCount() {
            player.addRoadCount();
            player.addRoadCount();
            assertEquals(4, player.getNumbRoads());
        }

        @Test
        @DisplayName("getRoads devuelve lista vacía inicialmente")
        void getRoads_initiallyEmpty_shouldReturnEmptyList() {
            assertTrue(player.getRoads().isEmpty());
        }

        @Test
        @DisplayName("addRoad añade una carretera a la lista del jugador")
        void addRoad_singleRoad_shouldStoreRoad() {
            Road road = new Road(0, 0, 0);
            player.addRoad(road);

            assertEquals(1, player.getRoads().size());
            assertSame(road, player.getRoads().get(0));
        }

        @Test
        @DisplayName("addRoad añade múltiples carreteras preservando el orden")
        void addRoad_multipleRoads_shouldStoreAllRoadsInOrder() {
            Road road1 = new Road(0, 0, 0);
            Road road2 = new Road(1, 1, 1);

            player.addRoad(road1);
            player.addRoad(road2);

            assertEquals(2, player.getRoads().size());
            assertSame(road1, player.getRoads().get(0));
            assertSame(road2, player.getRoads().get(1));
        }
    }

    // =========================================================================
    // 5. CABALLEROS Y GRAN EJÉRCITO (Largest Army)
    // =========================================================================
    @Nested
    @DisplayName("Tests de Caballeros y Gran Ejército")
    class KnightsAndLargestArmyTests {

        @Test
        @DisplayName("getNumbKnights devuelve 0 inicialmente")
        void getNumbKnights_initiallyZero_shouldReturnZero() {
            assertEquals(0, player.getNumbKnights());
        }

        @Test
        @DisplayName("incrementNumbKnights incrementa el número de caballeros en 1")
        void incrementNumbKnights_singleCall_shouldIncrementCount() {
            player.incrementNumbKnights();
            assertEquals(1, player.getNumbKnights());
        }

        @Test
        @DisplayName("incrementNumbKnights llamadas consecutivas incrementan secuencialmente")
        void incrementNumbKnights_multipleCalls_shouldIncrementSequentially() {
            player.incrementNumbKnights();
            player.incrementNumbKnights();
            player.incrementNumbKnights();
            assertEquals(3, player.getNumbKnights());
        }

        @Test
        @DisplayName("hasLargestArmy devuelve false inicialmente")
        void hasLargestArmy_initialValue_shouldBeFalse() {
            assertFalse(player.hasLargestArmy());
        }

        // Tests conservados de la implementación inicial
        @Test
        @DisplayName("Dar Largest Army a quien no lo tiene incrementa Victory Points (false -> true)")
        void setHasLargestArmy_FalseToTrue_IncreasesVP() {
            int initialVp = player.getVictoryPoints();

            player.setHasLargestArmy(true);

            assertEquals(initialVp + 1, player.getVictoryPoints());
            assertTrue(player.hasLargestArmy());
        }

        @Test
        @DisplayName("Dar Largest Army a quien ya lo tiene mantiene Victory Points intactos (true -> true)")
        void setHasLargestArmy_TrueToTrue_KeepsVP() {
            player.setHasLargestArmy(true);
            int vpAfterFirstTime = player.getVictoryPoints();

            player.setHasLargestArmy(true);

            assertEquals(vpAfterFirstTime, player.getVictoryPoints());
            assertTrue(player.hasLargestArmy());
        }

        @Test
        @DisplayName("Quitar Largest Army a quien lo tiene decrementa Victory Points (true -> false)")
        void setHasLargestArmy_TrueToFalse_DecreasesVP() {
            player.setHasLargestArmy(true);
            int vpWithArmy = player.getVictoryPoints();

            player.setHasLargestArmy(false);

            assertEquals(vpWithArmy - 1, player.getVictoryPoints());
            assertFalse(player.hasLargestArmy());
        }

        @Test
        @DisplayName("Quitar Largest Army a quien no lo tiene mantiene Victory Points intactos (false -> false)")
        void setHasLargestArmy_FalseToFalse_KeepsVP() {
            int initialVp = player.getVictoryPoints();

            player.setHasLargestArmy(false);

            assertEquals(initialVp, player.getVictoryPoints());
            assertFalse(player.hasLargestArmy());
        }
    }

    // =========================================================================
    // 6. PUERTOS
    // =========================================================================
    @Nested
    @DisplayName("Tests de Puertos")
    class PortsManagementTests {

        @Test
        @DisplayName("getPorts devuelve un array de 6 posiciones inicialmente todas en false")
        void getPorts_initiallyAllFalse_shouldReturnAllFalseArray() {
            boolean[] ports = player.getPorts();

            assertEquals(6, ports.length);
            for (boolean port : ports) {
                assertFalse(port);
            }
        }

        @Test
        @DisplayName("addPort activa el puerto correspondiente para etiquetas válidas")
        void addPort_validTags_shouldRegisterEachPort() {
            player.addPort(0); // general
            player.addPort(5); // lumber

            boolean[] ports = player.getPorts();
            assertAll(
                () -> assertTrue(ports[0]),
                () -> assertFalse(ports[1]),
                () -> assertFalse(ports[2]),
                () -> assertFalse(ports[3]),
                () -> assertFalse(ports[4]),
                () -> assertTrue(ports[5])
            );
        }

        @Test
        @DisplayName("addPort permite registrar todos los puertos del 0 al 5")
        void addPort_allTags_shouldSetAllPortsToTrue() {
            for (int i = 0; i <= 5; i++) {
                player.addPort(i);
            }

            for (boolean port : player.getPorts()) {
                assertTrue(port);
            }
        }

        @Test
        @DisplayName("addPort con índice negativo lanza ArrayIndexOutOfBoundsException")
        void addPort_invalidNegativeTag_shouldThrowIndexOutOfBoundsException() {
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> player.addPort(-1));
        }

        @Test
        @DisplayName("addPort con índice superior a 5 lanza ArrayIndexOutOfBoundsException")
        void addPort_invalidOverflowTag_shouldThrowIndexOutOfBoundsException() {
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> player.addPort(6));
        }
    }

    // =========================================================================
    // 7. PRUEBAS BASADAS EN PROPIEDADES CON JQWIK
    // =========================================================================
    @Property
    @Label("Añadir un número aleatorio de BRICK actualiza el contador total correctamente")
    void addResources_RandomAmount_UpdatesTotal(@ForAll @IntRange(min = 1, max = 50) int randomAmount) {
        Player p = new Player("PropPlayer", Color.BLUE);
        int initialTotal = p.getTotalResources();

        ArrayList<String> randomResources = new ArrayList<>();
        for (int i = 0; i < randomAmount; i++) {
            randomResources.add("BRICK");
        }

        p.addResources(randomResources);

        assertEquals(initialTotal + randomAmount, p.getTotalResources());
    }

    @Property
    @Label("Asignar cualquier valor aleatorio de Victory Points actualiza el estado fielmente")
    void setVictoryPoints_randomValue_shouldMatch(@ForAll @IntRange(min = 0, max = 100) int randomVp) {
        Player p = new Player("PropPlayer", Color.RED);
        p.setVictoryPoints(randomVp);
        assertEquals(randomVp, p.getVictoryPoints());
    }
}
