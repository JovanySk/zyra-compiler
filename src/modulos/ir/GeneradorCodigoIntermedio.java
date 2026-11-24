package modulos.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.HashMap;
import java.util.Map;

/**
 * Generador de Código Intermedio (IR Generator)
 * 
 * Esta clase es responsable de traducir el AST a representación intermedia
 * en forma de cuádruplos y triplos. Mantiene ambas representaciones
 * sincronizadas.
 * 
 * El código generado sigue el formato de tres direcciones (TAC) donde cada
 * instrucción tiene como máximo 3 operandos.
 * 
 * @author Zyra Compiler Team
 */
public class GeneradorCodigoIntermedio {

    // Almacenamiento de código intermedio
    private List<Cuadruplo> cuadruplos;
    private List<Triplo> triplos;

    // Contadores para nombres únicos
    private int contadorTemporales;
    private int contadorEtiquetas;

    // Pilas para manejo de break/continue en estructuras de control
    private Stack<String> pilaEtiquetasContinue; // Para continue en loops
    private Stack<String> pilaEtiquetasBreak; // Para break en loops

    // Mapeo de temporales a índices de triplos (para conversión)
    private Map<String, Integer> mapaTempATriplo;

    /**
     * Constructor: inicializa el generador de código intermedio
     */
    public GeneradorCodigoIntermedio() {
        cuadruplos = new ArrayList<>();
        triplos = new ArrayList<>();
        contadorTemporales = 0;
        contadorEtiquetas = 0;
        pilaEtiquetasContinue = new Stack<>();
        pilaEtiquetasBreak = new Stack<>();
        mapaTempATriplo = new HashMap<>();
    }

    // ==================== GENERACIÓN DE NOMBRES ÚNICOS ====================

    /**
     * Genera un nuevo nombre de variable temporal único
     * 
     * @return Nombre de temporal en formato "t0", "t1", "t2", etc.
     */
    public String nuevoTemporal() {
        return "t" + (contadorTemporales++);
    }

    /**
     * Genera un nuevo nombre de etiqueta único
     * 
     * @return Nombre de etiqueta en formato "L0", "L1", "L2", etc.
     */
    public String nuevaEtiqueta() {
        return "L" + (contadorEtiquetas++);
    }

    /**
     * Reinicia los contadores (útil para compilaciones múltiples)
     */
    public void reiniciarContadores() {
        contadorTemporales = 0;
        contadorEtiquetas = 0;
    }

    // ==================== EMISIÓN DE CUÁDRUPLOS ====================

    /**
     * Emite un cuádruplo y automáticamente genera el triplo correspondiente
     * 
     * @param op  Operador
     * @param a1  Primer argumento
     * @param a2  Segundo argumento
     * @param res Resultado
     */
    public void emitir(String op, String a1, String a2, String res) {
        Cuadruplo c = new Cuadruplo(op, a1, a2, res);
        cuadruplos.add(c);

        // Generar triplo correspondiente
        int numTriplo = triplos.size();
        Triplo t = convertirCuadruploATriplo(c, numTriplo);
        triplos.add(t);

        // Mapear temporal a triplo
        if (res != null && !res.isEmpty() && res.startsWith("t")) {
            mapaTempATriplo.put(res, numTriplo);
        }
    }

    // ==================== MARCADORES DE FUNCIÓN ====================

    /**
     * Marca el inicio de una función en el IR
     * 
     * @param nombreFuncion Nombre de la función
     */
    public void marcarInicioFuncion(String nombreFuncion) {
        emitir("BEGIN_FUNCTION", nombreFuncion, "", "");
    }

    /**
     * Marca el fin de una función en el IR
     * 
     * @param nombreFuncion Nombre de la función
     */
    public void marcarFinFuncion(String nombreFuncion) {
        emitir("END_FUNCTION", nombreFuncion, "", "");
    }

    /**
     * Convierte un cuádruplo a su representación en triplo
     * Los temporales se convierten a referencias de triplos
     */
    private Triplo convertirCuadruploATriplo(Cuadruplo c, int num) {
        String op = c.getOperador();
        String a1 = convertirTemporalAReferencia(c.getArg1());
        String a2 = convertirTemporalAReferencia(c.getArg2());

        // Para etiquetas, el nombre de la etiqueta va en arg1
        if (c.esEtiqueta()) {
            return new Triplo(num, op, c.getResultado(), "");
        }

        // Para goto y saltos condicionales, la etiqueta destino va en arg
        if (op.equals("goto")) {
            return new Triplo(num, op, c.getResultado(), "");
        }

        if (op.equals("if_false") || op.equals("if_true")) {
            return new Triplo(num, op, a1, c.getResultado());
        }

        return new Triplo(num, op, a1, a2);
    }

