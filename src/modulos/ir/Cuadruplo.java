package modulos.ir;

/**
 * Representa un cuádruplo en código de tres direcciones.
 * Estructura: (operador, arg1, arg2, resultado)
 * 
 * Ejemplos:
 * (+, a, b, t1) -> t1 = a + b
 * (=, t1, _, x) -> x = t1
 * (if_false, t2, _, L1) -> if_false t2 goto L1
 * (label, _, _, L1) -> L1:
 */
public class Cuadruplo {
    private String operador; // +, -, *, /, =, goto, if_false, label, etc.
    private String arg1; // Primer operando
    private String arg2; // Segundo operando
    private String resultado; // Variable resultado o etiqueta destino

    /**
     * Constructor de cuádruplo
     * 
     * @param op  Operador
     * @param a1  Primer argumento (puede ser null)
     * @param a2  Segundo argumento (puede ser null)
     * @param res Resultado (puede ser null)
     */
    public Cuadruplo(String op, String a1, String a2, String res) {
        this.operador = op;
        this.arg1 = a1 != null ? a1 : "";
        this.arg2 = a2 != null ? a2 : "";
        this.resultado = res != null ? res : "";
    }

    // ==================== GETTERS Y SETTERS ====================

    public String getOperador() {
        return operador;
    }

    public String getArg1() {
        return arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public String getResultado() {
        return resultado;
    }

    public void setOperador(String op) {
        this.operador = op;
    }

    public void setArg1(String a1) {
        this.arg1 = a1;
    }

    public void setArg2(String a2) {
        this.arg2 = a2;
    }

    public void setResultado(String res) {
        this.resultado = res;
    }

    // ==================== MÉTODOS DE CLASIFICACIÓN ====================

    /**
     * @return true si este cuádruplo representa una etiqueta
     */
    public boolean esEtiqueta() {
        return operador.equals("label");
    }

    /**
     * @return true si este cuádruplo representa un salto (goto, if_false, if_true)
     */
    public boolean esSalto() {
        return operador.equals("goto") || operador.equals("if_false") ||
                operador.equals("if_true");
    }

    /**
     * @return true si este cuádruplo es una asignación simple
     */
    public boolean esAsignacion() {
        return operador.equals("=");
    }

    /**
     * @return true si es una operación aritmética (+, -, *, /, %)
     */
    public boolean esOperacionAritmetica() {
        return operador.matches("[+\\-*/\\%]");
    }

    /**
     * @return true si es una operación lógica (&&, ||, !)
     */
    public boolean esOperacionLogica() {
        return operador.equals("&&") || operador.equals("||") || operador.equals("!");
    }

    /**
     * @return true si es una comparación (==, !=, <, >, <=, >=)
     */
    public boolean esComparacion() {
        return operador.matches("(==|!=|<|>|<=|>=)");
    }

    /**
     * @return true si es una operación con arrays
     */
    public boolean esOperacionArray() {
        return operador.equals("=[]") || operador.equals("[]=");
    }

    /**
     * @return true si este cuádruplo tiene efectos secundarios (no puede
     *         eliminarse)
     */
    public boolean tieneEfectosSecundarios() {
        return operador.equals("call") ||
                operador.equals("print") ||
                operador.equals("read") ||
                operador.equals("[]=") ||
                operador.equals("return");
    }

    // ==================== MÉTODOS DE FORMATO ====================

    /**
     * Formato formal de cuádruplo: (op, arg1, arg2, resultado)
     */
    @Override
    public String toString() {
        return String.format("(%s, %s, %s, %s)",
                operador,
                arg1.isEmpty() ? "_" : arg1,
                arg2.isEmpty() ? "_" : arg2,
                resultado.isEmpty() ? "_" : resultado);
    }

    /**
     * Formato legible para humanos (código de tres direcciones)
     * 
     * @return String representando el cuádruplo en formato TAC
     */
    public String toReadableString() {
        if (esEtiqueta()) {
            return resultado + ":";
        } else if (operador.equals("goto")) {
            return "goto " + resultado;
        } else if (operador.equals("if_false")) {
            return "if_false " + arg1 + " goto " + resultado;
        } else if (operador.equals("if_true")) {
            return "if_true " + arg1 + " goto " + resultado;
        } else if (esAsignacion() && arg2.isEmpty()) {
            return resultado + " = " + arg1;
        } else if (esOperacionAritmetica() || esOperacionLogica() || esComparacion()) {
            return resultado + " = " + arg1 + " " + operador + " " + arg2;
        } else if (operador.equals("param")) {
            return "param " + arg1;
        } else if (operador.equals("call")) {
            if (!resultado.isEmpty()) {
                return resultado + " = call " + arg1 + ", " + arg2;
            } else {
                return "call " + arg1 + ", " + arg2;
            }
        } else if (operador.equals("return")) {
            if (!arg1.isEmpty()) {
                return "return " + arg1;
            } else {
                return "return";
            }
        } else if (operador.equals("[]=")) {
            // Asignación a array: arr[index] = valor
            return arg1 + "[" + arg2 + "] = " + resultado;
        } else if (operador.equals("=[]")) {
            // Lectura de array: resultado = arr[index]
            return resultado + " = " + arg1 + "[" + arg2 + "]";
        } else if (operador.equals("print")) {
            return "print " + arg1;
        } else if (operador.equals("read")) {
            return resultado + " = read()";
        } else if (operador.equals("print_str")) {
            return "print \"" + arg1 + "\"";
        } else if (operador.equals("BEGIN_FUNCTION")) {
            return "func " + arg1 + "(" + arg2 + "):";
        } else if (operador.equals("END_FUNCTION")) {
            return "end " + arg1;
        }

        return toString();
    }

    /**
     * Crea una copia del cuádruplo
     */
    public Cuadruplo clonar() {
        return new Cuadruplo(operador, arg1, arg2, resultado);
    }

    /**
     * Compara dos cuádruplos por igualdad estructural
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Cuadruplo))
            return false;
        Cuadruplo otro = (Cuadruplo) obj;
        return this.operador.equals(otro.operador) &&
                this.arg1.equals(otro.arg1) &&
                this.arg2.equals(otro.arg2) &&
                this.resultado.equals(otro.resultado);
    }

    @Override
    public int hashCode() {
        return operador.hashCode() + arg1.hashCode() + arg2.hashCode() + resultado.hashCode();
    }
}
