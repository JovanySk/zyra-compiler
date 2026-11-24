// IMPLEMENTACIÓN COMPLETA con IR
package estructuras.ast.sentencias;

import excepciones.semanticas.ExcepcionSemantica;

public class NodoBreak extends NodoSentencia {

    public NodoBreak() {
        // No necesita campos adicionales
    }

    @Override
    public void chequear() throws ExcepcionSemantica {
        // La lógica para chequear que el break esté dentro de un switch o bucle irá
        // aquí
        // Por ahora, aceptamos break sin validación
    }

    /**
     * Genera código intermedio para break
     * 
     * @param gci Generador de código intermedio
     */
    public void generar(modulos.ir.GeneradorCodigoIntermedio gci) {
        // Break salta a la etiqueta de fin del loop actual
        // El GeneradorCodigoIntermedio mantiene una pila de etiquetas de break
        gci.generarBreak();
    }
}
