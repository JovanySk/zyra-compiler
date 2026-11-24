// Stub con IR
package estructuras.ast.sentencias;

import estructuras.ast.expresiones.NodoLiteral;
import excepciones.semanticas.ExcepcionSemantica;
import java.util.LinkedList;

public class NodoCase extends NodoSentencia {

    private NodoLiteral valor;
    private LinkedList<NodoSentencia> sentencias;

    public NodoCase(NodoLiteral valor, LinkedList<NodoSentencia> sentencias) {
        this.valor = valor;
        this.sentencias = sentencias;
    }

    public NodoLiteral getValor() {
        return valor;
    }

    public LinkedList<NodoSentencia> getSentencias() {
        return sentencias;
    }

    public boolean esDefault() {
        return valor == null;
    }

    @Override
    public void chequear() throws ExcepcionSemantica {
        for (NodoSentencia s : sentencias) {
            s.chequear();
        }
    }

    /**
     * Stub: Genera IR para case
     */
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Generar sentencias del case
        for (NodoSentencia s : sentencias) {
            s.generar(gci);
        }
    }
}
