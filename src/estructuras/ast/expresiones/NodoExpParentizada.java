// Contenido completo y CORREGIDO
package estructuras.ast.expresiones;

import estructuras.Token;
import estructuras.ast.encadenados.NodoEncadenado;
import estructuras.ts.tipos.TipoMetodo;
import excepciones.semanticas.ExcepcionExpParentizadaPrimitiva;
import excepciones.semanticas.ExcepcionSemantica;
import modulos.GenCod;

public class NodoExpParentizada extends NodoPrimario {

	private Token identificador; // Representa el paréntesis de apertura '('
	private NodoExpresion expresion;

	public NodoExpParentizada(Token id, NodoExpresion expr, NodoEncadenado cad) {
		super(cad);
		identificador = id;
		expresion = expr;
	}

	public NodoExpresion getExpresion() {
		return expresion;
	}

	public void setExpresion(NodoExpresion expresion) {
		this.expresion = expresion;
	}

	@Override
	public Token getToken() {
		return identificador;
	}

	@Override
	public TipoMetodo chequear() throws ExcepcionSemantica {
		TipoMetodo tipo = expresion.chequear();
		if (this.getCadena() != null) {
			if (tipo.esTipoPrimitivo())
				throw new ExcepcionExpParentizadaPrimitiva(identificador);

			this.getCadena().setEsLadoIzq(this.getEsLadoIzq());
			return this.getCadena().chequear(tipo);
		} else {
			return tipo;
		}
	}

	/**
	 * Genera código intermedio para la expresión parentizada
	 * Los paréntesis no afectan la generación de código, solo la precedencia
	 * 
	 * @param gci Generador de código intermedio
	 * @return El resultado de la expresión interna
	 */
	public String generar(modulos.ir.GeneradorCodigoIntermedio gci) {
		// Simplemente delegar a la expresión interna
		return expresion.generar(gci);
	}

	@Override
	public boolean terminaEnVar() {
		if (this.getCadena() != null)
			return this.getCadena().terminaEnVar();
		else
			return false;
	}

	@Override
	public boolean terminaEnLlamada() {
		if (this.getCadena() != null)
			return this.getCadena().terminaEnLlamada();
		else
			return false;
	}
}