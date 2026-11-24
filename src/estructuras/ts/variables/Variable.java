// Contenido completo y CORREGIDO
package estructuras.ts.variables;

import estructuras.Token;
import estructuras.ts.tipos.Tipo;

public abstract class Variable implements Comparable<Variable> {
	
	private Tipo tipoVar;
	private Token identificador;
	private int offset;
    private String rol;     // NUEVO: "Parámetro", "Variable Local", "Atributo"
    private Object valor;   // NUEVO: Para guardar un valor si es constante (ej: 15, "hola")
	
	public Variable(Token id, Tipo tipo, String rol) {
		this.identificador = id;
		this.tipoVar = tipo;
        this.rol = rol;
        this.valor = null; // Por defecto no tiene valor constante
	}
	
	public Tipo getTipoVar() { return tipoVar; }
	public void setTipoVar(Tipo tipoVar) { this.tipoVar = tipoVar; }
	public Token getToken() { return identificador; }
	public void setIdentificador(Token identificador) { this.identificador = identificador; }
	public int getOffset() { return offset; }
	public void setOffset(int offset) { this.offset = offset; }
    
    // --- NUEVOS MÉTODOS ---
    public String getRol() { return rol; }
    public Object getValor() { return valor; }
    public void setValor(Object valor) { this.valor = valor; }
	
	@Override
	public int compareTo(Variable var) {
		return new Integer(this.getOffset()).compareTo(new Integer(var.getOffset()));
	}
}