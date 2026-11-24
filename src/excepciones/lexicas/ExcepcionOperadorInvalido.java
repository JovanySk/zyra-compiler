// Contenido completo y CORREGIDO
package excepciones.lexicas;

@SuppressWarnings("serial")
public class ExcepcionOperadorInvalido extends ExcepcionLexica {
    public ExcepcionOperadorInvalido(int linea, int columna, char chr) {
        super(String.format("[Línea: %d, Col: %d] Operador inválido. Se encontró un '%c' pero se esperaba otro igual para formar '&&' o '||'.", linea, columna, chr), linea, columna);
    }
}