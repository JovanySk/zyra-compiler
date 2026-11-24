package modulos.ir.optimizacion;

import modulos.ir.Cuadruplo;
import modulos.ir.GeneradorCodigoIntermedio;
import java.util.List;

/**
 * Optimizador: Optimización Algebraica
 * 
 * Esta optimización aplica reglas algebraicas básicas para simplificar
 * expresiones.
 * 
 * Reglas aplicadas:
 * - x + 0 = x
 * - x - 0 = x
 * - x * 1 = x
 * - x * 0 = 0
 * - x / 1 = x
 * - 0 + x = x
 * - 1 * x = x
 * - x - x = 0
 * 
 * Ejemplo:
 * ANTES: DESPUÉS:
 * t1 = a + 0 t1 = a
 * t2 = b * 1 t2 = b
 * t3 = c * 0 t3 = 0
 * 
 * @author Zyra Compiler Team
 */
public class OptimizacionAlgebraica implements OptimizadorIR {

    @Override
    public String getNombre() {
        return "Optimización Algebraica";
    }

    @Override
    public String getDescripcion() {
        return "Aplica reglas algebraicas para simplificar expresiones (x+0=x, x*1=x, etc.)";
    }

    @Override
    public boolean optimizar(GeneradorCodigoIntermedio gci) {
        List<Cuadruplo> cuads = gci.getCuadruplos();
        boolean cambios = false;

        for (int i = 0; i < cuads.size(); i++) {
            Cuadruplo c = cuads.get(i);

            // Solo aplicar a operaciones aritméticas
            if (!c.esOperacionAritmetica()) {
                continue;
            }

            Cuadruplo optimizado = aplicarReglasAlgebraicas(c);

            if (optimizado != null) {
                gci.reemplazarCuadruplo(i, optimizado);
                cambios = true;
            }
        }

        return cambios;
    }

    /**
     * Aplica reglas algebraicas a un cuádruplo
     * 
     * @return Cuádruplo optimizado, o null si no se aplica ninguna regla
     */
    private Cuadruplo aplicarReglasAlgebraicas(Cuadruplo c) {
        String op = c.getOperador();
        String a1 = c.getArg1();
        String a2 = c.getArg2();
        String res = c.getResultado();

        // ==================== REGLAS PARA SUMA ====================

        if (op.equals("+")) {
            // x + 0 = x
            if (esCero(a2)) {
                return new Cuadruplo("=", a1, "", res);
            }

            // 0 + x = x
            if (esCero(a1)) {
                return new Cuadruplo("=", a2, "", res);
            }
        }

        // ==================== REGLAS PARA RESTA ====================

        if (op.equals("-")) {
            // x - 0 = x
            if (esCero(a2)) {
                return new Cuadruplo("=", a1, "", res);
            }

            // x - x = 0 (mismo operando)
            if (a1.equals(a2)) {
                return new Cuadruplo("=", "0", "", res);
            }

            // 0 - x = -x (negación)
            if (esCero(a1)) {
                return new Cuadruplo("NEG", a2, "", res);
            }
        }

        // ==================== REGLAS PARA MULTIPLICACIÓN ====================

        if (op.equals("*")) {
            // x * 1 = x
            if (esUno(a2)) {
                return new Cuadruplo("=", a1, "", res);
            }

            // 1 * x = x
            if (esUno(a1)) {
                return new Cuadruplo("=", a2, "", res);
            }

            // x * 0 = 0
            if (esCero(a1) || esCero(a2)) {
                return new Cuadruplo("=", "0", "", res);
            }

            // x * 2 = x + x (puede ser más eficiente en algunos casos)
            // (Comentado porque no siempre es mejor)
            /*
             * if (a2.equals("2")) {
             * return new Cuadruplo("+", a1, a1, res);
             * }
             */
        }

        // ==================== REGLAS PARA DIVISIÓN ====================

        if (op.equals("/")) {
            // x / 1 = x
            if (esUno(a2)) {
                return new Cuadruplo("=", a1, "", res);
            }

            // 0 / x = 0 (si x != 0, pero asumimos que el código es correcto)
            if (esCero(a1) && !esCero(a2)) {
                return new Cuadruplo("=", "0", "", res);
            }

            // x / x = 1 (si x != 0)
            if (a1.equals(a2)) {
                return new Cuadruplo("=", "1", "", res);
            }
        }

        // ==================== REGLAS PARA MÓDULO ====================

        if (op.equals("%")) {
            // x % 1 = 0
            if (esUno(a2)) {
                return new Cuadruplo("=", "0", "", res);
            }

            // 0 % x = 0
            if (esCero(a1)) {
                return new Cuadruplo("=", "0", "", res);
            }

            // x % x = 0
            if (a1.equals(a2)) {
                return new Cuadruplo("=", "0", "", res);
            }
        }

        // No se aplica ninguna regla
        return null;
    }

    /**
     * Verifica si un valor es cero (0 o 0.0)
     */
    private boolean esCero(String valor) {
        if (valor == null)
            return false;

        try {
            double d = Double.parseDouble(valor);
            return d == 0.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Verifica si un valor es uno (1 o 1.0)
     */
    private boolean esUno(String valor) {
        if (valor == null)
            return false;

        try {
            double d = Double.parseDouble(valor);
            return d == 1.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
