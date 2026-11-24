package excepciones.lexicas;

@SuppressWarnings("serial")
public class ExcepcionPalabraReservadaMayuscula extends ExcepcionLexica {
    public ExcepcionPalabraReservadaMayuscula(int linea, int columna, String lexema) {
        super(String.format("[Línea: %d, Col: %d] La palabra reservada '%s' debe estar escrita completamente en minúsculas.", linea, columna, lexema.toLowerCase()), linea, columna);
    }
}