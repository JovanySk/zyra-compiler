// Stub estructura con IR
package estructuras.ast;

import java.util.LinkedList;
import estructuras.ts.TablaSimbolos;
import excepciones.semanticas.ExcepcionSemantica;
import estructuras.ast.sentencias.NodoSentencia;
import estructuras.ts.Clase;
import modulos.GenCod;

public class NodoPrograma extends NodoSentencia {
    private LinkedList<NodoClase> clases;

    public NodoPrograma() {
        this.clases = new LinkedList<>();
    }

    public void agregarClase(NodoClase clase) {
        this.clases.add(clase);
    }

    public LinkedList<NodoClase> getClases() {
        return clases;
    }

    @Override
    public void chequear() throws ExcepcionSemantica {
        for (NodoClase clase : clases) {
            TablaSimbolos.claseActual = TablaSimbolos.clases.get(clase.getToken().getLexema());
            clase.chequear();
        }
    }

    /**
     * Genera IR para todo el programa
     * NOTA: Este es el punto de entrada para la generación de IR
     */
    @Override
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // El programa coordina la generación de IR para todas las clases
        for (NodoClase clase : clases) {
            TablaSimbolos.claseActual = TablaSimbolos.clases.get(clase.getToken().getLexema());
            if (TablaSimbolos.claseActual != null) {
                clase.generar(gci);
            }
        }
    }
}
