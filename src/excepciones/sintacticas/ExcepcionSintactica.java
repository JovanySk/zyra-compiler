// Contenido completo y ACTUALIZADO
package excepciones.sintacticas;

@SuppressWarnings("serial")
public class ExcepcionSintactica extends Exception {
	
    private final int linea;
    private final int columna;
    private final String esperado;
    private final String encontrado;

    public ExcepcionSintactica(int linea, int columna, String esperado, String encontrado) {
        super(String.format("[Línea: %d, Col: %d] Se encontró '%s' pero se esperaba %s.", 
                             linea, columna, encontrado, esperado));
        this.linea = linea;
        this.columna = columna;
        this.esperado = esperado;
        this.encontrado = encontrado;
    }

    public int getLineaError() {
        return linea;
    }

    public int getColumnaError() {
        return columna;
    }

    public String getEsperado() {
        return esperado;
    }

    public String getEncontrado() {
        return encontrado;
    }
}