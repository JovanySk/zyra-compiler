// Contenido completo y CORREGIDO
package excepciones.semanticas;

import estructuras.Token;

@SuppressWarnings("serial")
public class ExcepcionAtributoYaNombrado extends ExcepcionSemantica {
    public ExcepcionAtributoYaNombrado(String nom, Token token) {
        super("El nombre de atributo '"+nom+"' es el mismo que el de algun metodo o el de la clase.", token);
    }
}