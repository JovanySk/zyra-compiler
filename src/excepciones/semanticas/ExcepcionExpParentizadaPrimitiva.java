// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionExpParentizadaPrimitiva extends ExcepcionSemantica {
    public ExcepcionExpParentizadaPrimitiva(Token token) {
        super("La expresion parentizada no puede ser de un tipo primitivo.", token);
    }
}