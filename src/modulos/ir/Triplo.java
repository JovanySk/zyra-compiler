package modulos.ir;

/**
 * Representa un triplo en código de tres direcciones.
 * Estructura: (número) (operador, arg1, arg2)
 * 
 * Los triplos referencian resultados anteriores por número de triplo.
 * 
 * Ejemplos:
 * (0) (+, a, b) -> (0) = a + b, se referencia como (0)
 * (1) (*, (0), c) -> (1) = (0) * c
 * (2) (=, (1), x) -> x = (1)
 */
public class Triplo {
    private int numero; // Número de triplo (índice)
    private String operador; // Operación a realizar
    private String arg1; // Primer argumento (puede ser operando o referencia "(n)")
    private String arg2; // Segundo argumento (puede ser operando o referencia "(n)")

    /**
     * Constructor de triplo
     * 
     * @param num Número de triplo (índice en la lista)
     * @param op  Operador
     * @param a1  Primer argumento
     * @param a2  Segundo argumento
     */
    public Triplo(int num, String op, String a1, String a2) {
        this.numero = num;
        this.operador = op;
        this.arg1 = a1 != null ? a1 : "";
        this.arg2 = a2 != null ? a2 : "";
    }

    // ==================== GETTERS Y SETTERS ====================

    public int getNumero() {
        return numero;
    }

    public String getOperador() {
        return operador;
    }

    public String getArg1() {
        return arg1;
    }

    public String getArg2() {
        return arg2;
    }

    public void setNumero(int num) {
        this.numero = num;
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

    // ==================== MÉTODOS DE UTILIDAD ====================

    /**
     * @return true si arg1 es una referencia a otro triplo
     */
    public boolean arg1EsReferencia() {
        return arg1.matches("\\(\\d+\\)");
    }

    /**
     * @return true si arg2 es una referencia a otro triplo
     */
    public boolean arg2EsReferencia() {
        return arg2.matches("\\(\\d+\\)");
    }

    /**
     * Extrae el número de triplo de una referencia
     * 
     * @param ref Referencia en formato "(n)"
     * @return El número del triplo referenciado
     */
    public static int extraerNumeroReferencia(String ref) {
        if (ref.matches("\\(\\d+\\)")) {
            String num = ref.substring(1, ref.length() - 1);
            return Integer.parseInt(num);
        }
        return -1;
    }

    /**
     * Crea una referencia a este triplo
     * 
     * @return String en formato "(n)" donde n es el número del triplo
     */
    public String getReferencia() {
        return "(" + numero + ")";
    }

    // ==================== MÉTODOS DE CLASIFICACIÓN ====================

    public boolean esEtiqueta() {
        return operador.equals("label");
    }

    public boolean esSalto() {
        return operador.equals("goto") || operador.equals("if_false") ||
                operador.equals("if_true");
    }

    public boolean esAsignacion() {
        return operador.equals("=");
    }

    public boolean esOperacionAritmetica() {
        return operador.matches("[+\\-*/\\%]");
    }

    public boolean esOperacionLogica() {
        return operador.equals("&&") || operador.equals("||") || operador.equals("!");
    }

    public boolean esComparacion() {
        return operador.matches("(==|!=|<|>|<=|>=)");
    }

    // ==================== MÉTODOS DE FORMATO ====================

    /**
     * Formato formal de triplo: (num) (op, arg1, arg2)
     */
    @Override
    public String toString() {
        return String.format("(%d) (%s, %s, %s)",
                numero,
                operador,
                arg1.isEmpty() ? "_" : arg1,
                arg2.isEmpty() ? "_" : arg2);
    }

    /**
     * Formato legible para humanos
     */
    public String toReadableString() {
        if (esEtiqueta()) {
            // Las etiquetas no usan el número de triplo
            return arg1 + ":";
        } else if (operador.equals("goto")) {
            return "goto " + arg1;
        } else if (operador.equals("if_false")) {
            return "if_false " + arg1 + " goto " + arg2;
        } else if (operador.equals("if_true")) {
            return "if_true " + arg1 + " goto " + arg2;
        } else if (esAsignacion()) {
            return "(" + numero + ") = " + arg1;
        } else if (esOperacionAritmetica() || esOperacionLogica() || esComparacion()) {
            return "(" + numero + ") = " + arg1 + " " + operador + " " + arg2;
        } else if (operador.equals("param")) {
            return "param " + arg1;
        } else if (operador.equals("call")) {
            return "(" + numero + ") = call " + arg1 + ", " + arg2;
        } else if (operador.equals("return")) {
            if (!arg1.isEmpty()) {
                return "return " + arg1;
            } else {
                return "return";
            }
        } else if (operador.equals("=[]")) {
            return "(" + numero + ") = " + arg1 + "[" + arg2 + "]";
        } else if (operador.equals("[]=")) {
            return arg1 + "[" + arg2 + "] = " + getReferencia();
        } else if (operador.equals("print")) {
            return "print " + arg1;
        } else if (operador.equals("read")) {
            return "(" + numero + ") = read()";
        }

        return toString();
    }

    /**
     * Crea una copia del triplo
     */
    public Triplo clonar() {
        return new Triplo(numero, operador, arg1, arg2);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Triplo))
            return false;
        Triplo otro = (Triplo) obj;
        return this.numero == otro.numero &&
                this.operador.equals(otro.operador) &&
                this.arg1.equals(otro.arg1) &&
                this.arg2.equals(otro.arg2);
    }

    @Override
    public int hashCode() {
        return numero + operador.hashCode() + arg1.hashCode() + arg2.hashCode();
    }
}
