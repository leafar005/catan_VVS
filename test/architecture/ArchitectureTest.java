package architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = {"game", "board", "gui", "lib"})
public class ArchitectureTest {

    @ArchTest
    public static final ArchRule gameShouldNotDependOnGui =
        noClasses()
            .that().resideInAPackage("..game..")
            .should().dependOnClassesThat().resideInAPackage("..gui..");
            
    @ArchTest
    public static final ArchRule boardShouldNotDependOnGui =
        noClasses()
            .that().resideInAPackage("..board..")
            .should().dependOnClassesThat().resideInAPackage("..gui..");
}
