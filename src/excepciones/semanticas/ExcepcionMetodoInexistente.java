package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionMetodoInexistente extends ExcepcionSemantica {
	public ExcepcionMetodoInexistente(String metodo, String claseReceptora, Token token) {
		super("El metodo '"+metodo+"' no existe para la clase '"+claseReceptora+"'.", token);
	}
}