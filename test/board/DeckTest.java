package board;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DeckTest - Pruebas unitarias de la baraja de cartas de desarrollo")
public class DeckTest {

    private Deck deck;

    @BeforeEach
    void setUp() {
        deck = new Deck();
    }

    @Test
    @DisplayName("La baraja inicial contiene exactamente 25 cartas con la distribución oficial de Catan")
    void deckInitialization_containsExactly25CardsWithCorrectDistribution() {
        Map<String, Integer> typeCounts = new HashMap<>();
        Map<String, Integer> progressCounts = new HashMap<>();

        for (int i = 0; i < 25; i++) {
            DevCard card = deck.draw();
            assertNotNull(card, "Cada una de las primeras 25 cartas extraídas no debe ser null");
            typeCounts.put(card.getType(), typeCounts.getOrDefault(card.getType(), 0) + 1);

            if ("Progress".equals(card.getType())) {
                progressCounts.put(card.getSubType(), progressCounts.getOrDefault(card.getSubType(), 0) + 1);
            }
        }

        assertEquals(14, typeCounts.get("Knight"), "Debe haber 14 cartas de Caballero");
        assertEquals(5, typeCounts.get("Victory Point"), "Debe haber 5 cartas de Punto de Victoria");
        assertEquals(6, typeCounts.get("Progress"), "Debe haber 6 cartas de Progreso");

        assertEquals(2, progressCounts.get("Road building"), "Debe haber 2 cartas de Construcción de carreteras");
        assertEquals(2, progressCounts.get("Year of plenty"), "Debe haber 2 cartas de Año de la abundancia");
        assertEquals(2, progressCounts.get("Monopoly"), "Debe haber 2 cartas de Monopolio");
    }

    @Test
    @DisplayName("Extraer cartas reduce la baraja y al vaciarse draw devuelve null")
    void draw_returnsNullWhenDeckIsEmpty() {
        for (int i = 0; i < 25; i++) {
            deck.draw();
        }

        assertNull(deck.draw(), "Extraer de una baraja vacía debe devolver null");
    }

    @Test
    @DisplayName("isEmpty refleja el estado de la baraja antes y después de vaciarla")
    void isEmpty_reflectsDeckStateAccurately() {
        assertFalse(deck.isEmpty(), "Una baraja recién creada no debe estar vacía");

        for (int i = 0; i < 24; i++) {
            deck.draw();
            assertFalse(deck.isEmpty(), "La baraja aún no debe estar vacía con cartas pendientes");
        }

        deck.draw(); // Extrae la última carta (número 25)
        assertTrue(deck.isEmpty(), "La baraja debe indicar que está vacía tras extraer las 25 cartas");
    }
}
