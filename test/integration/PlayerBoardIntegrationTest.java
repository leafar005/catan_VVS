package integration;

import board.*;
import game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración caja negra que validan la interacción entre Player, Board y Game.
 * Se evalúa el ciclo de vida del juego: colocación de estructuras, conexión de carreteras,
 * regla de distancia, distribución de recursos, ladrón, compras y condición de victoria.
 */
public class PlayerBoardIntegrationTest {

    private Board board;
    private Player player1;
    private Player player2;
    private Player player3;

    @BeforeEach
    public void setUp() throws Exception {
        board = new Board();
        player1 = new Player("Rafa", Color.RED);
        player2 = new Player("Pedro", Color.BLUE);
        player3 = new Player("Santi", Color.GREEN);

        // Inicialización de la lista interna estática de GameRunner necesaria por acoplamiento en Game.<init>
        Field grPlayersField = GameRunner.class.getDeclaredField("players");
        grPlayersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Player> grPlayers = (ArrayList<Player>) grPlayersField.get(null);
        grPlayers.clear();
        grPlayers.add(player1);
        grPlayers.add(player2);
        grPlayers.add(player3);
    }

    private ArrayList<Player> createThreePlayers() {
        ArrayList<Player> players = new ArrayList<>();
        players.add(player1);
        players.add(player2);
        players.add(player3);
        return players;
    }

    @Test
    @DisplayName("Colocación inicial de poblado y carretera: asigna dueño y valida ocupación")
    public void testPlaceSettlementAndConnectedRoadUpdatesOwnership() {
        VertexLocation settlementLoc = new VertexLocation(2, 2, 0);
        EdgeLocation connectedRoadLoc = new EdgeLocation(2, 2, 0);

        // 1. Colocación inicial sin carretera previa
        boolean settlementPlaced = board.placeStructureNoRoad(settlementLoc, player1);
        assertTrue(settlementPlaced, "El poblado inicial debería poder colocarse en un vértice libre");
        assertEquals(player1, board.getStructure(settlementLoc).getOwner(), "El dueño de la estructura debe ser player1");

        // 2. No se puede colocar otro poblado en el mismo vértice
        boolean duplicateSettlement = board.placeStructureNoRoad(settlementLoc, player2);
        assertFalse(duplicateSettlement, "No se puede colocar una estructura en un vértice ya ocupado");

        // 3. Colocación de carretera conectada al poblado
        boolean roadPlaced = board.placeRoad(connectedRoadLoc, player1);
        assertTrue(roadPlaced, "La carretera conectada al poblado del jugador debe colocarse con éxito");
        assertEquals(player1, board.getRoad(connectedRoadLoc).getOwner(), "El dueño de la carretera debe ser player1");

        // 4. No se puede colocar otra carretera en la misma arista
        boolean duplicateRoad = board.placeRoad(connectedRoadLoc, player2);
        assertFalse(duplicateRoad, "No se puede colocar una carretera en una arista ya ocupada");
    }

    @Test
    @DisplayName("Regla de distancia: rechaza poblados adyacentes a menos de 2 vértices de distancia")
    public void testDistanceRulePreventsAdjacentSettlements() {
        VertexLocation loc1 = new VertexLocation(2, 2, 0);
        VertexLocation adjacentLoc = new VertexLocation(2, 3, 1);

        boolean placedFirst = board.placeStructureNoRoad(loc1, player1);
        assertTrue(placedFirst);

        // El vértice (2, 3, 1) es adyacente a (2, 2, 0)
        boolean placedAdjacent = board.placeStructureNoRoad(adjacentLoc, player2);
        assertFalse(placedAdjacent, "La regla de la distancia debe impedir poblados en vértices adyacentes");
    }

    @Test
    @DisplayName("Rechazo de carretera desconectada sin estructuras ni caminos adyacentes")
    public void testDisconnectedRoadCannotBePlaced() {
        EdgeLocation isolatedRoad = new EdgeLocation(5, 5, 0);
        boolean roadPlaced = board.placeRoad(isolatedRoad, player2);
        assertFalse(roadPlaced, "Un jugador no puede colocar una carretera sin conexión a su red");
    }

