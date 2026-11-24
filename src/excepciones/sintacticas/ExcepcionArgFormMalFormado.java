// Contenido completo y CORREGIDO
package excepciones.sintacticas;

@SuppressWarnings("serial")
public class ExcepcionArgFormMalFormado extends ExcepcionSintactica {

	/**
	 * Constructor para un error en la formación de argumentos formales.
	 * @param linea Línea del error.
	 * @param columna Columna del error.
	 * @param encontrado El lexema del token que se encontró inesperadamente.
	 */
	public ExcepcionArgFormMalFormado(int linea, int columna, String encontrado) {
		// Se llama al constructor de la clase padre con un mensaje de error específico.
		super(linea, columna, "un argumento formal válido (ej: int x)", encontrado);
	}
}