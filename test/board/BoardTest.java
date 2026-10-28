package board;

import game.Player;
import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de {@link Board}.
 *
 * Criterios de selección de datos:
 * - Particiones de equivalencia: posiciones válidas / inválidas, jugador
 * propietario / rival / ninguno.
 * - Valores frontera: bordes del tablero (y = 1, 2, 4, 5), tiradas 1, 2, 12,
 * 13, 0 y 7.
 * - Valores aleatorios: propiedades jqwik sobre tiradas, vértices y casillas.
 *
 * El tablero se genera aleatoriamente (Collections.shuffle), así que los tests
 * nunca asumen qué recurso
 * o número hay en una casilla concreta: o recorren getTiles(), o fijan el
 * estado con los setters.
 *
 * Geometría usada (coordenadas axiales del propio Board):
 * - Casilla (x,y) tiene los vértices (x,y,0) (x,y,1) (x+1,y+1,1) (x-1,y-1,0)
 * (x,y+1,1) (x,y-1,0).
 * - Vértice (x,y,0) es adyacente a (x,y+1,1) (x+1,y+1,1) (x+1,y+2,1).
 * - Vértice (x,y,1) es adyacente a (x,y-1,0) (x-1,y-1,0) (x-1,y-2,0).
 *
 * Los tests marcados con @Disabled describen el comportamiento correcto de un
 * defecto documentado
 * en INFORME.md; se desactivan para mantener la suite en verde (necesario para
 * PITest).
 */
class BoardTest {

    /** Coordenadas de las 19 casillas de tierra. */
    static final int[][] LAND = {
            { 1, 1 }, { 2, 1 }, { 3, 1 },
            { 1, 2 }, { 2, 2 }, { 3, 2 }, { 4, 2 },
            { 1, 3 }, { 2, 3 }, { 3, 3 }, { 4, 3 }, { 5, 3 },
            { 2, 4 }, { 3, 4 }, { 4, 4 }, { 5, 4 },
            { 3, 5 }, { 4, 5 }, { 5, 5 }
    };

    /** Vértice superior y su vértice inferior en la casilla central (3,3). */
    static final VertexLocation TOP = new VertexLocation(3, 3, 0);
    static final VertexLocation BOTTOM = new VertexLocation(3, 3, 1);

    private Board board;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        board = new Board();
        alice = player("Alice");
        bob = player("Bob");
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    /**
     * Crea un jugador sin color. Board nunca usa el color, y así esta clase (que
     * está en el paquete board)
     * no depende de java.awt, cumpliendo la regla ArchUnit
     * boardShouldNotDependOnSwingOrAwt.
     */
    static Player player(String name) {
        return new Player(name, null);
    }

    static VertexLocation v(int x, int y, int o) {
        return new VertexLocation(x, y, o);
    }

    static EdgeLocation e(int x, int y, int o) {
        return new EdgeLocation(x, y, o);
    }

    static void own(Board b, VertexLocation loc, Player p) {
        b.getStructure(loc).setOwner(p);
    }

    static void own(Board b, EdgeLocation loc, Player p) {
        b.getRoad(loc).setOwner(p);
    }

    /**
     * Los seis vértices de la casilla (x,y), en el mismo orden que usa
     * distributeResources.
     */
    static VertexLocation[] verticesOf(int x, int y) {
        return new VertexLocation[] {
                v(x, y, 0), v(x, y, 1), v(x + 1, y + 1, 1),
                v(x - 1, y - 1, 0), v(x, y + 1, 1), v(x, y - 1, 0)
        };
    }

    static List<Tile> landTiles(Board b) {
        List<Tile> out = new ArrayList<>();
        for (int[] c : LAND) {
            out.add(b.getTiles()[c[0]][c[1]]);
        }
        return out;
    }

    static Tile desert(Board b) {
        for (Tile t : landTiles(b)) {
            if ("DESERT".equals(t.getType())) {
                return t;
            }
        }
        throw new IllegalStateException("El tablero no tiene desierto");
    }

    static Tile anyResourceTile(Board b) {
        for (Tile t : landTiles(b)) {
            if (!"DESERT".equals(t.getType())) {
                return t;
            }
        }
        throw new IllegalStateException("El tablero no tiene casillas de recurso");
    }

    /**
     * Pone todas las casillas de tierra a un número que nunca sale en los dados,
     * para controlar la tirada.
     */
    static void neutralizeNumbers(Board b) {
        for (Tile t : landTiles(b)) {
            t.setNumber(-1);
        }
    }

    static int totalResources(Player p) {
        int sum = 0;
        for (String r : new String[] { "BRICK", "WOOL", "ORE", "GRAIN", "LUMBER" }) {
            sum += p.getNumberResourcesType(r);
        }
        return sum;
    }

    static int tilesWithRobber(Board b) {
        int n = 0;
        for (Tile[] col : b.getTiles()) {
            for (Tile t : col) {
                if (t.hasRobber()) {
                    n++;
                }
            }
        }
        return n;
    }

    // =========================================================================
    // 1. CONSTRUCTOR
    // =========================================================================
    @Nested
    @DisplayName("Constructor de Board")
    class ConstructorTests {

        @RepeatedTest(10)
        @DisplayName("Hay exactamente 19 casillas de tierra")
        void constructor_createsNineteenLandTiles() {
            int land = 0;
            for (Tile[] col : board.getTiles()) {
                for (Tile t : col) {
                    if (t.getType() != null) {
                        land++;
                    }
                }
            }
            assertEquals(19, land);
        }

        @RepeatedTest(10)
        @DisplayName("Las casillas de tierra están en las 19 coordenadas del hexágono")
        void constructor_landTilesOccupyHexagonCoordinates() {
            Set<String> expected = new HashSet<>();
            for (int[] c : LAND) {
                expected.add(c[0] + "," + c[1]);
            }
            Set<String> actual = new HashSet<>();
            Tile[][] tiles = board.getTiles();
            for (int i = 0; i < tiles.length; i++) {
                for (int j = 0; j < tiles[i].length; j++) {
                    if (tiles[i][j].getType() != null) {
                        actual.add(i + "," + j);
                    }
                }
            }
            assertEquals(expected, actual);
        }

        @RepeatedTest(10)
        @DisplayName("Reparto de recursos según el reglamento: 4 madera, 3 ladrillo, 4 grano, 4 lana, 3 mineral, 1 desierto")
        void constructor_resourceDistributionFollowsRulebook() {
            Map<String, Integer> expected = new HashMap<>();
            expected.put("LUMBER", 4);
            expected.put("BRICK", 3);
            expected.put("GRAIN", 4);
            expected.put("WOOL", 4);
            expected.put("ORE", 3);
            expected.put("DESERT", 1);
            Map<String, Integer> actual = new HashMap<>();
            for (Tile t : landTiles(board)) {
                actual.merge(t.getType(), 1, Integer::sum);
            }
            assertEquals(expected, actual);
        }

        @RepeatedTest(10)
        @DisplayName("Las 18 casillas de recurso reciben los números del reglamento")
        void constructor_numberTokensFollowRulebook() {
            int[] expected = { 2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12 };
            int[] actual = landTiles(board).stream()
                    .filter(t -> !"DESERT".equals(t.getType()))
                    .mapToInt(Tile::getNumber).sorted().toArray();
            assertArrayEquals(expected, actual);
        }

