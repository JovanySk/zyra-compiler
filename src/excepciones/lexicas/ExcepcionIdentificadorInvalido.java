package excepciones.lexicas;

@SuppressWarnings("serial")
public class ExcepcionIdentificadorInvalido extends ExcepcionLexica {
    public ExcepcionIdentificadorInvalido(int linea, int columna, String lexema, String sugerencia) {
        super(String.format("[Línea: %d, Col: %d] El identificador '%s' es inválido. Es una clase del sistema y debe empezar con mayúscula ('%s').", linea, columna, lexema, sugerencia), linea, columna);
    }
}