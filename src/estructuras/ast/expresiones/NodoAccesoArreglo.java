// Contenido completo y CORREGIDO
package estructuras.ast.expresiones;

import estructuras.Token;
import estructuras.ts.tipos.TipoArreglo;
import estructuras.ts.tipos.TipoMetodo;
import excepciones.semanticas.ExcepcionSemantica;
import excepciones.semanticas.ExcepcionSemanticaPersonalizada;
import modulos.GenCod;

public class NodoAccesoArreglo extends NodoPrimario {

    private NodoPrimario primario;
    private NodoExpresion indice;

    public NodoAccesoArreglo(NodoPrimario prim, NodoExpresion idx) {
        this.primario = prim;
        this.indice = idx;
    }

    public NodoPrimario getPrimario() {
        return primario;
    }

    public NodoExpresion getIndice() {
        return indice;
    }

    @Override
    public Token getToken() {
        return primario.getToken();
    }

    @Override
    public TipoMetodo chequear() throws ExcepcionSemantica {
        TipoMetodo tipoPrimario = primario.chequear();
        if (!(tipoPrimario instanceof TipoArreglo)) {
            throw new ExcepcionSemanticaPersonalizada(primario.getToken().getLinea(),
                    "Se intentó aplicar un acceso de arreglo a un tipo que no es un arreglo.", primario.getToken());
        }

        TipoMetodo tipoIndice = indice.chequear();
        if (!tipoIndice.getNombreClase().equals("int")) {
            // Usamos el token del primario para el resaltado, ya que el índice puede ser
            // una expresión compleja sin token único.
            throw new ExcepcionSemanticaPersonalizada(primario.getToken().getLinea(),
                    "El índice de un arreglo debe ser de tipo entero.", primario.getToken());
        }

        // El tipo de esta expresión es el tipo de los elementos del arreglo.
        TipoMetodo tipoResultado = ((TipoArreglo) tipoPrimario).getTipoDeElementos();

        if (this.getCadena() != null) {
            this.getCadena().setEsLadoIzq(this.getEsLadoIzq());
            return this.getCadena().chequear(tipoResultado);
        } else {
            return tipoResultado;
        }
    }

    /**
     * Genera código intermedio para acceso a array
     * 
     * @param gci Generador de código intermedio
     * @return Temporal que contiene el valor leído del array
     */
    public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Generar código para el array y el índice
        String array = primario.generar(gci);
        String idx = indice.generar(gci);

        // Si estamos en el lado izquierdo de una asignación, solo retornar
        // la referencia para que la asignación la use
        if (this.getEsLadoIzq() && this.getCadena() == null) {
            // La asignación se encargará de generar el cuádruplo []=
            return array + "[" + idx + "]";
        } else {
            // Lectura de array: generar cuádruplo =[]
            return gci.generarLecturaArray(array, idx);
        }
    }

    @Override
    public boolean terminaEnVar() {
        return getCadena() == null || getCadena().terminaEnVar();
    }

    @Override
    public boolean terminaEnLlamada() {
        return getCadena() != null && getCadena().terminaEnLlamada();
    }
}
