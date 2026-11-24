// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionSemantica extends Exception {
	
    private Token token;

	public ExcepcionSemantica (String msg, Token token) {
		super(msg);
        this.token = token;
	}
    
    public Token getToken() {
        return token;
    }
}