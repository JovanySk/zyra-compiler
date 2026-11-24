// Contenido completo y CORREGIDO
package estructuras.ast.expresiones;

import estructuras.Token;
import estructuras.ts.tipos.Tipo;
import estructuras.ts.tipos.TipoArreglo;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import estructuras.ts.tipos.TipoMetodo;

public class NodoCreacionArreglo extends NodoPrimario {

    private Token newToken;
    private Tipo tipoBase;
    private NodoExpresion dimension;

    public NodoCreacionArreglo(Token newToken, Tipo tipo, NodoExpresion dim) {
        this.newToken = newToken;
        this.tipoBase = tipo;
        this.dimension = dim;
    }

    public Tipo getTipoBase() {
        return tipoBase;
    }

    public NodoExpresion getDimension() {
        return dimension;
    }

    @Override
    public Token getToken() {
        return newToken;
    }

    @Override
    public TipoMetodo chequear() throws ExcepcionSemantica {
        // Chequeamos que la dimensión sea un entero.
        TipoMetodo tipoDimension = dimension.chequear();
        if (!tipoDimension.getNombreClase().equals("int")) {
            throw new ExcepcionSemanticaPersonalizada(this.getToken().getLinea(),
                    "La dimension de un arreglo debe ser de tipo entero.", this.getToken());
        }

        // El tipo de esta expresión es un arreglo del tipo base.
        return new TipoArreglo(tipoBase);
    }

    /**
     * Genera código intermedio para creación de array
     * 
     * @param gci Generador de código intermedio
     * @return Temporal que contiene la referencia al nuevo array
     */
    public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Generar código para la dimensión
        String dim = dimension.generar(gci);

        // Generar temporal para el array
        String temp = gci.nuevoTemporal();

        // Emitir cuádruplo para creación de array
        // Formato: (new_array, tipoBase, dimension, temp)
        gci.emitir("new_array", tipoBase.getNombreClase(), dim, temp);

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
