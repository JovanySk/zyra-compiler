// Stub estructural con IR
package estructuras.ast;

import estructuras.Token;
import estructuras.ast.sentencias.NodoBloque;
import estructuras.ast.sentencias.NodoSentencia;
import excepciones.semanticas.ExcepcionSemantica;
import estructuras.ts.tipos.TipoMetodo;
import estructuras.ts.variables.VarParametro;
import java.util.LinkedList;

public class NodoMetodo extends NodoSentencia {
    private Token token;
    private TipoMetodo tipoRetorno;
    private LinkedList<VarParametro> parametros;
    private NodoBloque bloque;
    private boolean esPublico;
    private boolean esEstatico;

    public NodoMetodo(Token token, TipoMetodo tipo, NodoBloque bloque, LinkedList<VarParametro> parametros,
            boolean esPublico, boolean esEstatico) {
        this.token = token;
        this.tipoRetorno = tipo;
        this.bloque = bloque;
        this.parametros = parametros;
        this.esPublico = esPublico;
        this.esEstatico = esEstatico;
    }

    public Token getToken() {
        return token;
    }

    public TipoMetodo getTipoRetorno() {
        return tipoRetorno;
    }

    public NodoBloque getBloque() {
        return bloque;
    }

    public LinkedList<VarParametro> getParametros() {
        return parametros;
    }

    public boolean esPublico() {
        return esPublico;
    }

    public boolean esEstatico() {
        return esEstatico;
    }

    @Override
    public void chequear() throws ExcepcionSemantica {
        // Chequeo delegado a tabla de símbolos
    }

    /**
     * Genera IR para un método, delimitado por BEGIN_FUNCTION / END_FUNCTION
     */
    @Override
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Delimitar la función en el IR: BEGIN_FUNCTION nombre, "p1,p2,..."
        StringBuilder nombresParams = new StringBuilder();
        if (parametros != null) {
            for (VarParametro p : parametros) {
                if (nombresParams.length() > 0) nombresParams.append(",");
                nombresParams.append(p.getToken().getLexema());
            }
        }
        gci.emitir("BEGIN_FUNCTION", token.getLexema(), nombresParams.toString(), "");
        if (bloque != null) {
            bloque.generar(gci);
        }
        gci.marcarFinFuncion(token.getLexema());
    }
}
