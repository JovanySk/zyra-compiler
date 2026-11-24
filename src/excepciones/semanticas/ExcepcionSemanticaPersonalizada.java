// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionSemanticaPersonalizada extends ExcepcionSemantica {

    public ExcepcionSemanticaPersonalizada(int linea, String msj, Token token) {
        super(msj, token);
    }
}
