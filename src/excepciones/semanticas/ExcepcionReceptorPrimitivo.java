// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionReceptorPrimitivo extends ExcepcionSemantica {
	public ExcepcionReceptorPrimitivo(Token token) {
		super("El receptor de la llamada debe ser un tipo clase (y no tipo primitivo).", token);
	}
}