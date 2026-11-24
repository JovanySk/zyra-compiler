// Archivo: src/modulos/AnalizadorLexico.java
// VERSIÓN MANUAL, COMPLETA Y DEFINITIVA
package modulos;

import estructuras.Token;
import estructuras.TablaTokens;
import excepciones.lexicas.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AnalizadorLexico {

    private final BufferedReader codigoFuente;
    private final TablaTokens tt;
    private int lineaActual;
    private int columnaActual;
    private char caracterActual;
    private boolean eofAlcanzado = false;

    public AnalizadorLexico(BufferedReader reader) throws IOException {
        this.codigoFuente = reader;
        this.tt = new TablaTokens();
        this.lineaActual = 1;
        this.columnaActual = 0;
        leerSiguienteCaracter();
    }

    private void leerSiguienteCaracter() throws IOException {
        int charCode = codigoFuente.read();
        if (charCode == -1) {
            caracterActual = (char) -1;
            eofAlcanzado = true;
        } else {
            caracterActual = (char) charCode;
            if (caracterActual == '\n') {
                lineaActual++;
                columnaActual = 0;
            } else {
                columnaActual++;
            }
        }
    }

    public List<Token> getAllTokens() throws ExcepcionLexica, IOException {
        List<Token> tokens = new ArrayList<>();
        Token token;
        do {
            token = getSiguienteToken();
            tokens.add(token);
        } while (token != null && !token.getNombre().equals(tt.EOF));
        return tokens;
    }

    private Token getSiguienteToken() throws ExcepcionLexica, IOException {
        while (true) {
            if (Character.isWhitespace(caracterActual)) {
                leerSiguienteCaracter();
                continue;
            }
            if (caracterActual == '/') {
                int colInicioComentario = columnaActual;
                leerSiguienteCaracter();
                if (caracterActual == '/') {
                    while (caracterActual != '\n' && !eofAlcanzado) {
                        leerSiguienteCaracter();
                    }
                    continue; 
                } else if (caracterActual == '*') {
                    leerSiguienteCaracter();
                    char anterior = ' ';
                    while (true) {
                        if (eofAlcanzado) {
                            throw new ExcepcionComentarioMalFormado(lineaActual, colInicioComentario);
                        }
                        if (anterior == '*' && caracterActual == '/') {
                            leerSiguienteCaracter();
                            break; 
                        }
                        anterior = caracterActual;
                        leerSiguienteCaracter();
                    }
                    continue; 
                } else {
                    return new Token(tt.OP_DIV, "/", lineaActual, colInicioComentario);
                }
            }
            break; 
        }

        if (eofAlcanzado) {
            return new Token(tt.EOF, "EOF", lineaActual, columnaActual);
        }

        int colInicioToken = columnaActual;

        if (Character.isLetter(caracterActual)) {
            return manejarIdentificadorYPalabraReservada(colInicioToken);
        }
        if (Character.isDigit(caracterActual)) {
            return manejarNumero(colInicioToken);
        }
        if (caracterActual == '"') {
            return manejarLiteralString(colInicioToken);
        }
        if (caracterActual == '\'') {
            return manejarLiteralChar(colInicioToken);
        }

        switch (caracterActual) {
            case '(': return crearTokenYLeerSiguiente(tt.P_ABRE, "(", colInicioToken);
            case ')': return crearTokenYLeerSiguiente(tt.P_CIERRA, ")", colInicioToken);
            case '{': return crearTokenYLeerSiguiente(tt.LLAVE_ABRE, "{", colInicioToken);
            case '}': return crearTokenYLeerSiguiente(tt.LLAVE_CIERRA, "}", colInicioToken);
            case '[': return crearTokenYLeerSiguiente(tt.C_ABRE, "[", colInicioToken);
            case ']': return crearTokenYLeerSiguiente(tt.C_CIERRA, "]", colInicioToken);
            case ';': return crearTokenYLeerSiguiente(tt.PUNTO_Y_COMA, ";", colInicioToken);
            case ',': return crearTokenYLeerSiguiente(tt.COMA, ",", colInicioToken);
            case '.': return crearTokenYLeerSiguiente(tt.PUNTO, ".", colInicioToken);
            case '+': return crearTokenYLeerSiguiente(tt.OP_SUMA, "+", colInicioToken);
            case '-': return crearTokenYLeerSiguiente(tt.OP_RESTA, "-", colInicioToken);
            case '*': return crearTokenYLeerSiguiente(tt.OP_MULT, "*", colInicioToken);
            case '%': return crearTokenYLeerSiguiente(tt.OP_MOD, "%", colInicioToken);
            case ':': return crearTokenYLeerSiguiente(tt.DOS_PUNTOS, ":", colInicioToken);

            case '=':
                leerSiguienteCaracter();
                if (caracterActual == '=') {
                    return crearTokenYLeerSiguiente(tt.OP_IGUAL, "==", colInicioToken);
                }
                return new Token(tt.OP_ASIG, "=", lineaActual, colInicioToken);
            
            case '!':
                leerSiguienteCaracter();
                if (caracterActual == '=') {
                    return crearTokenYLeerSiguiente(tt.OP_DIST, "!=", colInicioToken);
                }
                return new Token(tt.OP_NOT, "!", lineaActual, colInicioToken);

            case '<':
                leerSiguienteCaracter();
                if (caracterActual == '=') {
                    return crearTokenYLeerSiguiente(tt.OP_MEN_IG, "<=", colInicioToken);
                }
                return new Token(tt.OP_MENOR, "<", lineaActual, colInicioToken);

            case '>':
                leerSiguienteCaracter();
                if (caracterActual == '=') {
                    return crearTokenYLeerSiguiente(tt.OP_MAY_IG, ">=", colInicioToken);
                }
                return new Token(tt.OP_MAYOR, ">", lineaActual, colInicioToken);

            case '&':
                char charAnterior = caracterActual;
                leerSiguienteCaracter();
                if (caracterActual == '&') {
                    return crearTokenYLeerSiguiente(tt.OP_AND, "&&", colInicioToken);
                }
                throw new ExcepcionOperadorInvalido(lineaActual, colInicioToken, charAnterior);
            
            case '|':
                charAnterior = caracterActual;
                leerSiguienteCaracter();
                if (caracterActual == '|') {
                    return crearTokenYLeerSiguiente(tt.OP_OR, "||", colInicioToken);
                }
                throw new ExcepcionOperadorInvalido(lineaActual, colInicioToken, charAnterior);

            default:
                char charInvalido = caracterActual;
                leerSiguienteCaracter(); 
                throw new ExcepcionCharInvalido(lineaActual, colInicioToken, charInvalido);
        }
    }

    private Token crearTokenYLeerSiguiente(String nombre, String lexema, int columna) throws IOException {
        Token t = new Token(nombre, lexema, lineaActual, columna);
        leerSiguienteCaracter();
        return t;
    }

    private Token manejarIdentificadorYPalabraReservada(int col) throws IOException, ExcepcionLexica {
        StringBuilder sb = new StringBuilder();
        while (Character.isLetterOrDigit(caracterActual) || caracterActual == '_') {
            sb.append(caracterActual);
            leerSiguienteCaracter();
        }
        String lexema = sb.toString();
        
        String nombreToken = tt.getTipoPalabraReservada(lexema);
        if (nombreToken != null) {
            return new Token(nombreToken, lexema, lineaActual, col);
        }

        String lexemaMinusculas = lexema.toLowerCase();
        if (tt.getTipoPalabraReservada(lexemaMinusculas) != null) {
            throw new ExcepcionPalabraReservadaMayuscula(lineaActual, col, lexema);
        }

        if (lexema.equalsIgnoreCase("System") && !lexema.equals("System")) {
            throw new ExcepcionIdentificadorInvalido(lineaActual, col, lexema, "System");
        }
        if (lexema.equalsIgnoreCase("SystemIO") && !lexema.equals("SystemIO")) {
            throw new ExcepcionIdentificadorInvalido(lineaActual, col, lexema, "SystemIO");
        }
        
        if (Character.isUpperCase(lexema.charAt(0))) {
            return new Token(tt.ID_CLASE, lexema, lineaActual, col);
        } else {
            return new Token(tt.ID_MET_VAR, lexema, lineaActual, col);
        }
    }
    
    private Token manejarNumero(int col) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(caracterActual)) {
            sb.append(caracterActual);
            leerSiguienteCaracter();
        }

        if (caracterActual == '.') {
            sb.append(caracterActual);
            leerSiguienteCaracter();
            while (Character.isDigit(caracterActual)) {
                sb.append(caracterActual);
                leerSiguienteCaracter();
            }
            return new Token(tt.LIT_DECIMAL, sb.toString(), lineaActual, col);
        }

        return new Token(tt.LIT_INT, sb.toString(), lineaActual, col);
    }
    
    private Token manejarLiteralString(int col) throws ExcepcionStringMalFormado, IOException {
        StringBuilder sb = new StringBuilder();
        leerSiguienteCaracter();
        while (caracterActual != '"') {
            if (eofAlcanzado || caracterActual == '\n') {
                throw new ExcepcionStringMalFormado(lineaActual, col);
            }
            sb.append(caracterActual);
            leerSiguienteCaracter();
        }
        leerSiguienteCaracter();
        return new Token(tt.LIT_STRING, sb.toString(), lineaActual, col);
    }

    private Token manejarLiteralChar(int col) throws ExcepcionCharMalFormado, IOException {
        leerSiguienteCaracter();
        char valorChar = caracterActual;
        if (valorChar == '\'' || eofAlcanzado) {
             throw new ExcepcionCharMalFormado(lineaActual, col);
        }
        leerSiguienteCaracter();
        if (caracterActual != '\'') {
             while(caracterActual != '\'' && !eofAlcanzado && caracterActual != '\n'){
                leerSiguienteCaracter();
             }
             throw new ExcepcionCharMalFormado(lineaActual, col);
        }
        leerSiguienteCaracter();
        return new Token(tt.LIT_CHAR, String.valueOf(valorChar), lineaActual, col);
    }
}
