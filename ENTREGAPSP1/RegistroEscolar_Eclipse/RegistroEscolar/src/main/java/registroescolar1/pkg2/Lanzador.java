package registroescolar1.pkg2;

/**
 * Punto de entrada para ejecutar desde Eclipse (y desde un .jar).
 * Una clase que hereda de Application no arranca desde el classpath
 * ("Faltan componentes de JavaFX"); esta clase intermedia lo evita.
 */
public final class Lanzador {

    private Lanzador() {
    }

    public static void main(String[] args) {
        RegistroEscolar12.main(args);
    }
}
