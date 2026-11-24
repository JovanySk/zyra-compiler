// Contenido completo y CORREGIDO
package excepciones.sintacticas;

@SuppressWarnings("serial")
public class ExcepcionExprMalFormada extends ExcepcionSintactica {

	/**
	 * Constructor para una expresión mal formada.
	 * @param linea Línea del error.
	 */
	public ExcepcionExprMalFormada(int linea) {
        // Llama al constructor de la clase padre con un mensaje genérico.
        // Se mantiene por compatibilidad, aunque es mejor usar uno más detallado.
		super(linea, 0, "una expresión válida", "estructura desconocida");
	}
}