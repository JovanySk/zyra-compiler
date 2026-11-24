// Contenido completo y CORREGIDO
package excepciones.lexicas;

@SuppressWarnings("serial")
public class ExcepcionStringMalFormado extends ExcepcionLexica {
    public ExcepcionStringMalFormado(int linea, int columna) {
        super(String.format("[Línea: %d, Col: %d] Literal String mal formado (no se cerró antes del fin de línea o del archivo).", linea, columna), linea, columna);
    }
}