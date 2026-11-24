// Archivo: src/estructuras/TablaTokens.java
// VERSIÓN COMPLETA, FINAL Y CORREGIDA
package estructuras;

import java.util.HashMap;
import java.util.Map;

public final class TablaTokens {

    // Se mantiene la tabla de palabras reservadas para el lexer
    private final HashMap<String, String> palabrasReservadas;
    // Tabla estática para la traducción inversa (usada en mensajes de error)
    private static HashMap<String, String> tablaInvertida;

    // --- Definición de Constantes de Tokens ---
    public final String PR_CLASS = "PR_Class";
    public final String PR_EXTENDS = "PR_Extends";
    public final String PR_STATIC = "PR_Static";
    public final String PR_DYNAMIC = "PR_Dynamic";
    public final String PR_VOID = "PR_Void";
    public final String PR_BOOLEAN = "PR_Boolean";
    public final String PR_CHAR = "PR_Char";
    public final String PR_INT = "PR_Int";
    public final String PR_STRING = "PR_String";
    public final String PR_IF = "PR_If";
    public final String PR_ELSE = "PR_Else";
    public final String PR_WHILE = "PR_While";
    public final String PR_FOR = "PR_For";
    public final String PR_RETURN = "PR_Return";
    public final String PR_INSTANCEOF = "PR_Instanceof";
    public final String PR_THIS = "PR_This";
    public final String PR_NEW = "PR_New";
    public final String PR_NULL = "PR_Null";
    public final String PR_TRUE = "PR_True";
    public final String PR_FALSE = "PR_False";
    public final String PR_PUBLIC = "PR_Public";
    public final String PR_PRIVATE = "PR_Private";
    public final String PR_BREAK = "PR_Break";
    public final String PR_CASE = "PR_Case";
    public final String PR_DEFAULT = "PR_Default";
    public final String PR_SWITCH = "PR_Switch";
    public final String P_ABRE = "P_Parentesis_A";
    public final String P_CIERRA = "P_Parentesis_C";
    public final String LLAVE_ABRE = "P_Llave_A";
    public final String LLAVE_CIERRA = "P_Llave_C";
    public final String C_ABRE = "P_Corchetes_A";
    public final String C_CIERRA = "P_Corchetes_C";
    public final String PUNTO = "P_Punto"; // <-- AÑADIDO Y CORREGIDO
    public final String PUNTO_Y_COMA = "P_Puntocoma";
    public final String COMA = "P_Coma";
    public final String DOS_PUNTOS = "P_DosPuntos";
    public final String OP_MAYOR = "O_Mayor";
    public final String OP_MENOR = "O_Menor";
    public final String OP_NOT = "O_Not";
    public final String OP_ASIG = "O_Asignacion";
    public final String OP_IGUAL = "O_Comparacion";
    public final String OP_MAY_IG = "O_Mayorigual";
    public final String OP_MEN_IG = "O_Menorigual";
    public final String OP_DIST = "O_Distinto";
    public final String OP_SUMA = "O_Suma";
    public final String OP_RESTA = "O_Resta";
    public final String OP_MULT = "O_Mult";
    public final String OP_DIV = "O_Div";
    public final String OP_MOD = "O_Mod";
    public final String OP_OR = "O_Or";
    public final String OP_AND = "O_And";
    public final String ID_CLASE = "idClase";
    public final String ID_MET_VAR = "idMetVar";
    public final String LIT_INT = "L_Entero";
    public final String LIT_DECIMAL = "L_Decimal";
    public final String LIT_CHAR = "L_Caracter";
    public final String LIT_STRING = "L_String";
    public final String EOF = "EOF";


    public TablaTokens() {
        palabrasReservadas = new HashMap<>();
        inicializarPalabrasReservadas();
    }