    /**
     * Convierte un temporal a una referencia de triplo si existe
     */
    private String convertirTemporalAReferencia(String valor) {
        if (valor != null && valor.startsWith("t") && mapaTempATriplo.containsKey(valor)) {
            int numTriplo = mapaTempATriplo.get(valor);
            return "(" + numTriplo + ")";
        }
        return valor;
    }

    // ==================== OPERACIONES ARITMÉTICAS ====================

    /**
     * Genera código para una operación binaria
     * 
     * @param op  Operador (+, -, *, /, %, etc.)
     * @param izq Operando izquierdo
     * @param der Operando derecho
     * @return Temporal que contiene el resultado
     */
    public String generarOperacionBinaria(String op, String izq, String der) {
        String temp = nuevoTemporal();
        emitir(op, izq, der, temp);
        return temp;
    }

    /**
     * Genera código para una operación unaria
     * 
     * @param op       Operador (-, !, etc.)
     * @param operando Operando
     * @return Temporal que contiene el resultado
     */
    public String generarOperacionUnaria(String op, String operando) {
        String temp = nuevoTemporal();
        emitir(op, operando, "", temp);
        return temp;
    }

    // ==================== ASIGNACIONES ====================

    /**
     * Genera código para una asignación simple: destino = valor
     * 
     * @param destino Variable destino
     * @param valor   Valor a asignar
     */
    public void generarAsignacion(String destino, String valor) {
        emitir("=", valor, "", destino);
    }

    // ==================== ARRAYS ====================

    /**
     * Genera código para asignación a array: array[indice] = valor
     * Cuádruplo: ([]=, array, indice, valor)
     */
    public void generarAsignacionArray(String array, String indice, String valor) {
        emitir("[]=", array, indice, valor);
    }

    /**
     * Genera código para lectura de array: temp = array[indice]
     * Cuádruplo: (=[], array, indice, temp)
     * 
     * @return Temporal que contiene el valor leído
     */
    public String generarLecturaArray(String array, String indice) {
        String temp = nuevoTemporal();
        emitir("=[]", array, indice, temp);
        return temp;
    }

    // ==================== CONTROL DE FLUJO ====================

    /**
     * Genera una etiqueta en el código
     */
    public void generarEtiqueta(String etiqueta) {
        emitir("label", "", "", etiqueta);
    }

    /**
     * Genera un salto incondicional: goto etiqueta
     */
    public void generarGoto(String etiqueta) {
        emitir("goto", "", "", etiqueta);
    }

    /**
     * Genera un salto condicional si la condición es falsa
     * if_false condicion goto etiqueta
     */
    public void generarIfFalse(String condicion, String etiqueta) {
        emitir("if_false", condicion, "", etiqueta);
    }

    /**
     * Genera un salto condicional si la condición es verdadera
     * if_true condicion goto etiqueta
     */
    public void generarIfTrue(String condicion, String etiqueta) {
        emitir("if_true", condicion, "", etiqueta);
    }

    // ==================== IF-THEN-ELSE ====================

    /**
     * Contexto para generación de if-then-else
     */
    public static class ContextoIf {
        public String etiquetaElse;
        public String etiquetaFin;

        public ContextoIf(String etiqElse, String etiqFin) {
            this.etiquetaElse = etiqElse;
            this.etiquetaFin = etiqFin;
        }
    }

    /**
     * Inicia la generación de un if-then-else
     * 
     * @return Contexto con las etiquetas necesarias
     */
    public ContextoIf iniciarIf() {
        String etiqElse = nuevaEtiqueta();
        String etiqFin = nuevaEtiqueta();
        return new ContextoIf(etiqElse, etiqFin);
    }

    // ==================== WHILE ====================

    /**
     * Contexto para generación de while
     */
    public static class ContextoWhile {
        public String etiquetaInicio;
        public String etiquetaFin;

