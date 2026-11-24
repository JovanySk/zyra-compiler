// Stub con IR
package estructuras.ast.sentencias;

import excepciones.semanticas.ExcepcionSemantica;

public class NodoSentenciaVacia extends NodoSentencia {

    @Override
    public void chequear() throws ExcepcionSemantica {
        // No hace nada
    }

    /**
     * Sentencia vacía no genera IR
     */
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // No genera código
    }
}