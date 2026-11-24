package modulos.ir.optimizacion;

import modulos.ir.GeneradorCodigoIntermedio;

/**
 * Interfaz para optimizadores de código intermedio
 * 
 * Cada optimizador implementa un pase específico de optimización
 * que transforma el código intermedio para mejorar eficiencia.
 * 
 * @author Zyra Compiler Team
 */
public interface OptimizadorIR {

    /**
     * Aplica optimizaciones al código intermedio
     * 
     * @param gci Generador con los cuádruplos a optimizar
     * @return true si se realizaron cambios, false si no hubo optimizaciones
     */
    boolean optimizar(GeneradorCodigoIntermedio gci);

    /**
     * Retorna el nombre del optimizador para logging y debugging
     * 
     * @return Nombre descriptivo del optimizador
     */
    String getNombre();

    /**
     * Retorna una breve descripción de qué hace este optimizador
     * 
     * @return Descripción de la optimización
     */
    default String getDescripcion() {
        return "Optimizador de IR";
    }
}
