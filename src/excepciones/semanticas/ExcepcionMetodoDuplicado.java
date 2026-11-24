// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionMetodoDuplicado extends ExcepcionSemantica {
	public ExcepcionMetodoDuplicado(String nom, Token token) {
		super("El metodo '"+nom+"' ya esta declarado.", token);
	}
}