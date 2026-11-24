// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionReceptorVoid extends ExcepcionSemantica {
	public ExcepcionReceptorVoid(Token token) {
		super("El receptor de la llamada debe ser un tipo clase (y no tipo void).", token);
	}
}