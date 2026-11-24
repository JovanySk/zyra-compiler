// Contenido completo y CORREGIDO
package estructuras.ast.expresiones;

import estructuras.Token;
import estructuras.ast.encadenados.NodoEncadenado;

public abstract class NodoPrimario extends NodoExpPrimario {
	
	private NodoEncadenado cadena;
	
	public NodoPrimario() {
		cadena = null;
		this.setEsLadoIzq(false);
	}
	
	public NodoPrimario(NodoEncadenado cad) {
		cadena = cad;
	}
	
	public NodoEncadenado getCadena() {
		return cadena;
	}

	public void setCadena(NodoEncadenado cadena) {
		this.cadena = cadena;
	}

    @Override
    public abstract Token getToken();

	public abstract boolean terminaEnVar();
	
	public abstract boolean terminaEnLlamada();
}