        public ContextoWhile(String etiqInicio, String etiqFin) {
            this.etiquetaInicio = etiqInicio;
            this.etiquetaFin = etiqFin;
        }
    }

    /**
     * Inicia la generación de un while
     * Agrega etiquetas a las pilas para break/continue
     */
    public ContextoWhile iniciarWhile() {
        String etiqInicio = nuevaEtiqueta();
        String etiqFin = nuevaEtiqueta();
        pilaEtiquetasContinue.push(etiqInicio);
        pilaEtiquetasBreak.push(etiqFin);
        return new ContextoWhile(etiqInicio, etiqFin);
    }

    /**
     * Finaliza la generación de un while
     * Remueve etiquetas de las pilas
     */
    public void finalizarWhile() {
        if (!pilaEtiquetasContinue.isEmpty()) {
            pilaEtiquetasContinue.pop();
        }
        if (!pilaEtiquetasBreak.isEmpty()) {
            pilaEtiquetasBreak.pop();
        }
    }

    // ==================== FOR ====================

    /**
     * Contexto para generación de for
     */
    public static class ContextoFor {
        public String etiquetaCondicion;
        public String etiquetaCuerpo;
        public String etiquetaIncremento;
        public String etiquetaFin;

        public ContextoFor(String cond, String cuerpo, String inc, String fin) {
            this.etiquetaCondicion = cond;
            this.etiquetaCuerpo = cuerpo;
            this.etiquetaIncremento = inc;
            this.etiquetaFin = fin;
        }
    }

    /**
     * Inicia la generación de un for
     * Agrega etiquetas a las pilas para break/continue
     */
    public ContextoFor iniciarFor() {
        String etiqCond = nuevaEtiqueta();
        String etiqCuerpo = nuevaEtiqueta();
        String etiqInc = nuevaEtiqueta();
        String etiqFin = nuevaEtiqueta();
        pilaEtiquetasContinue.push(etiqInc);
        pilaEtiquetasBreak.push(etiqFin);
        return new ContextoFor(etiqCond, etiqCuerpo, etiqInc, etiqFin);
    }

    /**
     * Finaliza la generación de un for
     * Remueve etiquetas de las pilas
     */
    public void finalizarFor() {
        if (!pilaEtiquetasContinue.isEmpty()) {
            pilaEtiquetasContinue.pop();
        }
        if (!pilaEtiquetasBreak.isEmpty()) {
            pilaEtiquetasBreak.pop();
        }
    }

    // ==================== BREAK Y CONTINUE ====================

    /**
     * Genera código para break (salta al final del loop actual)
     */
    public void generarBreak() {
        if (!pilaEtiquetasBreak.isEmpty()) {
            generarGoto(pilaEtiquetasBreak.peek());
        } else {
            // Error: break fuera de un loop (debería detectarse en semántica)
            System.err.println("ERROR IR: break fuera de estructura de control");
        }
    }

    /**
     * Genera código para continue (salta al incremento o condición del loop)
     */
    public void generarContinue() {
        if (!pilaEtiquetasContinue.isEmpty()) {
            generarGoto(pilaEtiquetasContinue.peek());
        } else {
            // Error: continue fuera de un loop (debería detectarse en semántica)
            System.err.println("ERROR IR: continue fuera de estructura de control");
        }
    }

    // ==================== LLAMADAS A FUNCIONES ====================

    /**
     * Genera código para pasar un parámetro a una función
     * param valor
     */
    public void generarParam(String valor) {
        emitir("param", valor, "", "");
    }

    /**
     * Genera código para llamar a una función con valor de retorno
     * temp = call nombreFuncion, numParams
     * 
     * @return Temporal que contiene el valor retornado
     */
    public String generarLlamada(String nombreFuncion, int numParams) {
        String temp = nuevoTemporal();
        emitir("call", nombreFuncion, String.valueOf(numParams), temp);
        return temp;
    }

    /**
     * Genera código para llamar a una función void
     * call nombreFuncion, numParams
     */
    public void generarLlamadaVoid(String nombreFuncion, int numParams) {
        emitir("call", nombreFuncion, String.valueOf(numParams), "");
    }

    /**
     * Genera código para return
     * return valor (o return si es void)
     */
    public void generarReturn(String valor) {
        if (valor != null && !valor.isEmpty()) {
            emitir("return", valor, "", "");
        } else {
            emitir("return", "", "", "");
        }
    }

