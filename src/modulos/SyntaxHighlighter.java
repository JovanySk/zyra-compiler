package modulos;

import estructuras.Token;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Clase encargada de colorear la sintaxis de un JTextPane en tiempo real.
 * Utiliza el AnalizadorLexico del compilador para asegurar consistencia.
 */
public class SyntaxHighlighter {
    private final JTextPane textPane;
    private final StyledDocument styledDoc;
    private final Style keywordStyle;
    private final Style typeStyle;
    private final Style literalStyle;
    private final Style commentStyle; // Aunque el lexer los salta, es bueno tenerlo
    private final Style defaultStyle;

    public SyntaxHighlighter(JTextPane pane) {
        this.textPane = pane;
        this.styledDoc = pane.getStyledDocument();

        // --- Definición de Colores ---

        // Estilo por defecto (identificadores, operadores, etc.)
        defaultStyle = styledDoc.addStyle("Default", null);
        StyleConstants.setForeground(defaultStyle, Color.WHITE);

        // Estilo para palabras clave (class, if, while, return, etc.)
        keywordStyle = styledDoc.addStyle("Keyword", defaultStyle);
        StyleConstants.setForeground(keywordStyle, new Color(215, 150, 80)); // Naranja
        StyleConstants.setBold(keywordStyle, true);
        
        // Estilo para tipos (int, boolean, y nombres de Clases)
        typeStyle = styledDoc.addStyle("Type", defaultStyle);
        StyleConstants.setForeground(typeStyle, new Color(78, 201, 176)); // Verde azulado

        // Estilo para literales (numeros, strings, "true", "false", 'c')
        literalStyle = styledDoc.addStyle("Literal", defaultStyle);
        StyleConstants.setForeground(literalStyle, new Color(181, 206, 168)); // Verde pálido
        
        // Estilo para comentarios
        commentStyle = styledDoc.addStyle("Comment", defaultStyle);
        StyleConstants.setForeground(commentStyle, new Color(106, 153, 85)); // Verde oscuro
        StyleConstants.setItalic(commentStyle, true);
    }

    /**
     * Procesa el texto del JTextPane y aplica los estilos de color.
     * Se ejecuta en el hilo de la GUI para evitar problemas de concurrencia.
     */
    public void colorear() {
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Obtiene el contenido y lo limpia de estilos previos
                String content = styledDoc.getText(0, styledDoc.getLength());
                styledDoc.setCharacterAttributes(0, content.length(), defaultStyle, true);

                if (content.isEmpty()) {
                    return;
                }
                
                // 2. Usa tu AnalizadorLexico para obtener los tokens
                // (Se debe manejar la IOException aunque con StringReader es improbable)
                try {
                    AnalizadorLexico lexico = new AnalizadorLexico(new BufferedReader(new StringReader(content)));
                    List<Token> tokens = lexico.getAllTokens();
                    
                    // 3. Itera sobre los tokens y aplica el estilo correspondiente
                    for (Token token : tokens) {
                        if (token.getNombre().equals("EOF")) continue;

                        int start = getOffset(token.getLinea(), token.getColumna());
                        int length = token.getLexema().length();
                        
                        if (start < 0 || (start + length) > styledDoc.getLength()) {
                            continue; // Evita errores si el offset está fuera de rango
                        }
                        
                        Style styleToApply = getStyleForToken(token.getNombre());
                        styledDoc.setCharacterAttributes(start, length, styleToApply, true);
                    }
                } catch (Exception lexEx) {
                    // Si el lexer falla (p.ej. caracter inválido), no hacer nada para no interrumpir al usuario.
                }

            } catch (BadLocationException e) {
                // Error al obtener el texto, muy improbable.
            }
        });
    }
    
    /**
     * Asigna un estilo de color basado en el nombre del token.
     */
    private Style getStyleForToken(String tokenName) {
        if (tokenName.startsWith("PR_")) { // Palabras Reservadas
             if (tokenName.equals("PR_True") || tokenName.equals("PR_False") || tokenName.equals("PR_Null")) {
                 return literalStyle;
             }
             if (tokenName.equals("PR_Boolean") || tokenName.equals("PR_Char") || tokenName.equals("PR_Int") || tokenName.equals("PR_String") || tokenName.equals("PR_Void")) {
                 return typeStyle;
             }
            return keywordStyle;
        } else if (tokenName.startsWith("L_")) { // Literales
            return literalStyle;
        } else if (tokenName.equals("idClase")) { // Identificadores de Clase
            return typeStyle;
        }
        // Nota: Los comentarios son omitidos por tu lexer, si no, se agregarían aquí.
        return defaultStyle;
    }

    /**
     * Convierte la posición (línea, columna) de un token a un offset
     * dentro del documento del JTextPane.
     */
    private int getOffset(int line, int col) throws BadLocationException {
        Element root = styledDoc.getDefaultRootElement();
        if (line < 1 || line > root.getElementCount()) {
            return -1;
        }
        Element lineElement = root.getElement(line - 1);
        int lineStartOffset = lineElement.getStartOffset();
        
        return lineStartOffset + col - 1;
    }
}