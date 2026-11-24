package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionConstructorDuplicado extends ExcepcionSemantica {
    public ExcepcionConstructorDuplicado(String nom, Token token) {
        super("El constructor '"+nom+"' ya esta declarado.", token);
    }
}