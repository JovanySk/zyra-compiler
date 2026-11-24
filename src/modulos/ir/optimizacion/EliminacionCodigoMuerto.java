package modulos.ir.optimizacion;

import modulos.ir.Cuadruplo;
import modulos.ir.GeneradorCodigoIntermedio;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Optimizador: Eliminación de Código Muerto (Dead Code Elimination)
 * 
 * Esta optimización elimina cuádruplos cuyos resultados nunca se usan.
 * Un cuádruplo es código muerto si:
 * 1. Calcula un valor que nunca se lee posteriormente
 * 2. No tiene efectos secundarios (no es I/O, llamada de función, etc.)
 * 
 * Ejemplo:
 * ANTES: DESPUÉS:
 * t1 = a + b [eliminado]
 * t2 = c * d x = c * d
 * x = t2
 * 
 * @author Zyra Compiler Team
 */
public class EliminacionCodigoMuerto implements OptimizadorIR {

    @Override
    public String getNombre() {
        return "Eliminación de Código Muerto";
    }

    @Override
    public String getDescripcion() {
        return "Elimina instrucciones cuyos resultados nunca se utilizan";
    }

    @Override
    public boolean optimizar(GeneradorCodigoIntermedio gci) {
        List<Cuadruplo> cuads = gci.getCuadruplos();
        Set<String> variablesUsadas = new HashSet<>();
        Set<Integer> indicesAEliminar = new HashSet<>();
        boolean cambios = false;

        // Pase 1: Marcar todas las variables que se usan (análisis hacia atrás)
        for (int i = cuads.size() - 1; i >= 0; i--) {
            Cuadruplo c = cuads.get(i);

            // Marcar argumentos como usados
            if (!c.getArg1().isEmpty() && esVariable(c.getArg1())) {
                variablesUsadas.add(c.getArg1());
            }
            if (!c.getArg2().isEmpty() && esVariable(c.getArg2())) {
                variablesUsadas.add(c.getArg2());
            }

            // Si es un cuádruplo que produce un resultado
            if (!c.getResultado().isEmpty() && esVariable(c.getResultado())) {
                // Si el resultado nunca se usa y no tiene efectos secundarios
                if (!variablesUsadas.contains(c.getResultado()) &&
                        !tieneEfectosSecundarios(c) &&
                        !esVariableGlobal(c.getResultado())) {

                    // Marcar para eliminación
                    indicesAEliminar.add(i);
                } else {
                    // El resultado se usa, así que marcar los argumentos como necesarios
                    if (!c.getArg1().isEmpty() && esVariable(c.getArg1())) {
                        variablesUsadas.add(c.getArg1());
                    }
                    if (!c.getArg2().isEmpty() && esVariable(c.getArg2())) {
                        variablesUsadas.add(c.getArg2());
                    }
                }
            }
        }

        // Pase 2: Eliminar cuádruplos marcados (de atrás hacia adelante)
        for (int i = cuads.size() - 1; i >= 0; i--) {
            if (indicesAEliminar.contains(i)) {
                gci.eliminarCuadruplo(i);
                cambios = true;
            }
        }

        return cambios;
    }

    /**
     * Verifica si un string es un nombre de variable (temporal o no)
     */
    private boolean esVariable(String valor) {
        if (valor == null || valor.isEmpty())
            return false;

        // Temporales empiezan con 't' seguido de números
        if (valor.matches("t\\d+"))
            return true;

        // Variables normales (identificadores)
        if (valor.matches("[a-zA-Z_][a-zA-Z0-9_]*"))
            return true;

        return false;
    }

    /**
     * Verifica si un cuádruplo tiene efectos secundarios
     * (no puede eliminarse aunque su resultado no se use)
     */
    private boolean tieneEfectosSecundarios(Cuadruplo c) {
        String op = c.getOperador();

        // Operaciones con efectos secundarios
        return op.equals("call") || // Llamadas a funciones
                op.equals("print") || // Impresión
                op.equals("read") || // Lectura
                op.equals("[]=") || // Escritura a array
                op.equals("return") || // Return
                op.equals("param") || // Paso de parámetros
                op.equals("goto") || // Saltos
                op.equals("if_false") ||
                op.equals("if_true") ||
                op.equals("label"); // Etiquetas
    }

    /**
     * Verifica si una variable es global/visible fuera de la función
     * (no puede eliminarse aunque parezca no usarse)
     */
    private boolean esVariableGlobal(String variable) {
        // Los temporales (t0, t1, etc.) nunca son globales
        if (variable.matches("t\\d+"))
            return false;

        // Las variables que no son temporales podrían ser globales
        // o usadas fuera del alcance actual
        // Por seguridad, consideramos que variables de una sola letra
        // o variables que no son temporales podrían ser importantes
        return !variable.startsWith("t");
    }
}