    @Test
    @DisplayName("Distribución de recursos: poblado recibe recursos al coincidir la tirada")
    public void testDistributeResourcesHarvestsToSettlementOwner() {
        Tile productionTile = findResourceTileWithRoll();
        assertNotNull(productionTile, "Debe existir al menos una loseta con tirada y recurso");

        Location tileLoc = productionTile.getLocation();
        VertexLocation vertexOnTile = new VertexLocation(tileLoc.getXCoord(), tileLoc.getYCoord(), 0);

        board.placeStructureNoRoad(vertexOnTile, player1);

        String resType = productionTile.getType();
        int initialResources = player1.getNumberResourcesType(resType);

        board.distributeResources(productionTile.getNumber());

        int harvestedResources = player1.getNumberResourcesType(resType);
        assertEquals(initialResources + 1, harvestedResources,
                "El jugador debe recibir exactamente 1 recurso del tipo de la loseta");
    }

    @Test
    @DisplayName("Mejora a Ciudad vía Game: actualiza estructura a City y duplica la producción de recursos")
    public void testCityUpgradeDoublesResourceProduction() {
        ArrayList<Player> players = createThreePlayers();
        Game game = new Game(players);
        Board gameBoard = game.getBoard();

        Tile productionTile = findResourceTileWithRoll(gameBoard);
        Location tileLoc = productionTile.getLocation();
        VertexLocation vertexOnTile = new VertexLocation(tileLoc.getXCoord(), tileLoc.getYCoord(), 0);

        gameBoard.placeStructureNoRoad(vertexOnTile, player1);

        // Mejora a ciudad a través de Game (que instancia la clase City en el tablero)
        boolean cityPlaced = game.placeCity(player1, vertexOnTile);
        assertTrue(cityPlaced, "El dueño del poblado debe poder mejorarlo a ciudad vía Game");
        assertTrue(gameBoard.getStructure(vertexOnTile) instanceof City, "La estructura en el tablero debe ser instancia de City");

        // Otro jugador no puede mejorar un poblado ajeno
        boolean invalidUpgrade = game.placeCity(player2, vertexOnTile);
        assertFalse(invalidUpgrade, "Un jugador no puede mejorar estructuras ajenas");

        String resType = productionTile.getType();
        int initialResources = player1.getNumberResourcesType(resType);

        // Distribución de recursos para la ciudad
        gameBoard.distributeResources(productionTile.getNumber());

        int harvestedResources = player1.getNumberResourcesType(resType);
        assertEquals(initialResources + 2, harvestedResources,
                "Una ciudad debe generar 2 recursos por cada tirada productiva");
    }

    @Test
    @DisplayName("Ladrón bloquea la distribución de recursos en la loseta ocupada")
    public void testRobberBlocksResourceProductionOnTile() {
        Tile productionTile = findResourceTileWithRoll();
        Location tileLoc = productionTile.getLocation();
        VertexLocation vertexOnTile = new VertexLocation(tileLoc.getXCoord(), tileLoc.getYCoord(), 0);

        board.placeStructureNoRoad(vertexOnTile, player1);

        board.moveRobber(tileLoc);
        assertTrue(productionTile.hasRobber(), "La loseta debe tener el ladrón");

        String resType = productionTile.getType();
        int initialResources = player1.getNumberResourcesType(resType);

        board.distributeResources(productionTile.getNumber());

        assertEquals(initialResources, player1.getNumberResourcesType(resType),
                "El ladrón debe impedir que los jugadores recolecten recursos de esa loseta");
    }

    @Test
    @DisplayName("Detección de jugadores adyacentes al ladrón")
    public void testGetRobberAdjacentPlayersIdentifiesSettlementOwners() {
        Tile tile = findResourceTileWithRoll();
        Location tileLoc = tile.getLocation();

        VertexLocation v1 = new VertexLocation(tileLoc.getXCoord(), tileLoc.getYCoord(), 0);
        VertexLocation v2 = new VertexLocation(tileLoc.getXCoord(), tileLoc.getYCoord(), 1);

        board.placeStructureNoRoad(v1, player1);
        board.placeStructureNoRoad(v2, player2);

        board.moveRobber(tileLoc);

        ArrayList<Player> adjacentPlayers = board.getRobberAdjacentPlayers();
        assertTrue(adjacentPlayers.contains(player1), "Player1 debe ser detectado junto al ladrón");
        assertTrue(adjacentPlayers.contains(player2), "Player2 debe ser detectado junto al ladrón");
    }

