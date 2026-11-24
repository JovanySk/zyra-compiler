// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionVarLocalDuplicada extends ExcepcionSemantica {
	public ExcepcionVarLocalDuplicada(String nom, Token token) {
		super("La variable local '"+nom+"' ya fue declarada.", token);
	}
}