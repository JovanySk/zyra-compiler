// Contenido completo y ACTUALIZADO
package excepciones.lexicas;

@SuppressWarnings("serial")
public class ExcepcionLexica extends Exception {
    
    private final int linea;
    private final int columna;

    public ExcepcionLexica(String message, int linea, int columna) {
        super(message);
        this.linea = linea;
        this.columna = columna;
    }
    
    public int getLinea() {
        return linea;
    }

    public int getColumna() {
        return columna;
    }
}