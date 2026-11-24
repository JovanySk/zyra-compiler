// Contenido completo y CORREGIDO
package estructuras.ast.expresiones;

import estructuras.Token;
import estructuras.ts.tipos.TipoMetodo;
import excepciones.semanticas.ExcepcionSemantica;

public abstract class NodoExpresion {

    public abstract TipoMetodo chequear() throws ExcepcionSemantica;

    /**
     * Genera código intermedio para esta expresión
     * 
     * @param gci Generador de código intermedio
     * @return Temporal o valor que contiene el resultado de la expresión
     */
    public abstract String generar(modulos.ir.GeneradorCodigoIntermedio gci);

    public abstract Token getToken();

    /**
     * Método stub para compatibilidad con NodoAsignacion
     * Indica si la expresión está en el lado izquierdo de una asignación
     * Por defecto no hace nada; sobreescribir en subclases que lo requieran
     */
    public void setEsLadoIzq(boolean esLadoIzq) {
        // Stub - las subclases que soportan asignación lo implementarán
    }
}