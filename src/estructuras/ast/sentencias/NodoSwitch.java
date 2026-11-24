// Stub con IR
package estructuras.ast.sentencias;

import estructuras.ast.expresiones.NodoExpresion;
import excepciones.semanticas.ExcepcionSemantica;
import java.util.LinkedList;

public class NodoSwitch extends NodoSentencia {

    private NodoExpresion expresion;
    private LinkedList<NodoCase> casos;

    public NodoSwitch(NodoExpresion expresion, LinkedList<NodoCase> casos) {
        this.expresion = expresion;
        this.casos = casos;
    }

    public NodoExpresion getExpresion() {
        return expresion;
    }

    public LinkedList<NodoCase> getCasos() {
        return casos;
    }

    @Override
    public void chequear() throws ExcepcionSemantica {
        expresion.chequear();
        for (NodoCase caso : casos) {
            caso.chequear();
        }
    }

    /**
     * Stub: Genera IR para switch (simplificado)
     */
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // TODO: Implementar switch completo con IR
        // Por ahora, stub simple que evalúa casos secuencialmente
        String valor = expresion.generar(gci);

        for (NodoCase caso : casos) {
            caso.generar(gci);
        }
    }
}
