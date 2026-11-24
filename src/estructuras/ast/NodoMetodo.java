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
     * STUB: Genera IR para un método
     */
    @Override
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Generar IR para el bloque del método
        if (bloque != null) {
            bloque.generar(gci);
        }
    }
}
