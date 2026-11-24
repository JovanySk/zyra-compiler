package modulos.ir.optimizacion;

import modulos.ir.Cuadruplo;
import modulos.ir.GeneradorCodigoIntermedio;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optimizador: Propagación de Constantes
 * 
 * Esta optimización detecta asignaciones de constantes a variables
 * y propaga esos valores constantes a través del código, reemplazando
 * referencias a la variable con el valor constante.
 * 
 * Ejemplo:
 * ANTES: DESPUÉS:
 * t1 = 5 t1 = 5
 * t2 = t1 + 3 t2 = 5 + 3 → t2 = 8 (plegado de constantes)
 * x = t2 * 2 x = 8 * 2 → x = 16 (plegado de constantes)
 * 
 * @author Zyra Compiler Team
 */
public class PropagacionConstantes implements OptimizadorIR {

    @Override
    public String getNombre() {
        return "Propagación de Constantes";
    }

    @Override
    public String getDescripcion() {
        return "Reemplaza variables con valores constantes conocidos y pliega constantes";
    }

    @Override
    public boolean optimizar(GeneradorCodigoIntermedio gci) {
        List<Cuadruplo> cuads = gci.getCuadruplos();
        Map<String, String> constantes = new HashMap<>();
        boolean cambios = false;

        for (int i = 0; i < cuads.size(); i++) {
            Cuadruplo c = cuads.get(i);

            // Detectar asignaciones de constantes: t1 = 5
            if (c.esAsignacion() && c.getArg2().isEmpty() && esConstante(c.getArg1())) {
                constantes.put(c.getResultado(), c.getArg1());
                continue;
            }

            // Reemplazar uso de constantes conocidas
            String nuevoArg1 = c.getArg1();
            String nuevoArg2 = c.getArg2();
            boolean modificado = false;

            if (constantes.containsKey(c.getArg1())) {
                nuevoArg1 = constantes.get(c.getArg1());
                modificado = true;
            }

            if (constantes.containsKey(c.getArg2())) {
                nuevoArg2 = constantes.get(c.getArg2());
                modificado = true;
            }

            if (modificado) {
                Cuadruplo nuevo = new Cuadruplo(c.getOperador(), nuevoArg1, nuevoArg2, c.getResultado());
                gci.reemplazarCuadruplo(i, nuevo);
                cambios = true;

                // Plegado de constantes: si ambos operandos son constantes, evaluar
                if (nuevo.esOperacionAritmetica() &&
                        esConstante(nuevoArg1) &&
                        esConstante(nuevoArg2)) {

                    String resultado = evaluarOperacion(nuevo.getOperador(), nuevoArg1, nuevoArg2);
                    if (resultado != null) {
                        Cuadruplo plegado = new Cuadruplo("=", resultado, "", nuevo.getResultado());
                        gci.reemplazarCuadruplo(i, plegado);
                        constantes.put(nuevo.getResultado(), resultado);
                    }
                } else if (nuevo.esComparacion() &&
                        esConstante(nuevoArg1) &&
                        esConstante(nuevoArg2)) {

                    String resultado = evaluarComparacion(nuevo.getOperador(), nuevoArg1, nuevoArg2);
                    if (resultado != null) {
                        Cuadruplo plegado = new Cuadruplo("=", resultado, "", nuevo.getResultado());
                        gci.reemplazarCuadruplo(i, plegado);
                        constantes.put(nuevo.getResultado(), resultado);
                    }
                }
            }

            // Invalidar constantes si la variable es modificada
            if (c.esAsignacion() || c.esOperacionAritmetica() || c.esComparacion()) {
                constantes.remove(c.getResultado());
            }

            // Invalidar en control de flujo (podríamos ser más precisos con análisis de
            // flujo de datos)
            if (c.esEtiqueta()) {
                // En un análisis más sofisticado, solo invalidaríamos
                // las variables que podrían ser modificadas en otros caminos
                // Por ahora, mantenemos las constantes literales
            }
        }

        return cambios;
    }

    /**
     * Verifica si un valor es una constante (número o booleano)
     */
    private boolean esConstante(String valor) {
        if (valor == null || valor.isEmpty())
            return false;

        // Números enteros
        try {
            Integer.parseInt(valor);
            return true;
        } catch (NumberFormatException e) {
            // Números decimales
            try {
                Double.parseDouble(valor);
                return true;
            } catch (NumberFormatException e2) {
                // Booleanos
                return valor.equals("true") || valor.equals("false");
            }
        }
    }

    /**
     * Evalúa una operación aritmética entre dos constantes
     * 
     * @return El resultado como string, o null si no se puede evaluar
     */
    private String evaluarOperacion(String op, String a1, String a2) {
        try {
            // Intentar como enteros primero
            int v1 = Integer.parseInt(a1);
            int v2 = Integer.parseInt(a2);

            switch (op) {
                case "+":
                    return String.valueOf(v1 + v2);
                case "-":
                    return String.valueOf(v1 - v2);
                case "*":
                    return String.valueOf(v1 * v2);
                case "/":
                    return v2 != 0 ? String.valueOf(v1 / v2) : null;
                case "%":
                    return v2 != 0 ? String.valueOf(v1 % v2) : null;
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            // Si no son enteros, intentar como doubles
            try {
                double v1 = Double.parseDouble(a1);
                double v2 = Double.parseDouble(a2);

                switch (op) {
                    case "+":
                        return String.valueOf(v1 + v2);
                    case "-":
                        return String.valueOf(v1 - v2);
                    case "*":
                        return String.valueOf(v1 * v2);
                    case "/":
                        return v2 != 0 ? String.valueOf(v1 / v2) : null;
                    case "%":
                        return v2 != 0 ? String.valueOf(v1 % v2) : null;
                    default:
                        return null;
                }
            } catch (NumberFormatException e2) {
                return null;
            }
        }
    }

    /**
     * Evalúa una comparación entre dos constantes
     */
    private String evaluarComparacion(String op, String a1, String a2) {
        try {
            int v1 = Integer.parseInt(a1);
            int v2 = Integer.parseInt(a2);

            boolean resultado;
            switch (op) {
                case "==":
                    resultado = (v1 == v2);
                    break;
                case "!=":
                    resultado = (v1 != v2);
                    break;
                case "<":
                    resultado = (v1 < v2);
                    break;
                case ">":
                    resultado = (v1 > v2);
                    break;
                case "<=":
                    resultado = (v1 <= v2);
                    break;
                case ">=":
                    resultado = (v1 >= v2);
                    break;
                default:
                    return null;
            }

            return resultado ? "true" : "false";
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
