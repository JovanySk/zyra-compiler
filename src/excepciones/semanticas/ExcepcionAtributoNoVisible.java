package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionAtributoNoVisible extends ExcepcionSemantica {
    public ExcepcionAtributoNoVisible(String nom, Token token) {
        super("La variable de instancia '"+nom+"' no es visible.", token);
    }
}