Settlers of Catan
================

This project is a Java implementation of the classic board game, "Settlers of Catan."   
Graphics make use of the Swing libraries.  
Project runs one game with input for four players (and open hand information), while fully automating all steps that don't require user input.

Much of the UI/UX could be improved.
For a true multiplayer experience, networking capabilities would be needed to support multiple devices, or player-state could be hidden for a pass-and-play approach.

![An early game screenshot from this Catan project.](./gameplay-demo.png)

EJECUCIÓN DEL PROGRAMA:

Primera vez:
cd "c:/software/informatica/4 curso/1 cuatri/VVS/catan_VVS"
mkdir out
javac -d out $(Get-ChildItem -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)

Ejecución:
java -cp out game.GameRunner

DIRECTORIOS A REVISAR:

Paquete	     Archivos	Líneas	¿Entra en las pruebas?
gui     	    7	    2.846	❌ Fuera (según el profesor)
board       	11	    1.395	✅
game        	3	    990	    ✅
lib         	1	    346	    ❌ Fuera (utilidad visual)
Total a probar	14  	~2.385	—

HERRAMIENTAS:

jqwik / tests con JUnit5 → centrados en clases de board y game.
ArchUnit → aquí sí puede tener sentido mirar el proyecto entero, aunque sea de refilón: por ejemplo, comprobar que board/game no dependen de clases de gui (buena práctica de capas), sin necesidad de "probar" la GUI en sí.
CheckStyle / SpotBugs → son análisis estático, casi gratis de ejecutar; podéis pasarlos por todo el repo si queréis (incluida la GUI) como añadido, pero no es obligatorio si el profesor solo pide pruebas de lógica.
JaCoCo → conviene limitar la cobertura a board y game en la config, excluyendo gui y lib.
PIT → igual, restringir targetClasses a esos dos paquetes (board y game) para evitar problemas de cobertura.