        @RepeatedTest(10)
        @DisplayName("Ninguna casilla recibe el número 7")
        void constructor_noTileHasNumberSeven() {
            assertTrue(landTiles(board).stream().noneMatch(t -> t.getNumber() == 7));
        }

        @RepeatedTest(10)
        @DisplayName("El desierto no tiene número (0)")
        void constructor_desertHasNumberZero() {
            assertEquals(0, desert(board).getNumber());
        }

        @RepeatedTest(10)
        @DisplayName("El ladrón empieza en el desierto")
        void constructor_desertStartsWithRobber() {
            assertTrue(desert(board).hasRobber());
        }

        @RepeatedTest(10)
        @DisplayName("La ubicación del ladrón coincide con la del desierto")
        void constructor_robberLocationIsDesertLocation() {
            Location desertLoc = desert(board).getLocation();
            Location robber = board.getRobberLocation();
            assertEquals(desertLoc.getXCoord() + "," + desertLoc.getYCoord(),
                    robber.getXCoord() + "," + robber.getYCoord());
        }

        @RepeatedTest(10)
        @DisplayName("Solo una casilla tiene el ladrón")
        void constructor_exactlyOneTileHasRobber() {
            assertEquals(1, tilesWithRobber(board));
        }

        @Test
        @DisplayName("Cada casilla de tierra conoce sus propias coordenadas")
        void constructor_landTileCoordinatesMatchArrayIndex() {
            boolean allMatch = true;
            for (int[] c : LAND) {
                Location l = board.getTiles()[c[0]][c[1]].getLocation();
                allMatch &= l.getXCoord() == c[0] && l.getYCoord() == c[1];
            }
            assertTrue(allMatch);
        }

        @Test
        @DisplayName("Las casillas de mar no tienen tipo ni número")
        void constructor_seaTilesHaveNoTypeAndNumberZero() {
            Tile sea = board.getTiles()[0][0];
            assertTrue(sea.getType() == null && sea.getNumber() == 0);
        }

        @Test
        @DisplayName("Las dimensiones de las matrices son 7x7, 7x7x2 y 7x7x3")
        void constructor_gridDimensions() {
            String dims = board.getTiles().length + "x" + board.getTiles()[0].length + " "
                    + board.getStructures().length + "x" + board.getStructures()[0].length + "x"
                    + board.getStructures()[0][0].length + " "
                    + board.getRoads().length + "x" + board.getRoads()[0].length + "x" + board.getRoads()[0][0].length;
            assertEquals("7x7 7x7x2 7x7x3", dims);
        }

        @Test
        @DisplayName("Todos los vértices empiezan como poblados sin dueño")
        void constructor_allStructuresAreUnownedSettlements() {
            boolean ok = true;
            for (Structure[][] a : board.getStructures()) {
                for (Structure[] b : a) {
                    for (Structure s : b) {
                        ok &= s instanceof Settlement && s.getOwner() == null && s.getType() == 0;
                    }
                }
            }
            assertTrue(ok);
        }

        @Test
        @DisplayName("Todas las aristas empiezan como carreteras sin dueño")
        void constructor_allRoadsAreUnowned() {
            boolean ok = true;
            for (Road[][] a : board.getRoads()) {
                for (Road[] b : a) {
                    for (Road r : b) {
                        ok &= r.getOwner() == null;
                    }
                }
            }
            assertTrue(ok);
        }

        @Test
        @DisplayName("Cada estructura conoce su ubicación en la matriz")
        void constructor_structureLocationsMatchArrayIndex() {
            Structure s = board.getStructures()[4][2][1];
            VertexLocation l = s.getLocation();
            assertEquals("4,2,1", l.getXCoord() + "," + l.getYCoord() + "," + l.getOrientation());
        }

        @Test
        @DisplayName("Dos tableros nuevos son instancias independientes")
        void constructor_boardsAreIndependent() {
            Board other = new Board();
            own(board, TOP, alice);
            assertNull(other.getStructure(TOP).getOwner());
        }
    }

    // =========================================================================
    // 2. GETTERS Y SETTERS
    // =========================================================================
    @Nested
    @DisplayName("Getters y setters")
    class AccessorTests {

        @Test
        @DisplayName("getStructure devuelve la estructura de la ubicación pedida")
        void getStructure_returnsStructureAtLocation() {
            VertexLocation l = board.getStructure(BOTTOM).getLocation();
            assertEquals("3,3,1", l.getXCoord() + "," + l.getYCoord() + "," + l.getOrientation());
        }

        @Test
        @DisplayName("getStructure devuelve la misma instancia que la matriz interna")
        void getStructure_returnsSameInstanceAsArray() {
            assertSame(board.getStructures()[3][3][0], board.getStructure(TOP));
        }

        @Test
        @DisplayName("setStructure reemplaza la estructura en esa ubicación")
        void setStructure_replacesStructure() {
            City city = new City(TOP);
            board.setStructure(TOP, city);
            assertSame(city, board.getStructure(TOP));
        }

        @Test
        @DisplayName("setStructure no modifica otras ubicaciones")
        void setStructure_doesNotAffectOtherVertices() {
            Structure before = board.getStructure(BOTTOM);
            board.setStructure(TOP, new City(TOP));
            assertSame(before, board.getStructure(BOTTOM));
        }

        @Test
        @DisplayName("getRoad devuelve la carretera de la arista pedida")
        void getRoad_returnsRoadAtLocation() {
            EdgeLocation l = board.getRoad(e(2, 4, 2)).getLocation();
            assertEquals("2,4,2", l.getXCoord() + "," + l.getYCoord() + "," + l.getOrientation());
        }

        @Test
        @DisplayName("getRoad devuelve la misma instancia que la matriz interna")
        void getRoad_returnsSameInstanceAsArray() {
            assertSame(board.getRoads()[2][4][2], board.getRoad(e(2, 4, 2)));
        }

        @Test
        @DisplayName("getTile devuelve la casilla de la ubicación pedida")
        void getTile_returnsTileAtLocation() {
            assertSame(board.getTiles()[3][3], board.getTile(new Location(3, 3)));
        }

        @Test
        @DisplayName("setRobberLocation actualiza getRobberLocation")
        void setRobberLocation_updatesLocation() {
            Location loc = new Location(2, 2);
            board.setRobberLocation(loc);
            assertSame(loc, board.getRobberLocation());
        }

