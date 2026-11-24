// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionClaseNoDeclarada extends ExcepcionSemantica {

	public ExcepcionClaseNoDeclarada(String nom, Token token) {
		super("La clase '"+nom+"' no esta declarada.", token);
	}
}