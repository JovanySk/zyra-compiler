package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionConstructorMalNombrado extends ExcepcionSemantica {
    public ExcepcionConstructorMalNombrado(String nom, Token token) {
        super("El nombre del constructor '"+nom+"' no corresponde al nombre de clase.", token);
    }
}