        @Test
        @DisplayName("Coordenadas fuera de la matriz lanzan ArrayIndexOutOfBoundsException")
        void getStructure_outOfArrayThrows() {
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> board.getStructure(v(7, 0, 0)));
        }
    }

    // =========================================================================
    // 3. COLOCAR POBLADO SIN CARRETERA (fase inicial)
    // =========================================================================
    @Nested
    @DisplayName("placeStructureNoRoad")
    class PlaceStructureNoRoadTests {

        @Test
        @DisplayName("Vértice superior libre: devuelve true")
        void noRoad_freeTopVertex_returnsTrue() {
            assertTrue(board.placeStructureNoRoad(TOP, alice));
        }

        @Test
        @DisplayName("Vértice superior libre: el jugador pasa a ser el dueño")
        void noRoad_freeTopVertex_setsOwner() {
            board.placeStructureNoRoad(TOP, alice);
            assertSame(alice, board.getStructure(TOP).getOwner());
        }

        @Test
        @DisplayName("Vértice inferior libre: devuelve true")
        void noRoad_freeBottomVertex_returnsTrue() {
            assertTrue(board.placeStructureNoRoad(BOTTOM, alice));
        }

        @Test
        @DisplayName("Vértice inferior libre: el jugador pasa a ser el dueño")
        void noRoad_freeBottomVertex_setsOwner() {
            board.placeStructureNoRoad(BOTTOM, alice);
            assertSame(alice, board.getStructure(BOTTOM).getOwner());
        }

        @Test
        @DisplayName("Vértice ya ocupado por otro jugador: devuelve false")
        void noRoad_occupiedVertex_returnsFalse() {
            own(board, TOP, bob);
            assertFalse(board.placeStructureNoRoad(TOP, alice));
        }

        @Test
        @DisplayName("Vértice ocupado: el dueño original se conserva")
        void noRoad_occupiedVertex_keepsOriginalOwner() {
            own(board, TOP, bob);
            board.placeStructureNoRoad(TOP, alice);
            assertSame(bob, board.getStructure(TOP).getOwner());
        }

        @Test
        @DisplayName("Regla de distancia (sup.): vecino (x, y+1, 1) ocupado bloquea")
        void noRoad_top_neighbourSameColumnBlocks() {
            own(board, v(3, 4, 1), bob);
            assertFalse(board.placeStructureNoRoad(TOP, alice));
        }

        @Test
        @DisplayName("Regla de distancia (sup.): vecino (x+1, y+1, 1) ocupado bloquea")
        void noRoad_top_neighbourDiagonalBlocks() {
            own(board, v(4, 4, 1), bob);
            assertFalse(board.placeStructureNoRoad(TOP, alice));
        }

        @Test
        @DisplayName("Regla de distancia (sup.): vecino (x+1, y+2, 1) ocupado bloquea")
        void noRoad_top_neighbourAboveBlocks() {
            own(board, v(4, 5, 1), bob);
            assertFalse(board.placeStructureNoRoad(TOP, alice));
        }

        @Test
        @DisplayName("Regla de distancia (inf.): vecino (x, y-1, 0) ocupado bloquea")
        void noRoad_bottom_neighbourSameColumnBlocks() {
            own(board, v(3, 2, 0), bob);
            assertFalse(board.placeStructureNoRoad(BOTTOM, alice));
        }

        @Test
        @DisplayName("Regla de distancia (inf.): vecino (x-1, y-1, 0) ocupado bloquea")
        void noRoad_bottom_neighbourDiagonalBlocks() {
            own(board, v(2, 2, 0), bob);
            assertFalse(board.placeStructureNoRoad(BOTTOM, alice));
        }

        @Test
        @DisplayName("Regla de distancia (inf.): vecino (x-1, y-2, 0) ocupado bloquea")
        void noRoad_bottom_neighbourBelowBlocks() {
            own(board, v(2, 1, 0), bob);
            assertFalse(board.placeStructureNoRoad(BOTTOM, alice));
        }

        @Test
        @DisplayName("Un poblado a distancia 2 no bloquea")
        void noRoad_settlementAtDistanceTwo_doesNotBlock() {
            own(board, v(2, 3, 0), bob);
            assertTrue(board.placeStructureNoRoad(TOP, alice));
        }

        @Test
        @DisplayName("Frontera y = 4 (sup.): se comprueba el vecino en y = 6")
        void noRoad_top_y4_checksNeighbourAtRow6() {
            own(board, v(4, 6, 1), bob);
            assertFalse(board.placeStructureNoRoad(v(3, 4, 0), alice));
        }

        @Test
        @DisplayName("Frontera y = 5 (sup.): se omite el vecino inexistente en y = 7")
        void noRoad_top_y5_skipsNeighbourOutsideBoard() {
            assertTrue(board.placeStructureNoRoad(v(3, 5, 0), alice));
        }

        @Test
        @DisplayName("Frontera y = 2 (inf.): se comprueba el vecino en y = 0")
        void noRoad_bottom_y2_checksNeighbourAtRow0() {
            own(board, v(1, 0, 0), bob);
            assertFalse(board.placeStructureNoRoad(v(2, 2, 1), alice));
        }

        @Test
        @DisplayName("Frontera y = 1 (inf.): se omite el vecino inexistente en y = -1")
        void noRoad_bottom_y1_skipsNeighbourOutsideBoard() {
            assertTrue(board.placeStructureNoRoad(v(3, 1, 1), alice));
        }

        @Test
        @DisplayName("Coordenadas fuera de la matriz lanzan ArrayIndexOutOfBoundsException")
        void noRoad_outsideArray_throws() {
            assertThrows(ArrayIndexOutOfBoundsException.class,
                    () -> board.placeStructureNoRoad(v(6, 6, 0), alice));
        }

        @Test
        @Disabled("Issue #13: placeStructureNoRoad permite construir en un vértice rodeado solo de mar")
        @DisplayName("Un vértice sin casillas de tierra adyacentes no admite poblado")
        void noRoad_seaVertex_returnsFalse() {
            assertFalse(board.placeStructureNoRoad(v(0, 4, 0), alice));
        }
    }

    // =========================================================================
    // 4. COLOCAR POBLADO CON CARRETERA
    // =========================================================================
    @Nested
    @DisplayName("placeStructure")
    class PlaceStructureTests {

        @Test
        @DisplayName("Sup.: sin carretera propia adyacente devuelve false")
        void place_top_withoutRoad_returnsFalse() {
            assertFalse(board.placeStructure(TOP, alice));
        }

        @Test
        @DisplayName("Sup.: carretera (x, y, 0) propia permite construir")
        void place_top_withRoadOrientation0_returnsTrue() {
            own(board, e(3, 3, 0), alice);
            assertTrue(board.placeStructure(TOP, alice));
        }

        @Test
        @DisplayName("Sup.: carretera (x, y, 1) propia permite construir")
        void place_top_withRoadOrientation1_returnsTrue() {
            own(board, e(3, 3, 1), alice);
            assertTrue(board.placeStructure(TOP, alice));
        }

        @Test
        @DisplayName("Sup.: carretera (x, y+1, 2) propia permite construir")
        void place_top_withRoadOrientation2_returnsTrue() {
            own(board, e(3, 4, 2), alice);
            assertTrue(board.placeStructure(TOP, alice));
        }

        @Test
        @DisplayName("Sup.: construir asigna el dueño")
        void place_top_setsOwner() {
            own(board, e(3, 3, 0), alice);
            board.placeStructure(TOP, alice);
            assertSame(alice, board.getStructure(TOP).getOwner());
        }

        @Test
        @DisplayName("Sup.: la carretera de un rival no sirve")
        void place_top_withRivalRoad_returnsFalse() {
            own(board, e(3, 3, 0), bob);
            assertFalse(board.placeStructure(TOP, alice));
        }

        @Test
        @DisplayName("Sup.: vértice ocupado devuelve false aunque haya carretera")
        void place_top_occupied_returnsFalse() {
            own(board, e(3, 3, 0), alice);
            own(board, TOP, bob);
            assertFalse(board.placeStructure(TOP, alice));
        }

        @Test
        @DisplayName("Sup.: vecino (x, y+1, 1) ocupado bloquea aunque haya carretera")
        void place_top_neighbourSameColumnBlocks() {
            own(board, e(3, 3, 0), alice);
            own(board, v(3, 4, 1), bob);
            assertFalse(board.placeStructure(TOP, alice));
        }

        @Test
        @DisplayName("Sup.: vecino (x+1, y+1, 1) ocupado bloquea aunque haya carretera")
        void place_top_neighbourDiagonalBlocks() {
            own(board, e(3, 3, 0), alice);
            own(board, v(4, 4, 1), bob);
            assertFalse(board.placeStructure(TOP, alice));
        }

        @Test
        @DisplayName("Sup.: vecino (x+1, y+2, 1) ocupado bloquea aunque haya carretera")
        void place_top_neighbourAboveBlocks() {
            own(board, e(3, 3, 0), alice);
            own(board, v(4, 5, 1), bob);
            assertFalse(board.placeStructure(TOP, alice));
        }

        @Test
        @DisplayName("Sup. frontera y = 5: se omite el vecino inexistente en y = 7")
        void place_top_y5_skipsNeighbourOutsideBoard() {
            own(board, e(3, 5, 0), alice);
            assertTrue(board.placeStructure(v(3, 5, 0), alice));
        }

        @Test
        @DisplayName("Inf.: sin carretera propia adyacente devuelve false")
        void place_bottom_withoutRoad_returnsFalse() {
            assertFalse(board.placeStructure(BOTTOM, alice));
        }

        @Test
        @DisplayName("Inf.: carretera (x, y-1, 0) propia permite construir")
        void place_bottom_withRoadOrientation0_returnsTrue() {
            own(board, e(3, 2, 0), alice);
            assertTrue(board.placeStructure(BOTTOM, alice));
        }

        @Test
        @DisplayName("Inf.: carretera (x-1, y-1, 1) propia permite construir")
        void place_bottom_withRoadOrientation1_returnsTrue() {
            own(board, e(2, 2, 1), alice);
            assertTrue(board.placeStructure(BOTTOM, alice));
        }

        @Test
        @DisplayName("Inf.: carretera (x-1, y-1, 2) propia permite construir")
        void place_bottom_withRoadOrientation2_returnsTrue() {
            own(board, e(2, 2, 2), alice);
            assertTrue(board.placeStructure(BOTTOM, alice));
        }

        @Test
        @DisplayName("Inf.: construir asigna el dueño")
        void place_bottom_setsOwner() {
            own(board, e(3, 2, 0), alice);
            board.placeStructure(BOTTOM, alice);
            assertSame(alice, board.getStructure(BOTTOM).getOwner());
        }

        @Test
        @DisplayName("Inf.: vecino (x, y-1, 0) ocupado bloquea aunque haya carretera")
        void place_bottom_neighbourSameColumnBlocks() {
            own(board, e(2, 2, 1), alice);
            own(board, v(3, 2, 0), bob);
            assertFalse(board.placeStructure(BOTTOM, alice));
        }

        @Test
        @DisplayName("Inf.: vecino (x-1, y-1, 0) ocupado bloquea aunque haya carretera")
        void place_bottom_neighbourDiagonalBlocks() {
            own(board, e(3, 2, 0), alice);
            own(board, v(2, 2, 0), bob);
            assertFalse(board.placeStructure(BOTTOM, alice));
        }

        @Test
        @DisplayName("Inf.: vecino (x-1, y-2, 0) ocupado bloquea aunque haya carretera")
        void place_bottom_neighbourBelowBlocks() {
            own(board, e(3, 2, 0), alice);
            own(board, v(2, 1, 0), bob);
            assertFalse(board.placeStructure(BOTTOM, alice));
        }

        @Test
        @DisplayName("Inf.: vértice ocupado devuelve false aunque haya carretera")
        void place_bottom_occupied_returnsFalse() {
            own(board, e(3, 2, 0), alice);
            own(board, BOTTOM, bob);
            assertFalse(board.placeStructure(BOTTOM, alice));
        }

        @Test
        @DisplayName("Inf. frontera y = 1: se omite el vecino inexistente en y = -1")
        void place_bottom_y1_skipsNeighbourOutsideBoard() {
            own(board, e(3, 0, 0), alice);
            assertTrue(board.placeStructure(v(3, 1, 1), alice));
        }
    }

    // =========================================================================
    // 5. PUERTOS (checkPort, probado a través de placeStructure*)
    // =========================================================================
    @Nested
    @DisplayName("Puertos")
    class PortTests {

        /**
         * {x, y, orientación, tipo de puerto}. 0 genérico, 1 ladrillo, 2 lana, 3
         * mineral, 4 grano, 5 madera.
         */
        final int[][] ports = {
                { 4, 1, 0, 1 }, { 4, 2, 1, 1 },
                { 4, 5, 0, 2 }, { 5, 6, 1, 2 },
                { 1, 3, 0, 3 }, { 2, 5, 1, 3 },
                { 0, 1, 0, 4 }, { 1, 3, 1, 4 },
                { 2, 0, 0, 5 }, { 2, 1, 1, 5 },
                { 0, 0, 0, 0 }, { 1, 1, 1, 0 },
                { 5, 2, 0, 0 }, { 6, 4, 1, 0 },
                { 5, 4, 0, 0 }, { 6, 5, 1, 0 },
                { 3, 5, 0, 0 }, { 3, 6, 1, 0 }
        };

        boolean[] portsAfterSettlingAt(int x, int y, int o) {
            Board b = new Board();
            Player p = player("P");
            b.placeStructureNoRoad(v(x, y, o), p);
            return p.getPorts();
        }

        @Test
        @DisplayName("Puerto de ladrillo (4,1,0) otorga el puerto 1")
        void port_brick() {
            assertTrue(portsAfterSettlingAt(4, 1, 0)[1]);
        }

        @Test
        @DisplayName("Puerto de lana (4,5,0) otorga el puerto 2")
        void port_wool() {
            assertTrue(portsAfterSettlingAt(4, 5, 0)[2]);
        }

        @Test
        @DisplayName("Puerto de mineral (1,3,0) otorga el puerto 3")
        void port_ore() {
            assertTrue(portsAfterSettlingAt(1, 3, 0)[3]);
        }

        @Test
        @DisplayName("Puerto de grano (0,1,0) otorga el puerto 4")
        void port_grain() {
            assertTrue(portsAfterSettlingAt(0, 1, 0)[4]);
        }

        @Test
        @DisplayName("Puerto de madera (2,0,0) otorga el puerto 5")
        void port_lumber() {
            assertTrue(portsAfterSettlingAt(2, 0, 0)[5]);
        }

        @Test
        @DisplayName("Puerto genérico (0,0,0) otorga el puerto 0")
        void port_generic() {
            assertTrue(portsAfterSettlingAt(0, 0, 0)[0]);
        }

        @Test
        @DisplayName("Cada uno de los 18 vértices de puerto otorga exactamente su puerto")
        void port_everyPortVertexGrantsOnlyItsPort() {
            List<String> wrong = new ArrayList<>();
            for (int[] p : ports) {
                boolean[] expected = new boolean[6];
                expected[p[3]] = true;
                if (!Arrays.equals(expected, portsAfterSettlingAt(p[0], p[1], p[2]))) {
                    wrong.add(Arrays.toString(p));
                }
            }
            assertEquals(List.of(), wrong);
        }

        @Test
        @DisplayName("Mismas coordenadas que un puerto pero otra orientación: no otorga puerto")
        void port_sameCoordinatesOtherOrientationGrantsNoPort() {
            Set<String> portKeys = new HashSet<>();
            for (int[] p : ports) {
                portKeys.add(p[0] + "," + p[1] + "," + p[2]);
            }
            List<String> wrong = new ArrayList<>();
            for (int[] p : ports) {
                int flipped = 1 - p[2];
                if (portKeys.contains(p[0] + "," + p[1] + "," + flipped)) {
                    continue; // (1,3,0) y (1,3,1) son ambos puertos
                }
                try {
                    if (!Arrays.equals(new boolean[6], portsAfterSettlingAt(p[0], p[1], flipped))) {
                        wrong.add(p[0] + "," + p[1] + "," + flipped);
                    }
                } catch (ArrayIndexOutOfBoundsException outsideArray) {
                    // vértice fuera de la matriz: no aplica
                }
            }
            assertEquals(List.of(), wrong);
        }

        @Test
        @DisplayName("Un vértice interior no otorga ningún puerto")
        void port_interiorVertexGrantsNoPort() {
            assertArrayEquals(new boolean[6], portsAfterSettlingAt(3, 3, 0));
        }

        @Test
        @DisplayName("placeStructure (vértice sup.) también otorga el puerto")
        void port_grantedByPlaceStructureTop() {
            own(board, e(4, 1, 0), alice);
            board.placeStructure(v(4, 1, 0), alice);
            assertTrue(alice.getPorts()[1]);
        }

        @Test
        @DisplayName("placeStructure (vértice inf.) también otorga el puerto")
        void port_grantedByPlaceStructureBottom() {
            own(board, e(4, 1, 0), alice);
            board.placeStructure(v(4, 2, 1), alice);
            assertTrue(alice.getPorts()[1]);
        }

        @Test
        @DisplayName("Si la colocación falla no se otorga el puerto")
        void port_notGrantedWhenPlacementFails() {
            own(board, v(4, 2, 1), bob);
            board.placeStructureNoRoad(v(4, 1, 0), alice);
            assertFalse(alice.getPorts()[1]);
        }
    }

    // =========================================================================
    // 6. CARRETERAS
    // =========================================================================
    @Nested
    @DisplayName("placeRoad")
    class PlaceRoadTests {

        // --- Orientación 0: extremos (x,y+1,1) y (x,y,0) ---

        @Test
        @DisplayName("O0: conectada a poblado propio en (x, y+1, 1)")
        void road0_connectedToSettlementBelow() {
            own(board, v(3, 4, 1), alice);
            assertTrue(board.placeRoad(e(3, 3, 0), alice));
        }

        @Test
        @DisplayName("O0: conectada a poblado propio en (x, y, 0)")
        void road0_connectedToSettlementAbove() {
            own(board, TOP, alice);
            assertTrue(board.placeRoad(e(3, 3, 0), alice));
        }

        @Test
        @DisplayName("O0: conectada a carretera propia (x-1, y, 1)")
        void road0_connectedToRoadLeft1() {
            own(board, e(2, 3, 1), alice);
            assertTrue(board.placeRoad(e(3, 3, 0), alice));
        }

        @Test
        @DisplayName("O0: conectada a carretera propia (x-1, y, 2)")
        void road0_connectedToRoadLeft2() {
            own(board, e(2, 3, 2), alice);
            assertTrue(board.placeRoad(e(3, 3, 0), alice));
        }

        @Test
        @DisplayName("O0: conectada a carretera propia (x, y+1, 2)")
        void road0_connectedToRoadUp2() {
            own(board, e(3, 4, 2), alice);
            assertTrue(board.placeRoad(e(3, 3, 0), alice));
        }

        @Test
        @DisplayName("O0: conectada a carretera propia (x, y, 1)")
        void road0_connectedToRoadSame1() {
            own(board, e(3, 3, 1), alice);
            assertTrue(board.placeRoad(e(3, 3, 0), alice));
        }

        @Test
        @DisplayName("O0: sin conexión devuelve false")
        void road0_disconnected_returnsFalse() {
            assertFalse(board.placeRoad(e(3, 3, 0), alice));
        }

        // --- Orientación 1: extremos (x,y,0) y (x+1,y+1,1) ---

        @Test
        @DisplayName("O1: conectada a poblado propio en (x, y, 0)")
        void road1_connectedToSettlementTop() {
            own(board, TOP, alice);
            assertTrue(board.placeRoad(e(3, 3, 1), alice));
        }

        @Test
        @DisplayName("O1: conectada a poblado propio en (x+1, y+1, 1)")
        void road1_connectedToSettlementRight() {
            own(board, v(4, 4, 1), alice);
            assertTrue(board.placeRoad(e(3, 3, 1), alice));
        }

        @Test
        @DisplayName("O1: conectada a carretera propia (x, y, 0)")
        void road1_connectedToRoadSame0() {
            own(board, e(3, 3, 0), alice);
            assertTrue(board.placeRoad(e(3, 3, 1), alice));
        }

        @Test
        @DisplayName("O1: conectada a carretera propia (x, y+1, 2)")
        void road1_connectedToRoadUp2() {
            own(board, e(3, 4, 2), alice);
            assertTrue(board.placeRoad(e(3, 3, 1), alice));
        }

        @Test
        @DisplayName("O1: conectada a carretera propia (x, y, 2)")
        void road1_connectedToRoadSame2() {
            own(board, e(3, 3, 2), alice);
            assertTrue(board.placeRoad(e(3, 3, 1), alice));
        }

        @Test
        @DisplayName("O1: conectada a carretera propia (x+1, y, 0)")
        void road1_connectedToRoadRight0() {
            own(board, e(4, 3, 0), alice);
            assertTrue(board.placeRoad(e(3, 3, 1), alice));
        }

        @Test
        @DisplayName("O1: sin conexión devuelve false")
        void road1_disconnected_returnsFalse() {
            assertFalse(board.placeRoad(e(3, 3, 1), alice));
        }

        // --- Orientación 2: extremos (x,y-1,0) y (x+1,y+1,1) ---

        @Test
        @DisplayName("O2: conectada a poblado propio en (x, y-1, 0)")
        void road2_connectedToSettlementBelow() {
            own(board, v(3, 2, 0), alice);
            assertTrue(board.placeRoad(e(3, 3, 2), alice));
        }

        @Test
        @DisplayName("O2: conectada a poblado propio en (x+1, y+1, 1)")
        void road2_connectedToSettlementRight() {
            own(board, v(4, 4, 1), alice);
            assertTrue(board.placeRoad(e(3, 3, 2), alice));
        }

        @Test
        @DisplayName("O2: conectada a carretera propia (x, y, 1)")
        void road2_connectedToRoadSame1() {
            own(board, e(3, 3, 1), alice);
            assertTrue(board.placeRoad(e(3, 3, 2), alice));
        }

        @Test
        @DisplayName("O2: conectada a carretera propia (x+1, y, 0)")
        void road2_connectedToRoadRight0() {
            own(board, e(4, 3, 0), alice);
            assertTrue(board.placeRoad(e(3, 3, 2), alice));
        }

        @Test
        @DisplayName("O2: conectada a carretera propia (x, y-1, 0)")
        void road2_connectedToRoadDown0() {
            own(board, e(3, 2, 0), alice);
            assertTrue(board.placeRoad(e(3, 3, 2), alice));
        }

        @Test
        @DisplayName("O2: conectada a carretera propia (x, y-1, 1)")
        void road2_connectedToRoadDown1() {
            own(board, e(3, 2, 1), alice);
            assertTrue(board.placeRoad(e(3, 3, 2), alice));
        }

        @Test
        @DisplayName("O2: sin conexión devuelve false")
        void road2_disconnected_returnsFalse() {
            assertFalse(board.placeRoad(e(3, 3, 2), alice));
        }

        // --- Casos comunes ---

        @Test
        @DisplayName("Arista ya ocupada devuelve false")
        void road_occupied_returnsFalse() {
            own(board, TOP, alice);
            own(board, e(3, 3, 0), bob);
            assertFalse(board.placeRoad(e(3, 3, 0), alice));
        }

        @Test
        @DisplayName("Colocar asigna el dueño de la carretera")
        void road_setsOwner() {
            own(board, TOP, alice);
            board.placeRoad(e(3, 3, 0), alice);
            assertSame(alice, board.getRoad(e(3, 3, 0)).getOwner());
        }

        @Test
        @DisplayName("Colocar añade la carretera a la lista del jugador")
        void road_addedToPlayerRoads() {
            own(board, TOP, alice);
            board.placeRoad(e(3, 3, 0), alice);
            assertTrue(alice.getRoads().contains(board.getRoad(e(3, 3, 0))));
        }

        @Test
        @DisplayName("Un poblado rival no sirve como conexión")
        void road_rivalSettlementIsNotAConnection() {
            own(board, TOP, bob);
            assertFalse(board.placeRoad(e(3, 3, 0), alice));
        }

        @Test
        @DisplayName("Una carretera rival no sirve como conexión")
        void road_rivalRoadIsNotAConnection() {
            own(board, e(3, 3, 1), bob);
            assertFalse(board.placeRoad(e(3, 3, 0), alice));
        }

        @Test
        @Disabled("Issue #15: placeRoad permite continuar una carretera a través de un poblado rival")
        @DisplayName("No se puede continuar una carretera a través de un poblado rival")
        void road_cannotPassThroughRivalSettlement() {
            own(board, e(2, 3, 1), alice); // llega al vértice (3,4,1)
            own(board, v(3, 4, 1), bob); // Bob tiene poblado en ese vértice
            assertFalse(board.placeRoad(e(3, 3, 0), alice));
        }
    }

    // =========================================================================
    // 7. CIUDADES
    // =========================================================================
    @Nested
    @DisplayName("placeCity")
    class PlaceCityTests {

        @Test
        @DisplayName("Mejorar un poblado propio devuelve true")
        void city_ownSettlement_returnsTrue() {
            own(board, TOP, alice);
            assertTrue(board.placeCity(TOP, alice));
        }

        @Test
        @DisplayName("Mejorar un poblado propio cambia el tipo a 1")
        void city_ownSettlement_setsTypeCity() {
            own(board, TOP, alice);
            board.placeCity(TOP, alice);
            assertEquals(1, board.getStructure(TOP).getType());
        }

        @Test
        @DisplayName("Mejorar algo que ya es ciudad devuelve false")
        void city_alreadyCity_returnsFalse() {
            own(board, TOP, alice);
            board.placeCity(TOP, alice);
            assertFalse(board.placeCity(TOP, alice));
        }

        @Test
        @DisplayName("Mejorar el poblado de un rival devuelve false")
        void city_rivalSettlement_returnsFalse() {
            own(board, TOP, bob);
            assertFalse(board.placeCity(TOP, alice));
        }

        @Test
        @DisplayName("Mejorar el poblado de un rival no cambia su tipo")
        void city_rivalSettlement_keepsType() {
            own(board, TOP, bob);
            board.placeCity(TOP, alice);
            assertEquals(0, board.getStructure(TOP).getType());
        }

        @Test
        @DisplayName("Mejorar un vértice vacío devuelve false")
        void city_emptyVertex_returnsFalse() {
            assertFalse(board.placeCity(TOP, alice));
        }

        @Test
        @Disabled("Issue #6: placeCity solo cambia el tipo; la estructura sigue siendo Settlement")
        @DisplayName("Tras mejorar, la estructura es una instancia de City")
        void city_structureBecomesCityInstance() {
            own(board, TOP, alice);
            board.placeCity(TOP, alice);
            assertTrue(board.getStructure(TOP) instanceof City);
        }
    }

    // =========================================================================
    // 8. REPARTO DE RECURSOS
    // =========================================================================
    @Nested
    @DisplayName("distributeResources")
    class DistributeResourcesTests {

        Tile target;

        @BeforeEach
        void prepareTarget() {
            neutralizeNumbers(board);
            target = anyResourceTile(board);
            target.setNumber(6);
        }

        VertexLocation topOfTarget() {
            return v(target.getLocation().getXCoord(), target.getLocation().getYCoord(), 0);
        }

        @Test
        @DisplayName("Poblado junto a la casilla tirada recibe 1 recurso de su tipo")
        void distribute_settlementReceivesOne() {
            own(board, topOfTarget(), alice);
            board.distributeResources(6);
            assertEquals(1, alice.getNumberResourcesType(target.getType()));
        }

        @Test
        @DisplayName("Ciudad junto a la casilla tirada recibe 2 recursos de su tipo")
        void distribute_cityReceivesTwo() {
            City city = new City(topOfTarget());
            board.setStructure(topOfTarget(), city);
            city.setOwner(alice);
            board.distributeResources(6);
            assertEquals(2, alice.getNumberResourcesType(target.getType()));
        }

        @Test
        @DisplayName("Si el número no coincide no se reparte nada")
        void distribute_otherNumber_givesNothing() {
            own(board, topOfTarget(), alice);
            board.distributeResources(8);
            assertEquals(0, totalResources(alice));
        }

        @Test
        @DisplayName("La casilla con el ladrón no produce")
        void distribute_robberBlocksProduction() {
            own(board, topOfTarget(), alice);
            board.moveRobber(target.getLocation());
            board.distributeResources(6);
            assertEquals(0, totalResources(alice));
        }

        @Test
        @DisplayName("El desierto no produce aunque tenga número y no tenga ladrón")
        void distribute_desertNeverProduces() {
            Tile d = desert(board);
            d.setNumber(6);
            target.setNumber(-1);
            board.moveRobber(target.getLocation()); // saca el ladrón del desierto
            own(board, v(d.getLocation().getXCoord(), d.getLocation().getYCoord(), 0), alice);
            board.distributeResources(6);
            assertEquals(0, totalResources(alice));
        }

        @Test
        @DisplayName("Vértices sin dueño no provocan excepción")
        void distribute_noOwners_doesNotThrow() {
            assertDoesNotThrow(() -> board.distributeResources(6));
        }

        @Test
        @DisplayName("Dos jugadores en la misma casilla reciben ambos")
        void distribute_twoPlayersBothReceive() {
            int x = target.getLocation().getXCoord(), y = target.getLocation().getYCoord();
            own(board, v(x, y, 0), alice);
            own(board, v(x, y, 1), bob);
            board.distributeResources(6);
            assertEquals(2,
                    alice.getNumberResourcesType(target.getType()) + bob.getNumberResourcesType(target.getType()));
        }

        @Test
        @DisplayName("Un jugador lejos de la casilla no recibe nada")
        void distribute_farPlayerReceivesNothing() {
            int x = target.getLocation().getXCoord(), y = target.getLocation().getYCoord();
            List<String> targetVertices = new ArrayList<>();
            for (VertexLocation tv : verticesOf(x, y)) {
                targetVertices.add(tv.getXCoord() + "," + tv.getYCoord() + "," + tv.getOrientation());
            }
            // Primer vértice superior de tierra que no pertenece a la casilla objetivo
            VertexLocation far = null;
            for (int[] c : LAND) {
                if (!targetVertices.contains(c[0] + "," + c[1] + ",0")) {
                    far = v(c[0], c[1], 0);
                    break;
                }
            }
            own(board, far, bob);
            board.distributeResources(6);
            assertEquals(0, totalResources(bob));
        }

        @Test
        @DisplayName("Dos casillas con el mismo número producen ambas")
        void distribute_twoTilesSameNumber() {
            int x = target.getLocation().getXCoord(), y = target.getLocation().getYCoord();
            VertexLocation top = v(x, y, 0);
            // Las otras dos casillas de tierra que tocan este vértice (si existen) también
            // sacan un 6
            for (Tile t : board.getAdjacentTilesStructure(top)) {
                if (!"DESERT".equals(t.getType())) {
                    t.setNumber(6);
                }
            }
            own(board, top, alice);
            int expected = (int) board.getAdjacentTilesStructure(top).stream()
                    .filter(t -> !"DESERT".equals(t.getType())).count();
            board.distributeResources(6);
            assertEquals(expected, totalResources(alice));
        }

        @Test
        @DisplayName("Frontera: tirada 2 (mínima) produce")
        void distribute_roll2() {
            target.setNumber(2);
            own(board, topOfTarget(), alice);
            board.distributeResources(2);
            assertEquals(1, totalResources(alice));
        }

        @Test
        @DisplayName("Frontera: tirada 12 (máxima) produce")
        void distribute_roll12() {
            target.setNumber(12);
            own(board, topOfTarget(), alice);
            board.distributeResources(12);
            assertEquals(1, totalResources(alice));
        }

        @Test
        @DisplayName("Frontera: tirada 1 (por debajo del rango) no produce")
        void distribute_roll1() {
            own(board, topOfTarget(), alice);
            board.distributeResources(1);
            assertEquals(0, totalResources(alice));
        }

        @Test
        @DisplayName("Frontera: tirada 13 (por encima del rango) no produce")
        void distribute_roll13() {
            own(board, topOfTarget(), alice);
            board.distributeResources(13);
            assertEquals(0, totalResources(alice));
        }

        @Test
        @DisplayName("Tirada 7 (ladrón) no produce en un tablero recién creado")
        void distribute_roll7OnFreshBoard() {
            Board fresh = new Board();
            for (int[] c : LAND) {
                own(fresh, v(c[0], c[1], 0), alice);
            }
            fresh.distributeResources(7);
            assertEquals(0, totalResources(alice));
        }

        @Test
        @Disabled("Issue #12: distributeResources(0) lanza NullPointerException por las casillas de mar")
        @DisplayName("Tirada 0 (inválida) no lanza excepción")
        void distribute_roll0_doesNotThrow() {
            assertDoesNotThrow(() -> new Board().distributeResources(0));
        }
    }

    // =========================================================================
    // 9. LADRÓN
    // =========================================================================
    @Nested
    @DisplayName("Ladrón")
    class RobberTests {

        @Test
        @DisplayName("moveRobber a la misma casilla devuelve false")
        void moveRobber_sameTile_returnsFalse() {
            Location current = board.getRobberLocation();
            assertFalse(board.moveRobber(new Location(current.getXCoord(), current.getYCoord())));
        }

        @Test
        @DisplayName("moveRobber a la misma casilla mantiene el ladrón en ella")
        void moveRobber_sameTile_keepsRobber() {
            Location current = board.getRobberLocation();
            board.moveRobber(new Location(current.getXCoord(), current.getYCoord()));
            assertTrue(board.getTile(current).hasRobber());
        }

        @Test
        @DisplayName("Misma X pero distinta Y cuenta como casilla distinta")
        void moveRobber_sameXDifferentY_returnsTrue() {
            board.setRobberLocation(new Location(3, 3));
            board.getTiles()[3][3].setRobber(true);
            assertTrue(board.moveRobber(new Location(3, 4)));
        }

        @Test
        @DisplayName("Misma Y pero distinta X cuenta como casilla distinta")
        void moveRobber_sameYDifferentX_returnsTrue() {
            board.setRobberLocation(new Location(3, 3));
            board.getTiles()[3][3].setRobber(true);
            assertTrue(board.moveRobber(new Location(4, 3)));
        }

        @Test
        @DisplayName("moveRobber a otra casilla devuelve true")
        void moveRobber_otherTile_returnsTrue() {
            assertTrue(board.moveRobber(anyResourceTile(board).getLocation()));
        }

        @Test
        @DisplayName("moveRobber marca la casilla destino")
        void moveRobber_otherTile_marksDestination() {
            Tile t = anyResourceTile(board);
            board.moveRobber(t.getLocation());
            assertTrue(t.hasRobber());
        }

        @Test
        @DisplayName("moveRobber desmarca la casilla de origen")
        void moveRobber_otherTile_clearsOrigin() {
            Tile origin = desert(board);
            board.moveRobber(anyResourceTile(board).getLocation());
            assertFalse(origin.hasRobber());
        }

        @Test
        @DisplayName("moveRobber actualiza getRobberLocation")
        void moveRobber_updatesRobberLocation() {
            Location dest = anyResourceTile(board).getLocation();
            board.moveRobber(dest);
            assertSame(dest, board.getRobberLocation());
        }

        @Test
        @Disabled("Issue #14: moveRobber acepta casillas de mar")
        @DisplayName("No se puede mover el ladrón al mar")
        void moveRobber_seaTile_returnsFalse() {
            assertFalse(board.moveRobber(new Location(0, 0)));
        }

        @Test
        @DisplayName("getRobberAdjacentPlayers sin estructuras devuelve lista vacía")
        void adjacentPlayers_none_isEmpty() {
            board.setRobberLocation(new Location(3, 3));
            assertTrue(board.getRobberAdjacentPlayers().isEmpty());
        }

        @Test
        @DisplayName("getRobberAdjacentPlayers incluye al dueño de un vértice de la casilla")
        void adjacentPlayers_includesOwner() {
            board.setRobberLocation(new Location(3, 3));
            own(board, TOP, alice);
            assertEquals(List.of(alice), board.getRobberAdjacentPlayers());
        }

        @Test
        @DisplayName("Un jugador con dos estructuras aparece una sola vez")
        void adjacentPlayers_noDuplicates() {
            board.setRobberLocation(new Location(3, 3));
            own(board, TOP, alice);
            own(board, BOTTOM, alice);
            assertEquals(1, board.getRobberAdjacentPlayers().size());
        }

        @Test
        @DisplayName("Dos jugadores distintos aparecen ambos")
        void adjacentPlayers_twoPlayers() {
            board.setRobberLocation(new Location(3, 3));
            own(board, TOP, alice);
            own(board, BOTTOM, bob);
            assertEquals(2, board.getRobberAdjacentPlayers().size());
        }

        @Test
        @DisplayName("Un jugador con estructura fuera de la casilla no aparece")
        void adjacentPlayers_excludesFarOwner() {
            board.setRobberLocation(new Location(3, 3));
            own(board, v(2, 3, 0), bob);
            assertTrue(board.getRobberAdjacentPlayers().isEmpty());
        }
    }

    // =========================================================================
    // 10. CASILLAS ADYACENTES A UN VÉRTICE
    // =========================================================================
    @Nested
    @DisplayName("getAdjacentTilesStructure")
    class AdjacentTilesTests {

        Set<String> coords(List<Tile> tiles) {
            Set<String> s = new HashSet<>();
            for (Tile t : tiles) {
                s.add(t.getLocation().getXCoord() + "," + t.getLocation().getYCoord());
            }
            return s;
        }

        @Test
        @DisplayName("Vértice superior interior: casillas (x,y) (x,y+1) (x+1,y+1)")
        void adjacent_topInterior() {
            assertEquals(Set.of("3,3", "3,4", "4,4"), coords(board.getAdjacentTilesStructure(TOP)));
        }

        @Test
        @DisplayName("Vértice inferior interior: casillas (x,y) (x,y-1) (x-1,y-1)")
        void adjacent_bottomInterior() {
            assertEquals(Set.of("3,3", "3,2", "2,2"), coords(board.getAdjacentTilesStructure(BOTTOM)));
        }

        @Test
        @DisplayName("Vértice superior de costa: solo 1 casilla de tierra")
        void adjacent_topCoast() {
            assertEquals(1, board.getAdjacentTilesStructure(v(0, 1, 0)).size());
        }

        @Test
        @DisplayName("Vértice inferior de costa: 2 casillas de tierra")
        void adjacent_bottomCoast() {
            assertEquals(2, board.getAdjacentTilesStructure(v(5, 6, 1)).size());
        }

        @Test
        @DisplayName("Vértice superior en el mar: ninguna casilla")
        void adjacent_topSea() {
            assertTrue(board.getAdjacentTilesStructure(v(0, 4, 0)).isEmpty());
        }

        @Test
        @DisplayName("Vértice inferior en el mar: ninguna casilla")
        void adjacent_bottomSea() {
            assertTrue(board.getAdjacentTilesStructure(v(1, 5, 1)).isEmpty());
        }
    }

    // =========================================================================
    // 11. CAMINO MÁS LARGO (defectuoso: ver Issues #9, #10 y #11)
    // =========================================================================
    @Nested
    @DisplayName("findLongestRoad")
    class LongestRoadTests {

        @Test
        @Disabled("Issue #11: findLongestRoad devuelve 1 para un jugador sin carreteras")
        @DisplayName("Jugador sin carreteras: longitud 0")
        void longest_noRoads_isZero() {
            assertEquals(0, board.findLongestRoad(alice));
        }

        @Test
        @Disabled("Issue #9: los bucles usan <= size() y lanzan IndexOutOfBoundsException")
        @DisplayName("Una carretera (orientación 0): longitud 1")
        void longest_singleRoadOrientation0_isOne() {
            own(board, e(3, 3, 0), alice);
            assertEquals(1, board.findLongestRoad(alice));
        }

        @Test
        @Disabled("Issue #10: findAdjacentRoads usa siempre la orientación 0 y lanza NullPointerException")
        @DisplayName("Una carretera (orientación 1): longitud 1")
        void longest_singleRoadOrientation1_isOne() {
            own(board, e(3, 3, 1), alice);
            assertEquals(1, board.findLongestRoad(alice));
        }

        @Test
        @Disabled("Issues #9 y #10: findLongestRoad lanza excepción con cualquier carretera")
        @DisplayName("Tres carreteras en línea: longitud 3")
        void longest_chainOfThree_isThree() {
            own(board, e(3, 3, 1), alice); // (3,3,0) -> (4,4,1)
            own(board, e(4, 3, 0), alice); // (4,4,1) -> (4,3,0)
            own(board, e(4, 3, 1), alice); // (4,3,0) -> (5,4,1)
            assertEquals(3, board.findLongestRoad(alice));
        }
    }

    // =========================================================================
    // 12. PROPIEDADES (jqwik) — no usan @BeforeEach, que jqwik no ejecuta
    // =========================================================================

    @Property(tries = 50)
    @Label("Cualquier vértice de la casilla tirada recibe exactamente 1 recurso")
    void prop_everyVertexOfRolledTileReceivesOne(@ForAll @IntRange(min = 0, max = 5) int vertexIndex) {
        Board b = new Board();
        Player p = player("P");
        neutralizeNumbers(b);
        Tile t = anyResourceTile(b);
        t.setNumber(9);
        own(b, verticesOf(t.getLocation().getXCoord(), t.getLocation().getYCoord())[vertexIndex], p);
        b.distributeResources(9);
        assertEquals(1, p.getNumberResourcesType(t.getType()));
    }

    @Property(tries = 100)
    @Label("Para cualquier tirada válida, el vértice central recibe tantos recursos como casillas productoras toca")
    void prop_centralVertexReceivesOnePerProducingTile(@ForAll @IntRange(min = 2, max = 12) int roll) {
        Board b = new Board();
        Player p = player("P");
        own(b, TOP, p);
        long expected = b.getAdjacentTilesStructure(TOP).stream()
                .filter(t -> t.getNumber() == roll && !t.hasRobber() && !"DESERT".equals(t.getType()))
                .count();
        b.distributeResources(roll);
        assertEquals(expected, (long) totalResources(p));
    }

    @Property(tries = 100)
    @Label("Una tirada por encima de 12 nunca produce recursos")
    void prop_rollAboveTwelveProducesNothing(@ForAll @IntRange(min = 13, max = 10_000) int roll) {
        Board b = new Board();
        Player p = player("P");
        for (int[] c : LAND) {
            own(b, v(c[0], c[1], 0), p);
        }
        b.distributeResources(roll);
        assertEquals(0, totalResources(p));
    }

    @Property(tries = 100)
    @Label("Una tirada negativa nunca produce recursos")
    void prop_negativeRollProducesNothing(@ForAll @IntRange(min = -10_000, max = -1) int roll) {
        Board b = new Board();
        Player p = player("P");
        for (int[] c : LAND) {
            own(b, v(c[0], c[1], 0), p);
        }
        b.distributeResources(roll);
        assertEquals(0, totalResources(p));
    }

    @Property(tries = 50)
    @Label("Tras mover el ladrón a cualquier otra casilla de tierra, solo esa casilla tiene ladrón")
    void prop_robberIsAlwaysOnExactlyOneTile(@ForAll @IntRange(min = 0, max = 18) int landIndex) {
        Board b = new Board();
        Location dest = new Location(LAND[landIndex][0], LAND[landIndex][1]);
        Location cur = b.getRobberLocation();
        Assume.that(cur.getXCoord() != dest.getXCoord() || cur.getYCoord() != dest.getYCoord());
        b.moveRobber(dest);
        assertTrue(tilesWithRobber(b) == 1 && b.getTile(dest).hasRobber());
    }

    @Property(tries = 50)
    @Label("El dueño de cualquier vértice de la casilla del ladrón es un jugador adyacente")
    void prop_ownerOfAnyRobberVertexIsAdjacent(@ForAll @IntRange(min = 0, max = 5) int vertexIndex) {
        Board b = new Board();
        Player p = player("P");
        b.setRobberLocation(new Location(3, 3));
        own(b, verticesOf(3, 3)[vertexIndex], p);
        assertEquals(List.of(p), b.getRobberAdjacentPlayers());
    }

    @Property(tries = 50)
    @Label("Ningún vértice de tierra libre rechaza un poblado inicial en un tablero vacío")
    void prop_anyLandVertexAcceptsFirstSettlement(@ForAll @IntRange(min = 0, max = 18) int landIndex,
            @ForAll @IntRange(min = 0, max = 1) int orientation) {
        Board b = new Board();
        Player p = player("P");
        assertTrue(b.placeStructureNoRoad(v(LAND[landIndex][0], LAND[landIndex][1], orientation), p));
    }
}