package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionExpUnMalFormada extends ExcepcionSemantica {

	public ExcepcionExpUnMalFormada (String msj, Token token) {
		super(msj, token);
	}

	public ExcepcionExpUnMalFormada (String op, String permitidos, Token token) {
		super("El operador unario "+op+" solo permite "+permitidos+".", token);
	}
}