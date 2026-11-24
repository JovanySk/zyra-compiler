// Contenido completo y CORREGIDO
package excepciones.semanticas;

@SuppressWarnings("serial")
public class ExcepcionFaltaMain extends ExcepcionSemantica {
	public ExcepcionFaltaMain() {
        // Este error es global y no se asocia a un token específico.
		super("Alguna clase debe tener el metodo estatico 'main' (sin parametros)", null);
	}
}