// Stub estructural con IR
package estructuras.ast;

import estructuras.Token;
import estructuras.ts.tipos.Tipo;
import excepciones.semanticas.ExcepcionSemantica;
import estructuras.ast.sentencias.NodoSentencia;

public class NodoAtributo extends NodoSentencia {
    private Token token;
    private Tipo tipo;
    private boolean esPublico;

    public NodoAtributo(Token token, Tipo tipo, boolean esPublico) {
        this.token = token;
        this.tipo = tipo;
        this.esPublico = esPublico;
    }

    public Token getToken() {
        return token;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public boolean esPublico() {
        return esPublico;
    }

    @Override
    public void chequear() throws ExcepcionSemantica {
        /* No requiere chequeo individual */ }

    /**
     * Atributos no generan IR directamente
     */
    @Override
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Los atributos no generan código directamente
    }
}
