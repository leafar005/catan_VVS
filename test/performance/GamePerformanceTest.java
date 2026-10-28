package performance;

import board.Board;
import etm.core.configuration.BasicEtmConfigurator;
import etm.core.configuration.EtmManager;
import etm.core.monitor.EtmMonitor;
import etm.core.monitor.EtmPoint;
import etm.core.renderer.SimpleTextRenderer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GamePerformanceTest - Pruebas no funcionales de rendimiento y profiling con JETM")
public class GamePerformanceTest {

    private static EtmMonitor monitor;

    @BeforeAll
    static void initMonitor() {
        BasicEtmConfigurator.configure();
        monitor = EtmManager.getEtmMonitor();
        monitor.start();
    }

    @AfterAll
    static void tearDownMonitor() {
        if (monitor != null) {
            StringWriter writer = new StringWriter();
            monitor.render(new SimpleTextRenderer(writer));
            System.out.println("\n========== INFORME DE RENDIMIENTO JETM ==========\n" + writer);
            monitor.stop();
        }
    }

    @Test
    @DisplayName("Rendimiento en la instanciación repetida del tablero (Board)")
    void profileBoardCreationPerformance() {
        long startTime = System.currentTimeMillis();
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            EtmPoint point = monitor.createPoint("Board::Instantiation");
            try {
                Board board = new Board();
                assertNotNull(board.getTiles());
            } finally {
                point.collect();
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        double avgTime = (double) totalTime / iterations;

        assertTrue(avgTime < 50.0, "La creación media del tablero debe ser inferior a 50 ms (obtenido: " + avgTime + " ms)");
    }

    @Test
    @DisplayName("Rendimiento en el reparto de recursos (Board.distributeResources)")
    void profileResourceDistributionPerformance() {
        Board board = new Board();
        long startTime = System.currentTimeMillis();
        int iterations = 500;

        for (int i = 0; i < iterations; i++) {
            int roll = (i % 11) + 2; // Tiradas de 2 a 12
            EtmPoint point = monitor.createPoint("Board::distributeResources");
            try {
                board.distributeResources(roll);
            } finally {
                point.collect();
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        double avgTime = (double) totalTime / iterations;

        assertTrue(avgTime < 5.0, "El reparto medio de recursos debe ser inferior a 5 ms (obtenido: " + avgTime + " ms)");
    }
}
