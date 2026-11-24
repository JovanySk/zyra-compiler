// Stub final con IR
package estructuras.ast.encadenados;

import java.util.HashMap;
import estructuras.Token;
import estructuras.ts.Clase;
import estructuras.ts.TablaSimbolos;
import estructuras.ts.tipos.Tipo;
import estructuras.ts.tipos.TipoMetodo;
import estructuras.ts.variables.VarInstancia;
import excepciones.semanticas.ExcepcionAtributoInexistente;
import excepciones.semanticas.ExcepcionAtributoNoVisible;
import excepciones.semanticas.ExcepcionReceptorPrimitivo;
import excepciones.semanticas.ExcepcionReceptorVoid;
import excepciones.semanticas.ExcepcionSemantica;
import modulos.GenCod;

public class NodoIdEncadenado extends NodoEncadenado {

    private VarInstancia atributo;

    public NodoIdEncadenado(Token id, NodoEncadenado cad) {
        super(id, cad);
    }

    public NodoIdEncadenado(Token id) {
        this(id, null);
    }

    public boolean terminaEnVar() {
        if (this.getCadena() == null)
            return true;
        else
            return this.getCadena().terminaEnVar();
    }

    public boolean terminaEnLlamada() {
        if (this.getCadena() == null)
            return false;
        else
            return this.getCadena().terminaEnLlamada();
    }

    @Override
    public TipoMetodo chequear(TipoMetodo receptor) throws ExcepcionSemantica {
        Token idAtributo = this.getToken();

        if (receptor.esTipoPrimitivo())
            throw new ExcepcionReceptorPrimitivo(idAtributo);
        if (receptor.esVoid())
            throw new ExcepcionReceptorVoid(idAtributo);

        Clase claseReceptora = TablaSimbolos.clases.get(receptor.getNombreClase());
        HashMap<String, VarInstancia> tablaAtributos = claseReceptora.getAtributos();
        String nombreAtributo = idAtributo.getLexema();

        if (!tablaAtributos.containsKey(nombreAtributo))
            throw new ExcepcionAtributoInexistente(nombreAtributo, idAtributo);

        this.atributo = tablaAtributos.get(nombreAtributo);

        if (!this.atributo.getEsPublico()) {
            if (!TablaSimbolos.claseActual.getNombreClase().equals(claseReceptora.getNombreClase())) {
                throw new ExcepcionAtributoNoVisible(nombreAtributo, idAtributo);
            }
        }

        Tipo tipo = atributo.getTipoVar();
        if (this.getCadena() != null) {
            this.getCadena().setEsLadoIzq(this.getEsLadoIzq());
            return this.getCadena().chequear(tipo);
        } else
            return tipo;
    }

    /**
     * STUB: Genera IR para acceso a atributo encadenado
     */
    @Override
    public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Acceso a atributo encadenado - stub simple
        String temp = gci.nuevoTemporal();
        gci.emitir("loadref", String.valueOf(atributo.getOffset()), "", temp);

        if (this.getCadena() != null) {
            return this.getCadena().generar(gci);
        }

        return temp;
    }
}