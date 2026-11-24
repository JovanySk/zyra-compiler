// Stub con IR
package estructuras.ast.sentencias;

import excepciones.semanticas.ExcepcionSemantica;
import java.util.LinkedList;

public class NodoSentenciaCompuesta extends NodoSentencia {

    private final LinkedList<NodoSentencia> sentencias;

    public NodoSentenciaCompuesta(LinkedList<NodoSentencia> sentencias) {
        this.sentencias = sentencias;
    }

    public LinkedList<NodoSentencia> getSentencias() {
        return sentencias;
    }

    @Override
    public void chequear() throws ExcepcionSemantica {
        for (NodoSentencia s : sentencias) {
            s.chequear();
        }
    }

    /**
     * Genera IR para cada sentencia
     */
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        for (NodoSentencia s : sentencias) {
            s.generar(gci);
        }
    }
}