    private void inicializarPalabrasReservadas() {
        palabrasReservadas.put("class", PR_CLASS);
        palabrasReservadas.put("extends", PR_EXTENDS);
        palabrasReservadas.put("static", PR_STATIC);
        palabrasReservadas.put("dynamic", PR_DYNAMIC);
        palabrasReservadas.put("void", PR_VOID);
        palabrasReservadas.put("boolean", PR_BOOLEAN);
        palabrasReservadas.put("char", PR_CHAR);
        palabrasReservadas.put("int", PR_INT);
        palabrasReservadas.put("String", PR_STRING);
        palabrasReservadas.put("if", PR_IF);
        palabrasReservadas.put("else", PR_ELSE);
        palabrasReservadas.put("while", PR_WHILE);
        palabrasReservadas.put("for", PR_FOR);
        palabrasReservadas.put("return", PR_RETURN);
        palabrasReservadas.put("instanceof", PR_INSTANCEOF);
        palabrasReservadas.put("this", PR_THIS);
        palabrasReservadas.put("new", PR_NEW);
        palabrasReservadas.put("null", PR_NULL);
        palabrasReservadas.put("true", PR_TRUE);
        palabrasReservadas.put("false", PR_FALSE);
        palabrasReservadas.put("public", PR_PUBLIC);
        palabrasReservadas.put("private", PR_PRIVATE);
        palabrasReservadas.put("break", PR_BREAK);
        palabrasReservadas.put("case", PR_CASE);
        palabrasReservadas.put("default", PR_DEFAULT);
        palabrasReservadas.put("switch", PR_SWITCH);
    }
    
    public String getTipoPalabraReservada(String lexema) {
        return palabrasReservadas.get(lexema);
    }

    public static String traducirToken(String nombreToken) {
        if (tablaInvertida == null) {
            inicializarTablaInvertida();
        }
        String lexema = tablaInvertida.get(nombreToken);
        return lexema != null ? lexema : nombreToken;
    }

    private static void inicializarTablaInvertida() {
        tablaInvertida = new HashMap<>();
        TablaTokens tt = new TablaTokens();
        
        for (Map.Entry<String, String> entry : tt.palabrasReservadas.entrySet()) {
            tablaInvertida.put(entry.getValue(), entry.getKey());
        }

        tablaInvertida.put(tt.P_ABRE, "(");
        tablaInvertida.put(tt.P_CIERRA, ")");
        tablaInvertida.put(tt.LLAVE_ABRE, "{");
        tablaInvertida.put(tt.LLAVE_CIERRA, "}");
        tablaInvertida.put(tt.C_ABRE, "[");
        tablaInvertida.put(tt.C_CIERRA, "]");
        tablaInvertida.put(tt.PUNTO_Y_COMA, ";");
        tablaInvertida.put(tt.COMA, ",");
        tablaInvertida.put(tt.PUNTO, ".");
        tablaInvertida.put(tt.DOS_PUNTOS, ":");
        tablaInvertida.put(tt.OP_ASIG, "=");
        tablaInvertida.put(tt.OP_IGUAL, "==");
        tablaInvertida.put(tt.OP_DIST, "!=");
        tablaInvertida.put(tt.OP_MENOR, "<");
        tablaInvertida.put(tt.OP_MAYOR, ">");
        tablaInvertida.put(tt.OP_MEN_IG, "<=");
        tablaInvertida.put(tt.OP_MAY_IG, ">=");
        tablaInvertida.put(tt.OP_SUMA, "+");
        tablaInvertida.put(tt.OP_RESTA, "-");
        tablaInvertida.put(tt.OP_MULT, "*");
        tablaInvertida.put(tt.OP_DIV, "/");
        tablaInvertida.put(tt.OP_MOD, "%");
        tablaInvertida.put(tt.OP_AND, "&&");
        tablaInvertida.put(tt.OP_OR, "||");  
        tablaInvertida.put(tt.OP_NOT, "!");
        tablaInvertida.put(tt.EOF, "Fin de Archivo");
    }
}
