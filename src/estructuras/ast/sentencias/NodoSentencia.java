// VERSIÓN ACTUALIZADA para IR
package estructuras.ast.sentencias;

import excepciones.semanticas.ExcepcionSemantica;

public abstract class NodoSentencia {

    public abstract void chequear() throws ExcepcionSemantica;

    /**
     * Genera código intermedio para esta sentencia
     * 
     * @param gci Generador de código intermedio
     */
    public abstract void generar(modulos.ir.GeneradorCodigoIntermedio gci);
}