package board;

import game.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StructureUnitTest - Pruebas unitarias de City, Settlement y Road")
public class StructureUnitTest {

    @Test
    @DisplayName("City inicializa su VertexLocation y su type = 1 en el constructor")
    void cityConstructor_setsLocationAndType() {
        VertexLocation loc = new VertexLocation(2, 3, 0);
        City city = new City(loc);

        assertEquals(loc, city.getLocation(), "City debe almacenar la referencia de VertexLocation");
        assertEquals(1, city.getType(), "City debe inicializarse con type = 1");
    }

    @Test
    @DisplayName("City.giveResources otorga exactamente dos recursos a su propietario")
    void cityGiveResources_incrementsOwnerResourcesByTwo() {
        Player player = new Player("Alice", null);
        City city = new City(new VertexLocation(1, 1, 0));
        city.setOwner(player);

        city.giveResources("GRAIN");
        assertEquals(2, player.getNumberResourcesType("GRAIN"), "City debe incrementar en 2 el recurso producido");
    }

    @Test
    @DisplayName("Settlement inicializa su ubicación y su type = 0 en el constructor")
    void settlementConstructor_setsLocationAndType() {
        Settlement settlement = new Settlement(3, 4, 1);

        assertNotNull(settlement.getLocation(), "Settlement debe instanciar un VertexLocation");
        assertEquals(3, settlement.getLocation().getXCoord());
        assertEquals(4, settlement.getLocation().getYCoord());
        assertEquals(1, settlement.getLocation().getOrientation());
        assertEquals(0, settlement.getType(), "Settlement debe inicializarse con type = 0");
    }

    @Test
    @DisplayName("Settlement.giveResources otorga exactamente un recurso a su propietario")
    void settlementGiveResources_incrementsOwnerResourcesByOne() {
        Player player = new Player("Bob", null);
        Settlement settlement = new Settlement(2, 2, 0);
        settlement.setOwner(player);

        settlement.giveResources("BRICK");
        assertEquals(1, player.getNumberResourcesType("BRICK"), "Settlement debe incrementar en 1 el recurso producido");
    }

    @Test
    @DisplayName("Road inicializa su EdgeLocation y no tiene propietario inicial")
    void roadConstructor_setsLocationAndNullOwner() {
        Road road = new Road(1, 2, 0);

        assertNotNull(road.getLocation(), "Road debe instanciar su EdgeLocation");
        assertEquals(1, road.getLocation().getXCoord());
        assertEquals(2, road.getLocation().getYCoord());
        assertEquals(0, road.getLocation().getOrientation());
        assertNull(road.getOwner(), "Una carretera recién creada no debe tener dueño");
        assertFalse(road.isVisited(), "Una carretera recién creada no debe estar marcada como visitada");
    }

    @Test
    @DisplayName("El ciclo de vida de visitas de Road alterna fielmente el estado de isVisited")
    void roadVisit_togglesVisitedState() {
        Road road = new Road(3, 3, 1);

        assertFalse(road.isVisited(), "Estado inicial debe ser false");
        road.visit();
        assertTrue(road.isVisited(), "Tras visit(), isVisited debe ser true");
        road.resetVisited();
        assertFalse(road.isVisited(), "Tras resetVisited(), isVisited debe ser false");
    }

    @Test
    @DisplayName("Board.placeRoad asigna el propietario en carreteras de orientación 1 y 2")
    void boardPlaceRoad_setsOwnerForOrientationsOneAndTwo() {
        Board board = new Board();
        Player player = new Player("Charlie", null);

        // Orientación 1: vértice inicial (2, 2, 0)
        board.getStructures()[2][2][0].setOwner(player);
        boolean placedO1 = board.placeRoad(new EdgeLocation(2, 2, 1), player);
        assertTrue(placedO1, "Debe colocarse la carretera O1 conectada al poblado en (2,2,0)");
        assertEquals(player, board.getRoad(new EdgeLocation(2, 2, 1)).getOwner(), "La carretera O1 debe tener asignado el propietario");

        // Orientación 2: vértice adyacente (3, 3, 1)
        board.getStructures()[3][3][1].setOwner(player);
        boolean placedO2 = board.placeRoad(new EdgeLocation(2, 2, 2), player);
        assertTrue(placedO2, "Debe colocarse la carretera O2 conectada al poblado en (3,3,1)");
        assertEquals(player, board.getRoad(new EdgeLocation(2, 2, 2)).getOwner(), "La carretera O2 debe tener asignado el propietario");
    }
}
