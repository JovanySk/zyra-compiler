// Stub estructural con IR
package estructuras.ast;

import estructuras.Token;
import estructuras.ts.Constructor;
import estructuras.ts.TablaSimbolos;
import excepciones.semanticas.ExcepcionSemantica;
import estructuras.ast.sentencias.NodoSentencia;
import java.util.LinkedList;
import estructuras.ts.Metodo;

public class NodoClase extends NodoSentencia {
    private Token token;
    private LinkedList<NodoAtributo> atributos;
    private LinkedList<NodoSentencia> constructores;
    private LinkedList<NodoMetodo> metodos;

    public NodoClase(Token token) {
        this.token = token;
        this.atributos = new LinkedList<>();
        this.constructores = new LinkedList<>();
        this.metodos = new LinkedList<>();
    }

    public Token getToken() {
        return token;
    }

    public LinkedList<NodoAtributo> getAtributos() {
        return atributos;
    }

    public LinkedList<NodoSentencia> getConstructores() {
        return constructores;
    }

    public LinkedList<NodoMetodo> getMetodos() {
        return metodos;
    }

    public void agregarAtributo(NodoAtributo attr) {
        this.atributos.add(attr);
    }

    public void agregarConstructor(NodoSentencia ctor) {
        this.constructores.add(ctor);
    }

    public void agregarMetodo(NodoMetodo met) {
        this.metodos.add(met);
    }

    @Override
    public void chequear() throws ExcepcionSemantica {
        TablaSimbolos.claseActual = TablaSimbolos.clases.get(this.getToken().getLexema());

        if (!constructores.isEmpty() && constructores.getFirst() != null) {
            TablaSimbolos.metodoActual = TablaSimbolos.claseActual.getConstructor();
            constructores.getFirst().chequear();
        }

        for (NodoMetodo m : metodos) {
            Metodo metodoEnTS = null;
            if (TablaSimbolos.claseActual.getMetodos().containsKey(m.getToken().getLexema())) {
                for (Metodo sobrecarga : TablaSimbolos.claseActual.getMetodos().get(m.getToken().getLexema())) {
                    if (sobrecarga.getListaParametros().size() == m.getParametros().size()) {
                        metodoEnTS = sobrecarga;
                        break;
                    }
                }
            }

            if (metodoEnTS != null) {
                TablaSimbolos.metodoActual = metodoEnTS;
                m.chequear();
            }
        }
    }

    /**
     * STUB: Genera IR para una clase (delegado a métodos individuales)
     */
    @Override
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Las clases en sí no generan IR directamente
        // La IR se genera a nivel de método/función
        // Este stub existe para mantener la jerarquía consistente

        // Generar IR para constructor si existe
        if (!constructores.isEmpty() && constructores.getFirst() != null) {
            constructores.getFirst().generar(gci);
        }

        // Generar IR para cada método
        for (NodoMetodo m : metodos) {
            m.generar(gci);
        }
    }
}
