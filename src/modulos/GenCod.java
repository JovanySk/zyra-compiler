// Contenido completo y CORREGIDO
package modulos;

import estructuras.Token;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;

public class GenCod {
	
	public static GenCod GCI;
	private static BufferedWriter bw;
	private static int cantEtiquetas;
	private static int cantClases;
    
    // --- NUEVO: Almacenamiento centralizado para literales de string ---
    private static LinkedList<Token> stringLiterals = new LinkedList<>();
    private static LinkedList<String> stringLabels = new LinkedList<>();

	private GenCod() {
		GCI = this;
		bw = null;
		cantEtiquetas = 0;
		cantClases = 0;
	}
	
	public static void setBuffer(BufferedWriter buffer) {
		bw = buffer;
	}
	
	/* Escribe una secuencia de caracteres dada en la misma linea actual. */
	public static void write(String msg) {
		try {
			if (msg!=null)
				bw.write(msg);
			else bw.write("");
		} catch (IOException e) {
			System.out.println("Error al intentar escribir el archivo de salida.");
		}
	}
	
	/* Escribe un label en una misma linea (ojo, no baja de linea!) */
	public static void label(String lbl) {
		write(lbl+": ");
	}
	
	/* Escribe una instruccion en una nueva linea */
	public static void gen(String inst) {
		write(String.format("%-18s %-15s %n"," ",inst));
	}
	
	/* Escribe un comentario en una nueva linea */
	public static void comment(String com) {
		write(String.format("%-33s  ; %s %n"," ",com));
	}
	
	/* Escribe una instruccion seguido de un comentario en una nueva linea */
	public static void gen(String inst, String comment) {
		write(String.format("%-18s %-15s ; %s %n"," ",inst,comment));
	}
	
	/* Escribe un label, una instruccion y un comentario en una nueva linea */
	public static void gen(String lbl, String inst, String comment) {
		write(String.format("%s: %-15s ; %s %n",lbl,inst,comment));
	}
	
	/* Cierra el Buffered Writer asociado al generador de codigo */
	public static void close() throws IOException {
		if (bw!=null)
			bw.close();
	}
	
	public static void code() {
		write(".code \n");
	}
	
	public static void data() {
		write(".data \n");
	}
	
	public static String generarEtiqueta() {
		String label = ""+cantEtiquetas;
		cantEtiquetas++;
		return label;
	}
	
	public static int generarNumClase() {
		return cantClases++;
	}

    // --- NUEVOS MÉTODOS PARA MANEJO DE STRINGS ---
    public static String agregarStringLiteral(Token t) {
        String etiqueta = "str_" + generarEtiqueta();
        stringLiterals.add(t);
        stringLabels.add(etiqueta);
        return etiqueta;
    }
    
    public static void generarDataStrings() {
        for (int i = 0; i < stringLiterals.size(); i++) {
            String etiqueta = stringLabels.get(i);
            String literal = stringLiterals.get(i).getLexema();
            gen(etiqueta, "DW \"" + literal + "\",0", "Guardo un string");
        }
    }
}
