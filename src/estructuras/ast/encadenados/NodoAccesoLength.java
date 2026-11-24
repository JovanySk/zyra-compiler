// Stub final con IR
package estructuras.ast.encadenados;

import estructuras.Token;
import estructuras.ts.tipos.TipoMetodo;
import excepciones.semanticas.ExcepcionSemantica;

public class NodoAccesoLength extends NodoEncadenado {

    public NodoAccesoLength(Token token) {
        super(token);
    }

    @Override
    public TipoMetodo chequear(TipoMetodo receptor) throws ExcepcionSemantica {
        // Retorna int (tamaño del array)
        return new estructuras.ts.tipos.TipoInt();
    }

    /**
     * STUB: Genera IR para .length de arrays
     */
    @Override
    public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Acceso a la longitud de un array
        String temp = gci.nuevoTemporal();
        gci.emitir("length", "", "", temp);
        return temp;
    }

    @Override
    public boolean terminaEnVar() {
        return false;
    }

    @Override
    public boolean terminaEnLlamada() {
        return false;
    }
}