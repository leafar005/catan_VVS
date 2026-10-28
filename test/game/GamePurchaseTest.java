package game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import runner.GameRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("GamePurchaseTest - Pruebas de deducción exacta de recursos en compras de Game")
public class GamePurchaseTest {

    private Player player;
    private Game game;

    @BeforeEach
    void setUp() throws Exception {
        player = new Player("Xabi", null);
        Player dummy2 = new Player("Pedro", null);
        Player dummy3 = new Player("Santi", null);

        ArrayList<Player> players = new ArrayList<>();
        players.add(player);
        players.add(dummy2);
        players.add(dummy3);

        Field grPlayersField = GameRunner.class.getDeclaredField("players");
        grPlayersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Player> grPlayers = (ArrayList<Player>) grPlayersField.get(null);
        grPlayers.clear();
        grPlayers.addAll(players);

        game = new Game(players);
    }

    @Test
    @DisplayName("buyRoad descuenta exactamente 1 BRICK y 1 LUMBER de los recursos del jugador")
    void buyRoad_deductsExactResources() {
        player.setNumberResourcesType("BRICK", 3);
        player.setNumberResourcesType("LUMBER", 2);

        int code = game.buyRoad(player);

        assertEquals(0, code, "La compra de carretera debe ser exitosa");
        assertEquals(2, player.getNumberResourcesType("BRICK"), "Debe quedar exactamente 2 BRICK (3 - 1)");
        assertEquals(1, player.getNumberResourcesType("LUMBER"), "Debe quedar exactamente 1 LUMBER (2 - 1)");
    }

    @Test
    @DisplayName("buySettlement descuenta exactamente 1 de cada recurso (BRICK, LUMBER, GRAIN, WOOL)")
    void buySettlement_deductsExactResources() {
        player.setNumberResourcesType("BRICK", 2);
        player.setNumberResourcesType("LUMBER", 3);
        player.setNumberResourcesType("GRAIN", 4);
        player.setNumberResourcesType("WOOL", 5);

        int code = game.buySettlement(player);

        assertEquals(0, code, "La compra de poblado debe ser exitosa");
        assertEquals(1, player.getNumberResourcesType("BRICK"), "Debe quedar 1 BRICK");
        assertEquals(2, player.getNumberResourcesType("LUMBER"), "Debe quedar 2 LUMBER");
        assertEquals(3, player.getNumberResourcesType("GRAIN"), "Debe quedar 3 GRAIN");
        assertEquals(4, player.getNumberResourcesType("WOOL"), "Debe quedar 4 WOOL");
    }

    @Test
    @DisplayName("buyCity descuenta exactamente 2 GRAIN y 3 ORE de los recursos del jugador")
    void buyCity_deductsExactResources() {
        player.setNumberResourcesType("GRAIN", 5);
        player.setNumberResourcesType("ORE", 4);

        int code = game.buyCity(player);

        assertEquals(0, code, "La compra de ciudad debe ser exitosa");
        assertEquals(3, player.getNumberResourcesType("GRAIN"), "Debe quedar exactamente 3 GRAIN (5 - 2)");
        assertEquals(1, player.getNumberResourcesType("ORE"), "Debe quedar exactamente 1 ORE (4 - 3)");
    }

    @Test
    @DisplayName("buyRoad rechaza la compra con código 2 cuando se alcanza el límite de 15 carreteras")
    void buyRoad_capacityLimitReached() {
        player.setNumberResourcesType("BRICK", 2);
        player.setNumberResourcesType("LUMBER", 2);
        for (int i = 0; i < 15; i++) {
            player.addRoadCount();
        }

        int code = game.buyRoad(player);

        assertEquals(2, code, "Debe rechazar la compra por superar el límite de 15 carreteras");
        assertEquals(2, player.getNumberResourcesType("BRICK"), "No debe descontar recursos si la compra fue rechazada");
    }

    @Test
    @DisplayName("buySettlement rechaza la compra con código 2 cuando se alcanza el límite de 5 poblados")
    void buySettlement_capacityLimitReached() {
        player.setNumberResourcesType("BRICK", 2);
        player.setNumberResourcesType("LUMBER", 2);
        player.setNumberResourcesType("GRAIN", 2);
        player.setNumberResourcesType("WOOL", 2);
        for (int i = 0; i < 5; i++) {
            player.addSettlement();
        }

        int code = game.buySettlement(player);

        assertEquals(2, code, "Debe rechazar la compra por superar el límite de 5 poblados");
        assertEquals(2, player.getNumberResourcesType("BRICK"), "No debe descontar recursos si la compra fue rechazada");
    }

    @Test
    @DisplayName("buyCity rechaza la compra con código 2 cuando se alcanza el límite de 4 ciudades")
    void buyCity_capacityLimitReached() {
        player.setNumberResourcesType("GRAIN", 4);
        player.setNumberResourcesType("ORE", 4);
        for (int i = 0; i < 4; i++) {
            player.upCity();
        }

        int code = game.buyCity(player);

        assertEquals(2, code, "Debe rechazar la compra por superar el límite de 4 ciudades");
        assertEquals(4, player.getNumberResourcesType("GRAIN"), "No debe descontar recursos si la compra fue rechazada");
    }
}