    // ==================== INPUT/OUTPUT ====================

    /**
     * Genera código para imprimir un valor
     * print valor
     */
    public void generarPrint(String valor) {
        emitir("print", valor, "", "");
    }

    /**
     * Genera código para leer un valor
     * temp = read()
     * 
     * @return Temporal que contiene el valor leído
     */
    public String generarRead() {
        String temp = nuevoTemporal();
        emitir("read", "", "", temp);
        return temp;
    }

    // ==================== ACCESO A RESULTADOS ====================

    /**
     * @return Lista de cuádruplos generados
     */
    public List<Cuadruplo> getCuadruplos() {
        return cuadruplos;
    }

    /**
     * @return Lista de triplos generados
     */
    public List<Triplo> getTriplos() {
        return triplos;
    }

    /**
     * @return Número total de cuádruplos generados
     */
    public int getCantidadCuadruplos() {
        return cuadruplos.size();
    }

    /**
     * @return Número total de triplos generados
     */
    public int getCantidadTriplos() {
        return triplos.size();
    }

    /**
     * Obtiene un cuádruplo específico por índice
     */
    public Cuadruplo getCuadruplo(int indice) {
        if (indice >= 0 && indice < cuadruplos.size()) {
            return cuadruplos.get(indice);
        }
        return null;
    }

    /**
     * Obtiene un triplo específico por índice
     */
    public Triplo getTriplo(int indice) {
        if (indice >= 0 && indice < triplos.size()) {
            return triplos.get(indice);
        }
        return null;
    }

    // ==================== MODIFICACIÓN DE IR (para optimizaciones)
    // ====================

    /**
     * Reemplaza un cuádruplo en un índice específico
     * También reconstruye los triplos para mantener sincronización
     */
    public void reemplazarCuadruplo(int indice, Cuadruplo nuevo) {
        if (indice >= 0 && indice < cuadruplos.size()) {
            cuadruplos.set(indice, nuevo);
            reconstruirTriplos();
        }
    }

    /**
     * Elimina un cuádruplo en un índice específico
     * También reconstruye los triplos para mantener sincronización
     */
    public void eliminarCuadruplo(int indice) {
        if (indice >= 0 && indice < cuadruplos.size()) {
            cuadruplos.remove(indice);
            reconstruirTriplos();
        }
    }

    /**
     * Inserta un cuádruplo en un índice específico
     * También reconstruye los triplos
     */
    public void insertarCuadruplo(int indice, Cuadruplo nuevo) {
        if (indice >= 0 && indice <= cuadruplos.size()) {
            cuadruplos.add(indice, nuevo);
            reconstruirTriplos();
        }
    }

    /**
     * Reconstruye la lista de triplos desde cero basándose en los cuádruplos
     * actuales
     * Necesario después de modificaciones para mantener sincronización
     */
    private void reconstruirTriplos() {
        triplos.clear();
        mapaTempATriplo.clear();

        for (int i = 0; i < cuadruplos.size(); i++) {
            Cuadruplo c = cuadruplos.get(i);
            Triplo t = convertirCuadruploATriplo(c, i);
            triplos.add(t);

            // Reconstruir mapeo de temporales
            if (c.getResultado() != null && !c.getResultado().isEmpty() &&
                    c.getResultado().startsWith("t")) {
                mapaTempATriplo.put(c.getResultado(), i);
            }
        }
    }

    // ==================== UTILIDADES ====================

    /**
     * Limpia todo el código intermedio generado
     */
    public void limpiar() {
        cuadruplos.clear();
        triplos.clear();
        mapaTempATriplo.clear();
        pilaEtiquetasContinue.clear();
        pilaEtiquetasBreak.clear();
        reiniciarContadores();
    }

    /**
     * Imprime los cuádruplos a consola (para debugging)
     */
    public void imprimirCuadruplos() {
        System.out.println("\n=== CUÁDRUPLOS ===");
        for (int i = 0; i < cuadruplos.size(); i++) {
            System.out.printf("%3d: %s\n", i, cuadruplos.get(i).toReadableString());
        }
    }

    /**
     * Imprime los triplos a consola (para debugging)
     */
    public void imprimirTriplos() {
        System.out.println("\n=== TRIPLOS ===");
        for (Triplo t : triplos) {
            System.out.println(t.toReadableString());
        }
    }
}
