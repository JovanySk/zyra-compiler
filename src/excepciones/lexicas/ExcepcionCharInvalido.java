// Contenido completo y CORREGIDO
package excepciones.lexicas;

@SuppressWarnings("serial")
public class ExcepcionCharInvalido extends ExcepcionLexica {
    public ExcepcionCharInvalido(int linea, int columna, char chr) {
        super(String.format("[Línea: %d, Col: %d] Carácter inválido '%c' encontrado.", linea, columna, chr), linea, columna);
    }
}