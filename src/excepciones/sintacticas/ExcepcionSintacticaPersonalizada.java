// Contenido completo y actualizado de ExcepcionSintacticaPersonalizada.java
package excepciones.sintacticas;

@SuppressWarnings("serial")
public class ExcepcionSintacticaPersonalizada extends ExcepcionSintactica {
	
	public ExcepcionSintacticaPersonalizada (int linea, int columna, String mensajeGeneral) {
		super(linea, columna, mensajeGeneral, "una estructura inesperada");
	}

	public ExcepcionSintacticaPersonalizada (int linea, int columna, String mensajeGeneral, String encontrado) {
		super(linea, columna, mensajeGeneral, encontrado);
	}
}