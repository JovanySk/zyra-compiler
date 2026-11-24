// Contenido completo y actualizado de Token.java
package estructuras;

public class Token {
	
	private String nombre;
	private String lexema;
	private int nroLinea;
	private int columna; 
	// --- CONSTRUCTOR MODIFICADO ---
	public Token(String nombre, String lexema, int nroLinea, int columna) {
		this.nombre = nombre;
		this.lexema = lexema;
		this.nroLinea = nroLinea;
		this.columna = columna; 
	}
	
	public String getNombre() {
		return nombre;
	}
	
	public String getLexema() {
		return lexema;
	}
	
	public int getLinea() {
		return nroLinea;
	}

	// --- MÉTODO NUEVO ---
	public int getColumna() {
		return columna;
	}
	
	public boolean comparar(String msg){
		return nombre.equals(msg);
	}
}