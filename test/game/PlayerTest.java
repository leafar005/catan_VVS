package game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

class PlayerTest {

    private Player player;

    @BeforeEach
    void setUp() {
        // Inicializamos un jugador fresco antes de cada test para aislar las pruebas (regla de oro)
        player = new Player("TestPlayer", Color.BLUE);
    }

    // --- TESTEANDO: setHasLargestArmy ---
    // La rúbrica pide: "máis de un para cada funcionalidade pública" y "contén só un assert"

    @Test
    @DisplayName("Dar Largest Army a quien no lo tiene incrementa Victory Points")
    void setHasLargestArmy_FalseToTrue_IncreasesVP() {
        int initialVp = player.getVictoryPoints();
        
        player.setHasLargestArmy(true);
        
        assertEquals(initialVp + 1, player.getVictoryPoints());
    }

    @Test
    @DisplayName("Dar Largest Army a quien ya lo tiene mantiene Victory Points intactos")
    void setHasLargestArmy_TrueToTrue_KeepsVP() {
        player.setHasLargestArmy(true); // Estado inicial: ya lo tiene
        int vpAfterFirstTime = player.getVictoryPoints();
        
        player.setHasLargestArmy(true); // Intentar darlo de nuevo
        
        assertEquals(vpAfterFirstTime, player.getVictoryPoints());
    }

    @Test
    @DisplayName("Quitar Largest Army a quien lo tiene decrementa Victory Points")
    void setHasLargestArmy_TrueToFalse_DecreasesVP() {
        player.setHasLargestArmy(true); // Estado inicial: lo tiene
        int vpWithArmy = player.getVictoryPoints();
        
        player.setHasLargestArmy(false); // Quitarselo
        
        assertEquals(vpWithArmy - 1, player.getVictoryPoints());
    }
    
    // --- TESTEANDO: hasResources (Valores Frontera y Particiones Equivalentes) ---

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
        player.setNumberResourcesType("WOOL", 1); // Le falta una lana
        
        ArrayList<String> cost = new ArrayList<>(Arrays.asList("ORE", "WOOL", "WOOL"));
        
        assertFalse(player.hasResources(cost));
    }

    // --- TESTEANDO ALEATORIEDAD CON jqwik (Property-based testing) ---
    // La rúbrica pide: "diferentes datos de entrada elixidos de xeito estructurado e diverso (aleatorios)"

    @Property
    @DisplayName("Añadir un número aleatorio de BRICK actualiza el contador total correctamente")
    void addResources_RandomAmount_UpdatesTotal(@ForAll @IntRange(min = 1, max = 50) int randomAmount) {
        int initialTotal = player.getTotalResources(); 
        
        ArrayList<String> randomResources = new ArrayList<>();
        for (int i = 0; i < randomAmount; i++) {
            randomResources.add("BRICK");
        }
        
        player.addResources(randomResources);
        
        assertEquals(initialTotal + randomAmount, player.getTotalResources());
    }
}
