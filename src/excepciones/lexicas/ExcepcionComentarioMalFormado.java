// Contenido completo y CORREGIDO
package excepciones.lexicas;

@SuppressWarnings("serial")
public class ExcepcionComentarioMalFormado extends ExcepcionLexica {
    public ExcepcionComentarioMalFormado(int linea, int columna) {
        super(String.format("[Línea: %d, Col: %d] Comentario multilinea no cerrado.", linea, columna), linea, columna);
    }
}