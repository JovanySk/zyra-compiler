// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionHerenciaPropia extends ExcepcionSemantica {
	public ExcepcionHerenciaPropia(String nom, Token token) {
		super("La clase '"+nom+"' no puede heredar de si misma.", token);
	}
}