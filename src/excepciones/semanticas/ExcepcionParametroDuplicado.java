// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionParametroDuplicado extends ExcepcionSemantica {
	public ExcepcionParametroDuplicado(String nom, Token token) {
		super("Ya existe un parametro con el nombre '"+nom+"'.", token);
	}
}