// Stub con IR
package estructuras.ast.sentencias;

import java.util.HashMap;
import java.util.LinkedList;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.variables.VarLocal;
import estructuras.ts.variables.Variable;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import excepciones.semanticas.ExcepcionVarLocalDuplicada;
import modulos.GenCod;
import estructuras.Token;
import estructuras.ast.expresiones.NodoExpresion;

public class NodoBloque extends NodoSentencia {

    private LinkedList<NodoSentencia> sentencias;
    private HashMap<String, Variable> varsBloque;

    public NodoBloque() {
        sentencias = new LinkedList<>();
        varsBloque = new HashMap<>();
    }

    public NodoBloque(LinkedList<NodoSentencia> listaSent) {
        sentencias = listaSent;
        varsBloque = new HashMap<>();
    }

    public LinkedList<NodoSentencia> getSentencias() {
        return sentencias;
    }

    public void agregarVariable(VarLocal var) throws ExcepcionVarLocalDuplicada {
        if (varsBloque.containsKey(var.getToken().getLexema())) {
            throw new ExcepcionVarLocalDuplicada(var.getToken().getLexema(), var.getToken());
        }
        varsBloque.put(var.getToken().getLexema(), var);
    }

    @Override
    public void chequear() throws ExcepcionSemantica {
        NodoBloque anidado = TablaSimbolos.bloqueActual;
        TablaSimbolos.bloqueActual = this;

        boolean returnEncontrado = false;
        for (NodoSentencia sent : sentencias) {
            if (returnEncontrado) {
                Token tokenError = obtenerTokenDeSentencia(sent);
                throw new ExcepcionSemanticaPersonalizada(tokenError.getLinea(),
                        "Código inalcanzable detectado después de una sentencia 'return'.", tokenError);
            }
            sent.chequear();
            if (sent instanceof NodoReturn) {
                returnEncontrado = true;
            }
        }

        HashMap<String, Variable> tablaVariablesMetodo = TablaSimbolos.metodoActual.getTablaVariables();
        for (String nombreVar : varsBloque.keySet()) {
            tablaVariablesMetodo.remove(nombreVar);
        }

        TablaSimbolos.bloqueActual = anidado;
    }

    /**
     * Genera IR para bloque
     */
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Generar cada sentencia del bloque
        for (NodoSentencia s : sentencias) {
            s.generar(gci);
        }

        // Las variables locales se manejan automáticamente por el scope
        // No necesitamos FMEM explícito en IR
    }

    private Token obtenerTokenDeSentencia(NodoSentencia s) {
        if (s instanceof NodoAsignacion)
            return ((NodoAsignacion) s).getLadoIzq().getToken();
        if (s instanceof NodoReturn)
            return ((NodoReturn) s).getToken();
        if (s instanceof NodoDeclaracion)
            return ((NodoDeclaracion) s).getVariables().getFirst().getToken();
        if (s instanceof NodoSentenciaLlamada)
            return ((NodoSentenciaLlamada) s).getToken();
        if (s instanceof NodoIf)
            return ((NodoIf) s).getToken();
        if (s instanceof NodoWhile)
            return ((NodoWhile) s).getCondicion().getToken();
        if (s instanceof NodoFor)
            return ((NodoFor) s).getTokenFor();
        if (s instanceof NodoSentenciaCompuesta) {
            if (!((NodoSentenciaCompuesta) s).getSentencias().isEmpty()) {
                return obtenerTokenDeSentencia(((NodoSentenciaCompuesta) s).getSentencias().getFirst());
            }
        }
        if (s instanceof NodoBloque) {
            if (!((NodoBloque) s).getSentencias().isEmpty()) {
                return obtenerTokenDeSentencia(((NodoBloque) s).getSentencias().getFirst());
            }
        }
        return new Token("unknown", "sentencia", 0, 0);
    }
}
