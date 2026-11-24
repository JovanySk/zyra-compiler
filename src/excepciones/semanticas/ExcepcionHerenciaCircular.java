package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionHerenciaCircular extends ExcepcionSemantica {
	public ExcepcionHerenciaCircular(String nom, Token token) {
		super("La clase '"+nom+"' tiene herencia circular.", token);
	}
}