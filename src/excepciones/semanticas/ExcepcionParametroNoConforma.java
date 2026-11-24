package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionParametroNoConforma extends ExcepcionSemantica {
	public ExcepcionParametroNoConforma(String nom, Token token) {
		super("Un parametro actual en la llamada del metodo '"+nom+"' no conforma con el formal.", token);
	}
}