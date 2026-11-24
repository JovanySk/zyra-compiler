package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionAtributoDuplicado extends ExcepcionSemantica {
    public ExcepcionAtributoDuplicado(String nom, Token token) {
        super("El atributo '"+nom+"' ya esta declarado.", token);
    }
}