package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionClaseDeclarada extends ExcepcionSemantica {
    public ExcepcionClaseDeclarada(String nom, Token token) {
        super("La clase '"+nom+"' ya esta declarada.", token);
    }
}