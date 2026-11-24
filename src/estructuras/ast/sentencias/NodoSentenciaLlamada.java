// Stub con IR
package estructuras.ast.sentencias;

import estructuras.Token;
import estructuras.ast.expresiones.NodoExpPrimario;
import estructuras.ast.expresiones.NodoPrimario;
import estructuras.ts.tipos.TipoMetodo;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoSentenciaLlamada extends NodoSentencia {

    private Token identificador;
    private NodoExpPrimario llamada;
    private boolean necesitaPop;

    public NodoSentenciaLlamada(Token id, NodoExpPrimario llamada) {
        this.llamada = llamada;
        this.identificador = id;
        this.necesitaPop = false;
    }

    public NodoExpPrimario getLlamada() {
        return llamada;
    }

    public void setLlamada(NodoExpPrimario llamada) {
        this.llamada = llamada;
    }

    public Token getToken() {
        return identificador;
    }

    @Override
    public void chequear() throws ExcepcionSemantica {
        TipoMetodo tipoRetorno = llamada.chequear();

        NodoPrimario np = (NodoPrimario) llamada;

        if (!np.terminaEnLlamada())
            throw new ExcepcionSemanticaPersonalizada(identificador.getLinea(),
                    "La sentencia no termina en una llamada.", identificador);

        if (!tipoRetorno.esVoid()) {
            this.necesitaPop = true;
        }
    }

    /**
     * Genera IR para sentencia de llamada
     */
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Generar la llamada
        String resultado = llamada.generar(gci);

        // Si retorna valor, descartarlo (ya que es una sentencia, no expresión)
        // En IR esto se modela simplemente no usando el temporal resultante
        // No necesitamos hacer nada explícito
    }
}