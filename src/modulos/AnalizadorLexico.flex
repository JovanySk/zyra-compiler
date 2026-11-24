// Contenido para el nuevo archivo: AnalizadorLexico.flex
package modulos;

import estructuras.Token;
import estructuras.TablaTokens;
import excepciones.lexicas.*;

%%

%class AnalizadorLexico
%public
%final
%integer
%line
%column
%char
%cup
%unicode

%{
    // Buffer para construir strings
    private StringBuffer string = new StringBuffer();

    // Referencia a la tabla de tokens para no usar literales mágicos
    private TablaTokens tt = new TablaTokens();

    // Método para crear un nuevo símbolo (token) para CUP
    private Token symbol(String name, String lexema) {
        return new Token(name, lexema, yyline + 1, yycolumn + 1);
    }
%}

// Definiciones Regulares
SaltoLinea = \r|\n|\r\n
EspacioBlanco = {SaltoLinea} | [ \t\f]

Digito = [0-9]
Letra = [a-zA-Z]
IdentificadorClase = [A-Z]({Letra}|{Digito})*
IdentificadorMetVar = [a-z]({Letra}|{Digito})*

LiteralEntero = {Digito}+
LiteralChar = \' ( [^\'\\\r\n] | (\\.) ) \'
LiteralString = \" ( [^\"\\\r\n] | (\\.) )* \"

// Comentarios
ComentarioLinea = "//" [^\r\n]* {SaltoLinea}?
ComentarioBloque = "/*" [^*] ~"*/" | "/*" "*"+ "/"

%%

<YYINITIAL> {
    // Palabras Reservadas
    "class"             { return symbol(tt.PR_CLASS, yytext()); }
    "extends"           { return symbol(tt.PR_EXTENDS, yytext()); }
    "static"            { return symbol(tt.PR_STATIC, yytext()); }
    "dynamic"           { return symbol(tt.PR_DYNAMIC, yytext()); }
    "void"              { return symbol(tt.PR_VOID, yytext()); }
    "boolean"           { return symbol(tt.PR_BOOLEAN, yytext()); }
    "char"              { return symbol(tt.PR_CHAR, yytext()); }
    "int"               { return symbol(tt.PR_INT, yytext()); }
    "String"            { return symbol(tt.PR_STRING, yytext()); }
    "if"                { return symbol(tt.PR_IF, yytext()); }
    "else"              { return symbol(tt.PR_ELSE, yytext()); }
    "while"             { return symbol(tt.PR_WHILE, yytext()); }
    "for"               { return symbol(tt.PR_FOR, yytext()); }
    "return"            { return symbol(tt.PR_RETURN, yytext()); }
    "instanceof"        { return symbol(tt.PR_INSTANCEOF, yytext()); }
    "new"               { return symbol(tt.PR_NEW, yytext()); }
    "null"              { return symbol(tt.PR_NULL, yytext()); }
    "true"              { return symbol(tt.PR_TRUE, yytext()); }
    "false"             { return symbol(tt.PR_FALSE, yytext()); }
    "this"              { return symbol(tt.PR_THIS, yytext()); }

    // Identificadores
    {IdentificadorClase}    { return symbol(tt.ID_CLASE, yytext()); }
    {IdentificadorMetVar}   { return symbol(tt.ID_MET_VAR, yytext()); }

    // Literales
    {LiteralEntero}     { return symbol(tt.LIT_INT, yytext()); }
    {LiteralChar}       { return symbol(tt.LIT_CHAR, yytext()); }
    {LiteralString}     { return symbol(tt.LIT_STRING, yytext()); }

    // Operadores y Delimitadores
    "("                 { return symbol(tt.P_ABRE, yytext()); }
    ")"                 { return symbol(tt.P_CIERRA, yytext()); }
    "{"                 { return symbol(tt.LLAVE_ABRE, yytext()); }
    "}"                 { return symbol(tt.LLAVE_CIERRA, yytext()); }
    "["                 { return symbol(tt.C_ABRE, yytext()); }
    "]"                 { return symbol(tt.C_CIERRA, yytext()); }
    ";"                 { return symbol(tt.PUNTO_Y_COMA, yytext()); }
    ","                 { return symbol(tt.COMA, yytext()); }
    "."                 { return symbol(tt.PUNTO, yytext()); }
    "="                 { return symbol(tt.OP_ASIG, yytext()); }
    "=="                { return symbol(tt.OP_IGUAL, yytext()); }
    "!="                { return symbol(tt.OP_DIST, yytext()); }
    "<"                 { return symbol(tt.OP_MENOR, yytext()); }
    ">"                 { return symbol(tt.OP_MAYOR, yytext()); }
    "<="                { return symbol(tt.OP_MEN_IG, yytext()); }
    ">="                { return symbol(tt.OP_MAY_IG, yytext()); }
    "+"                 { return symbol(tt.OP_SUMA, yytext()); }
    "-"                 { return symbol(tt.OP_RESTA, yytext()); }
    "*"                 { return symbol(tt.OP_MULT, yytext()); }
    "/"                 { return symbol(tt.OP_DIV, yytext()); }
    "%"                 { return symbol(tt.OP_MOD, yytext()); }
    "&&"                { return symbol(tt.OP_AND, yytext()); }
    "||"                { return symbol(tt.OP_OR, yytext()); }
    "!"                 { return symbol(tt.OP_NOT, yytext()); }

    // Ignorar Espacios en Blanco y Comentarios
    {EspacioBlanco}     { /* No hacer nada */ }
    {ComentarioLinea}   { /* No hacer nada */ }
    {ComentarioBloque}  { /* No hacer nada */ }
}

// Manejo de errores: cualquier otro caracter es un error
[^] { throw new ExcepcionCharInvalido(yyline + 1, yycolumn + 1, yytext()); }