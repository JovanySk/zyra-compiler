package modulos.ir.optimizacion;

import modulos.ir.Cuadruplo;
import modulos.ir.GeneradorCodigoIntermedio;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimizador: Eliminación de Subexpresiones Comunes (CSE - Common
 * Subexpression Elimination)
 * 
 * Esta optimización detecta expresiones que se calculan múltiples veces
 * con los mismos operandos y reutiliza el resultado de la primera evaluación.
 * 
 * Ejemplo:
 * ANTES: DESPUÉS:
 * t1 = a + b t1 = a + b
 * t2 = a + b t2 = t1
 * t3 = t1 * t2 t3 = t1 * t1
 * 
 * @author Zyra Compiler Team
 */
public class EliminacionSubexpresionesComunes implements OptimizadorIR {

    @Override
    public String getNombre() {
        return "Eliminación de Subexpresiones Comunes (CSE)";
    }

    @Override
    public String getDescripcion() {
        return "Detecta y elimina cálculos redundantes de la misma expresión";
    }

    @Override
    public boolean optimizar(GeneradorCodigoIntermedio gci) {
        List<Cuadruplo> cuads = gci.getCuadruplos();
        Map<String, String> expresiones = new HashMap<>(); // clave expresión → temporal resultado
        Map<String, String> reemplazos = new HashMap<>(); // temporal → reemplazo
        boolean cambios = false;

        for (int i = 0; i < cuads.size(); i++) {
            Cuadruplo c = cuads.get(i);

            // Solo optimizar operaciones que sean puras (sin efectos secundarios)
            if (c.esOperacionAritmetica() || c.esOperacionLogica() || c.esComparacion()) {
                // Aplicar reemplazos pendientes a los argumentos
                String arg1 = aplicarReemplazo(c.getArg1(), reemplazos);
                String arg2 = aplicarReemplazo(c.getArg2(), reemplazos);

                // Crear clave única para la expresión
                String clave = arg1 + "|" + c.getOperador() + "|" + arg2;

                if (expresiones.containsKey(clave)) {
                    // Ya calculamos esta expresión antes
                    String tempAnterior = expresiones.get(clave);
                    reemplazos.put(c.getResultado(), tempAnterior);

                    // Reemplazar con asignación simple
                    Cuadruplo nuevo = new Cuadruplo("=", tempAnterior, "", c.getResultado());
                    gci.reemplazarCuadruplo(i, nuevo);
                    cambios = true;
                } else {
                    // Primera vez que vemos esta expresión
                    expresiones.put(clave, c.getResultado());

                    // Si los argumentos cambiaron por reemplazos, actualizar el cuádruplo
                    if (!arg1.equals(c.getArg1()) || !arg2.equals(c.getArg2())) {
                        Cuadruplo nuevo = new Cuadruplo(c.getOperador(), arg1, arg2, c.getResultado());
                        gci.reemplazarCuadruplo(i, nuevo);
                        cambios = true;
                    }
                }
            }

            // Invalidar expresiones si alguna variable es modificada
            if (c.esAsignacion() || c.esOperacionAritmetica() || c.esComparacion() ||
                    c.getOperador().equals("[]=")) {

                String varModificada = c.getResultado();

                // Remover expresiones que usan esta variable
                expresiones.entrySet()
                        .removeIf(e -> e.getKey().contains(varModificada) || e.getValue().equals(varModificada));

                // Remover reemplazos que involucran esta variable
                reemplazos.remove(varModificada);
            }

            // Invalidar todo en etiquetas (barreras de control de flujo)
            if (c.esEtiqueta()) {
                expresiones.clear();
                // No limpiamos reemplazos porque son válidos a través de etiquetas
            }
        }

        return cambios;
    }

    /**
     * Aplica un reemplazo de variable si existe
     */
    private String aplicarReemplazo(String variable, Map<String, String> reemplazos) {
        if (variable == null || variable.isEmpty()) {
            return variable;
        }

        // Seguir la cadena de reemplazos
        String actual = variable;
        int maxIteraciones = 10; // Prevenir bucles infinitos
        int iter = 0;

        while (reemplazos.containsKey(actual) && iter < maxIteraciones) {
            actual = reemplazos.get(actual);
            iter++;
        }

        return actual;
    }
}
