package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionAtributoInexistente extends ExcepcionSemantica {
    public ExcepcionAtributoInexistente(String nom, Token token) {
        super("La variable de instancia '"+nom+"' no existe.", token);
    }
}