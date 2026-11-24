// Contenido completo y CORREGIDO
package excepciones.sintacticas;

@SuppressWarnings("serial")
public class ExcepcionLiteralFaltante extends ExcepcionSintactica {

	/**
	 * Constructor para cuando se esperaba un literal.
	 * @param linea Línea del error.
	 * @param columna Columna del error.
	 * @param encontrado El lexema del token que se encontró en su lugar.
	 */
	public ExcepcionLiteralFaltante (int linea, int columna, String encontrado) {
		super(linea, columna, "un literal (número, string, true, false, null)", encontrado);
	}
}