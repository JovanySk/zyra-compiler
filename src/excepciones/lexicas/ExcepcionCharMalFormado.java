// Contenido completo y CORREGIDO
package excepciones.lexicas;

@SuppressWarnings("serial")
public class ExcepcionCharMalFormado extends ExcepcionLexica {
    public ExcepcionCharMalFormado(int linea, int columna) {
        super(String.format("[Línea: %d, Col: %d] Literal de carácter mal formado.", linea, columna), linea, columna);
    }
}