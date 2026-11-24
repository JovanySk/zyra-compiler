// Archivo: src/Vistas/AstTreeCellRenderer.java
// VERSIÓN DEFINITIVA CORREGIDA (Error de casteo solucionado)
package Vistas;

import estructuras.ast.*;
import estructuras.ast.expresiones.*;
import estructuras.ast.sentencias.*;
import estructuras.ts.variables.VarLocal;
import estructuras.ts.variables.VarParametro;
import java.awt.Component;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

public class AstTreeCellRenderer extends DefaultTreeCellRenderer {

    // Caracteres Unicode para dibujar el árbol
    private final String PREFIJO_RAMA = "├─ ";
    private final String PREFIJO_ULTIMA_RAMA = "└─ ";
    private final String PREFIJO_VERTICAL = "│  ";
    private final String PREFIJO_VACIO = "   ";

    public AstTreeCellRenderer() {
        // Constructor vacío.
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        
        setIcon(null); // Nos aseguramos de que no haya íconos

        if (value instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode guiNode = (DefaultMutableTreeNode) value;
            Object userObject = guiNode.getUserObject();
            
            // Construimos el prefijo con las líneas de conexión
            StringBuilder prefix = new StringBuilder();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) guiNode.getParent();
            
            while (parent != null) {
                DefaultMutableTreeNode grandParent = (DefaultMutableTreeNode) parent.getParent();
                if (grandParent != null) {
                    // << ===================== LÍNEA CORREGIDA #1 ===================== >>
                    // Se añade el casteo a (DefaultMutableTreeNode) para usar getLastChild()
                    boolean esUltimo = ((DefaultMutableTreeNode) parent.getParent()).getLastChild() == parent;
                    prefix.insert(0, esUltimo ? PREFIJO_VACIO : PREFIJO_VERTICAL);
                }
                parent = (DefaultMutableTreeNode) parent.getParent();
            }

            // Añadimos el conector final (├─ o └─)
            if (guiNode.getParent() != null) {
                // << ===================== LÍNEA CORREGIDA #2 ===================== >>
                // Se añade el casteo a (DefaultMutableTreeNode) para usar getLastChild()
                boolean esUltimo = ((DefaultMutableTreeNode) guiNode.getParent()).getLastChild() == guiNode;
                prefix.append(esUltimo ? PREFIJO_ULTIMA_RAMA : PREFIJO_RAMA);
            }

            // Asignamos el texto descriptivo del nodo
            setText(prefix.toString() + getTextoParaNodo(userObject));
        }
        
        return this;
    }

    private String getTextoParaNodo(Object nodo) {
        if (nodo == null) return "";

        // Nodos Principales
        if (nodo instanceof NodoPrograma) return "Programa";
        if (nodo instanceof NodoClase) return "Clase: " + ((NodoClase) nodo).getToken().getLexema();
        if (nodo instanceof NodoMetodo) return "Método: " + ((NodoMetodo) nodo).getToken().getLexema();
        if (nodo instanceof NodoAtributo) {
            NodoAtributo n = (NodoAtributo) nodo;
            return "Atributo: " + n.getTipo().getNombreClase() + " " + n.getToken().getLexema();
        }

        // Nodos de Sentencias
        if (nodo instanceof NodoBloque) return "Bloque de Sentencias";
        if (nodo instanceof NodoAsignacion) return "Asignación (" + ((NodoAsignacion) nodo).getSimbolo().getLexema() + ")";
        if (nodo instanceof NodoDeclaracion) {
            return "Declaración"; // Ahora es solo un contenedor
        }
        if (nodo instanceof VarLocal) {
            VarLocal n = (VarLocal) nodo;
            return n.getTipoVar().getNombreClase() + " " + n.getToken().getLexema();
        }
        if (nodo instanceof NodoIf) return "Sentencia If";
        if (nodo instanceof NodoWhile) return "Sentencia While";
        if (nodo instanceof NodoFor) return "Sentencia For";
        if (nodo instanceof NodoReturn) return "Sentencia Return";
        if (nodo instanceof NodoSentenciaLlamada) return "Llamada: " + ((NodoSentenciaLlamada) nodo).getToken().getLexema();
        if (nodo instanceof NodoSwitch) return "Sentencia Switch";
        if (nodo instanceof NodoCase) {
            NodoCase n = (NodoCase) nodo;
            if (n.esDefault()) return "Default:";
            return "Case: " + n.getValor().getToken().getLexema();
        }
        if (nodo instanceof NodoBreak) return "Sentencia Break";

        // Nodos de Expresiones
        if (nodo instanceof NodoExpresionBinaria) return "Expresión (" + ((NodoExpresionBinaria) nodo).getSimbolo().getLexema() + ")";
        if (nodo instanceof NodoExpresionUnaria) return "Expresión (" + ((NodoExpresionUnaria) nodo).getSimbolo().getLexema() + ")";
        if (nodo instanceof NodoLiteral) return "Literal: " + ((NodoLiteral) nodo).getToken().getLexema();
        if (nodo instanceof NodoVariable) return "Variable: " + ((NodoVariable) nodo).getToken().getLexema();
        if (nodo instanceof NodoThis) return "this";
        
        // Nodos auxiliares para organización
        if (nodo instanceof String && ((String)nodo).equals("Parámetros")) return "Parámetros";
        if (nodo instanceof VarParametro) {
            VarParametro p = (VarParametro) nodo;
            return p.getTipoVar().getNombreClase() + " " + p.getToken().getLexema();
        }

        // Fallback por si algún nodo no fue cubierto
        return nodo.getClass().getSimpleName();
    }
}