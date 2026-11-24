package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionParametrosNoCoinciden extends ExcepcionSemantica {
	public ExcepcionParametrosNoCoinciden(Token token) {
		super("La cantidad de parametros no coincide.", token);
	}
}