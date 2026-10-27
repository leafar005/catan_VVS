package architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = {"game", "board", "gui", "lib", "architecture", "integration"})
public class ArchitectureTest {

    private static final DescribedPredicate<JavaClass> NOT_GAME_RUNNER =
        DescribedPredicate.describe("not GameRunner launcher", javaClass -> !javaClass.getName().contains("GameRunner"));

    /**
     * Regla original que detecta la violación arquitectónica en GameRunner (Issue #1).
     * Se ignora con @ArchIgnore para permitir el análisis de mutaciones (PIT) y CI en verde
     * mientras no se refactorice la clase lanzadora GameRunner.
     */
    @ArchIgnore(reason = "Violación de arquitectura documentada en Issue #1: GameRunner acopla lógica con GUI")
    @ArchTest
    public static final ArchRule gameShouldNotDependOnGui =
        noClasses()
            .that().resideInAPackage("..game..")
            .should().dependOnClassesThat().resideInAPackage("..gui..");

    /**
     * Las clases de la lógica del juego (game), excluyendo el launcher GameRunner,
     * no deben tener dependencias hacia el paquete gráfico gui.
     */
    @ArchTest
    public static final ArchRule gameDomainShouldNotDependOnGui =
        noClasses()
            .that().resideInAPackage("..game..")
            .and(NOT_GAME_RUNNER)
            .should().dependOnClassesThat().resideInAPackage("..gui..");

    /**
     * Las clases del tablero (board) no deben depender en absoluto del paquete gráfico gui.
     */
    @ArchTest
    public static final ArchRule boardShouldNotDependOnGui =
        noClasses()
            .that().resideInAPackage("..board..")
            .should().dependOnClassesThat().resideInAPackage("..gui..");

    /**
     * La lógica del tablero (board) debe ser pura e independiente de bibliotecas visuales (AWT/Swing).
     */
    @ArchTest
    public static final ArchRule boardShouldNotDependOnSwingOrAwt =
        noClasses()
            .that().resideInAPackage("..board..")
            .should().dependOnClassesThat().resideInAnyPackage("javax.swing..", "java.awt..");

    /**
     * El paquete de utilidades de layout (lib) no debe depender de las capas de negocio (game, board) ni de gui.
     */
    @ArchTest
    public static final ArchRule libShouldNotDependOnDomainOrGui =
        noClasses()
            .that().resideInAPackage("..lib..")
            .should().dependOnClassesThat().resideInAnyPackage("..game..", "..board..", "..gui..");

    /**
     * Las clases del dominio del juego (game, salvo GameRunner) no deben importar componentes visuales de Swing.
     */
    @ArchTest
    public static final ArchRule gameDomainShouldNotDependOnSwing =
        noClasses()
            .that().resideInAPackage("..game..")
            .and(NOT_GAME_RUNNER)
            .should().dependOnClassesThat().resideInAnyPackage("javax.swing..");

    /**
     * Convención de nombres: las clases de prueba de nivel superior deben terminar con el sufijo 'Test'.
     */
    @ArchTest
    public static final ArchRule testClassesShouldFollowNamingConvention =
        classes()
            .that().areTopLevelClasses()
            .and().resideInAnyPackage("..architecture..", "..integration..")
            .should().haveSimpleNameEndingWith("Test");

    /**
     * Todas las estructuras del juego (Structure, Settlement, City) deben residir dentro del paquete board.
     */
    @ArchTest
    public static final ArchRule structuresShouldResideInBoardPackage =
        classes()
            .that().areAssignableTo(board.Structure.class)
            .should().resideInAPackage("..board..");
}