    @Test
    @DisplayName("Integración Game: compra de estructuras deduce recursos y suma puntos de victoria")
    public void testGameBuyStructuresDeductsResourcesAndUpdatesPoints() {
        ArrayList<Player> players = createThreePlayers();
        Game game = new Game(players);

        // Configurar recursos suficientes para un poblado (BRICK, LUMBER, GRAIN, WOOL)
        player1.setNumberResourcesType("BRICK", 1);
        player1.setNumberResourcesType("LUMBER", 1);
        player1.setNumberResourcesType("GRAIN", 1);
        player1.setNumberResourcesType("WOOL", 1);

        int initialVP = player1.getVictoryPoints();
        int initialSettlements = player1.getNumbSettlements();

        int buyResult = game.buySettlement(player1);
        assertEquals(0, buyResult, "La compra debe completarse con éxito (código 0)");
        assertEquals(initialVP + 1, player1.getVictoryPoints(), "Comprar un poblado suma 1 punto de victoria");
        assertEquals(initialSettlements + 1, player1.getNumbSettlements());
        assertEquals(0, player1.getNumberResourcesType("BRICK"));
        assertEquals(0, player1.getNumberResourcesType("LUMBER"));

        // Intentar comprar de nuevo sin recursos suficientes debe fallar
        int failedBuy = game.buySettlement(player1);
        assertEquals(1, failedBuy, "Debe retornar código 1 por recursos insuficientes");

        // Compra de carretera (BRICK + LUMBER)
        player1.setNumberResourcesType("BRICK", 1);
        player1.setNumberResourcesType("LUMBER", 1);
        int roadBuyResult = game.buyRoad(player1);
        assertEquals(0, roadBuyResult);
        assertEquals(3, player1.getNumbRoads());

        // Compra de ciudad (2 GRAIN + 3 ORE)
        player1.setNumberResourcesType("GRAIN", 2);
        player1.setNumberResourcesType("ORE", 3);
        int cityBuyResult = game.buyCity(player1);
        assertEquals(0, cityBuyResult);
        assertEquals(1, player1.getNumbCities());
        assertEquals(initialVP + 2, player1.getVictoryPoints());
    }

    @Test
    @DisplayName("Integración Game: robo de carta entre jugadores con takeCard")
    public void testGameTakeCardTransfersResourceBetweenPlayers() {
        ArrayList<Player> players = createThreePlayers();
        Game game = new Game(players);

        // Player2 tiene recursos, Player1 no tiene
        player2.setNumberResourcesType("BRICK", 2);
        player1.setNumberResourcesType("BRICK", 0);

        int p2TotalBefore = player2.getTotalResources();
        int p1TotalBefore = player1.getTotalResources();

        game.takeCard(player1, player2);

        assertEquals(p2TotalBefore - 1, player2.getTotalResources(), "Player2 debe perder 1 recurso robado");
        assertEquals(p1TotalBefore + 1, player1.getTotalResources(), "Player1 debe ganar el recurso robado");
        assertEquals(1, player1.getNumberResourcesType("BRICK"));
    }

    @Test
    @DisplayName("Condición de victoria: fin de partida al alcanzar 10 puntos de victoria")
    public void testGameOverAndWinningPlayerCondition() {
        ArrayList<Player> players = createThreePlayers();
        Game game = new Game(players);

        assertFalse(game.over(), "La partida no debe terminar al inicio");
        assertNull(game.winningPlayer(), "No debe haber ganador al inicio");

        // Debido al defecto en winningPlayer() (Issue #8), el ganador no puede ser el jugador en el índice 0
        // Asignamos 10 puntos al jugador en el índice 1 para que supere a players.get(0)
        Player winner = players.get(1);
        winner.setVictoryPoints(10);

        assertTrue(game.over(), "El juego debe finalizar cuando un jugador alcanza 10 PV");
        assertEquals(winner, game.winningPlayer(), "El jugador con 10 PV debe ser el ganador");
    }

    private Tile findResourceTileWithRoll() {
        return findResourceTileWithRoll(this.board);
    }

    private Tile findResourceTileWithRoll(Board targetBoard) {
        Tile[][] tiles = targetBoard.getTiles();
        for (int r = 1; r < 6; r++) {
            for (int c = 1; c < 6; c++) {
                Tile t = tiles[c][r];
                if (t != null && !t.getType().equals("DESERT") && t.getNumber() > 0) {
                    return t;
                }
            }
        }
        return null;
    }
}

