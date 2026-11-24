// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionMetodoMalHeredado extends ExcepcionSemantica {
	public ExcepcionMetodoMalHeredado(String nom, Token token) {
		super("El metodo '"+nom+"' no posee la misma signatura que su ancestro.", token);
	}
}